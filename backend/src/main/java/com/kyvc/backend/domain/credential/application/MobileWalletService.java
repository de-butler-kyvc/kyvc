package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.dto.WalletCredentialAcceptRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialAcceptResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialDetailResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialListResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialOfferResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialStatusRefreshResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialSummaryResponse;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.mobile.application.MobileDeviceService;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 모바일 Wallet Credential 서비스
@Service
@RequiredArgsConstructor
public class MobileWalletService {

    private static final String REFRESH_STATUS_MESSAGE = "Core 상태조회는 아직 연결하지 않았습니다. 현재 DB 상태를 반환합니다.";

    private final CredentialRepository credentialRepository;
    private final MobileDeviceService mobileDeviceService;
    private final CorporateRepository corporateRepository;
    private final LogEventLogger logEventLogger;

    // 모바일 Wallet Credential Offer 조회
    @Transactional(readOnly = true)
    public WalletCredentialOfferResponse getCredentialOffer(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long offerId // Offer ID
    ) {
        validateCredentialId(offerId);

        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        Credential credential = credentialRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_FOUND));
        validateCredentialOwnership(authContext.corporateId(), credential);
        validateOfferState(credential, LocalDateTime.now());

        Corporate corporate = corporateRepository.findById(credential.getCorporateId()).orElse(null); // 법인 정보

        logEventLogger.info(
                "wallet.offer.requested",
                "Wallet credential offer requested",
                createWalletLogFields(authContext.userId(), authContext.corporateId(), credential.getCredentialId(), offerId)
        );

        return new WalletCredentialOfferResponse(
                offerId,
                credential.getCredentialId(),
                credential.getCredentialTypeCode(),
                credential.getIssuerDid(),
                corporate == null ? null : corporate.getCorporateName(),
                corporate == null ? null : corporate.getBusinessRegistrationNo(),
                credential.getQrExpiresAt(),
                credential.isWalletSaved()
        );
    }

    // 모바일 Wallet Credential Offer 수락
    @Transactional
    public WalletCredentialAcceptResponse acceptCredentialOffer(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long offerId, // Offer ID
            WalletCredentialAcceptRequest request // Offer 수락 요청
    ) {
        validateCredentialId(offerId);
        validateAcceptRequest(request);

        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        Credential credential = credentialRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_FOUND));
        validateCredentialOwnership(authContext.corporateId(), credential);

        logEventLogger.info(
                "wallet.credential.accept.started",
                "Wallet credential accept started",
                createWalletLogFields(authContext.userId(), authContext.corporateId(), credential.getCredentialId(), offerId)
        );

        if (!StringUtils.hasText(credential.getQrToken())) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_FOUND);
        }
        if (!credential.getQrToken().equals(request.qrToken().trim())) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_INVALID_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now(); // 기준 일시
        if (credential.isOfferExpired(now)) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_EXPIRED);
        }
        if (!credential.isValid(now)) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_VALID);
        }

        if (!request.accepted()) {
            logEventLogger.info(
                    "wallet.credential.accept.rejected",
                    "Wallet credential accept rejected",
                    createWalletLogFields(authContext.userId(), authContext.corporateId(), credential.getCredentialId(), offerId)
            );
            return new WalletCredentialAcceptResponse(
                    credential.getCredentialId(),
                    false,
                    null,
                    null,
                    "사용자가 Credential 저장을 거절했습니다."
            );
        }

        mobileDeviceService.getActiveDeviceBinding(authContext.userId(), request.deviceId().trim());

        if (credential.isWalletSaved()) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_ALREADY_SAVED);
        }

        credential.acceptToWallet(
                request.deviceId().trim(),
                normalizeOptional(request.holderDid()),
                normalizeOptional(request.holderXrplAddress()),
                now
        );
        Credential savedCredential = credentialRepository.save(credential); // Wallet 저장 처리 Credential

        logEventLogger.info(
                "wallet.credential.accept.completed",
                "Wallet credential accept completed",
                createWalletLogFields(authContext.userId(), authContext.corporateId(), savedCredential.getCredentialId(), offerId)
        );

        return new WalletCredentialAcceptResponse(
                savedCredential.getCredentialId(),
                true,
                savedCredential.getWalletSavedAt(),
                createCredentialPayload(savedCredential),
                "Credential이 Wallet에 저장되었습니다."
        );
    }

    // 모바일 Wallet Credential 목록 조회
    @Transactional(readOnly = true)
    public WalletCredentialListResponse getWalletCredentials(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트

        List<WalletCredentialSummaryResponse> credentials =
                credentialRepository.findWalletCredentialsByCorporateId(authContext.corporateId()).stream()
                        .map(this::toWalletSummaryResponse)
                        .toList();

        logEventLogger.info(
                "wallet.credential.list.requested",
                "Wallet credential list requested",
                createWalletLogFields(authContext.userId(), authContext.corporateId(), null, null)
        );

        return new WalletCredentialListResponse(credentials, credentials.size());
    }

    // 모바일 Wallet Credential 상세 조회
    @Transactional(readOnly = true)
    public WalletCredentialDetailResponse getWalletCredentialDetail(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId // Credential ID
    ) {
        validateCredentialId(credentialId);

        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        Credential credential = credentialRepository.getById(credentialId); // Credential 정보
        validateCredentialOwnership(authContext.corporateId(), credential);
        if (!credential.isWalletSaved()) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_NOT_FOUND);
        }

        logEventLogger.info(
                "wallet.credential.detail.requested",
                "Wallet credential detail requested",
                createWalletLogFields(authContext.userId(), authContext.corporateId(), credentialId, null)
        );

        return toWalletDetailResponse(credential);
    }

    // 모바일 Wallet Credential 상태 갱신
    @Transactional(readOnly = true)
    public WalletCredentialStatusRefreshResponse refreshCredentialStatus(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId // Credential ID
    ) {
        validateCredentialId(credentialId);

        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        Credential credential = credentialRepository.getById(credentialId); // Credential 정보
        validateCredentialOwnership(authContext.corporateId(), credential);
        if (!credential.isWalletSaved()) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_NOT_FOUND);
        }

        // TODO(core-integration): Core credential status 계약 확정 후 CoreAdapter 상태 조회로 동기화한다.
        LocalDateTime refreshedAt = LocalDateTime.now(); // 상태 갱신 응답 시각

        logEventLogger.info(
                "wallet.credential.status.refreshed",
                "Wallet credential status refreshed",
                createWalletLogFields(authContext.userId(), authContext.corporateId(), credentialId, null)
        );

        return new WalletCredentialStatusRefreshResponse(
                credential.getCredentialId(),
                enumName(credential.getCredentialStatus()),
                credential.getXrplTxHash(),
                true,
                refreshedAt,
                REFRESH_STATUS_MESSAGE
        );
    }

    // Offer 상태 검증
    private void validateOfferState(
            Credential credential, // Credential 엔티티
            LocalDateTime now // 기준 일시
    ) {
        if (!StringUtils.hasText(credential.getQrToken())) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_FOUND);
        }
        if (credential.isOfferExpired(now)) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_EXPIRED);
        }
        if (!credential.isValid(now)) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_VALID);
        }
    }

    // Wallet Credential 요약 응답 변환
    private WalletCredentialSummaryResponse toWalletSummaryResponse(
            Credential credential // Credential 엔티티
    ) {
        return new WalletCredentialSummaryResponse(
                credential.getCredentialId(),
                credential.getCredentialTypeCode(),
                enumName(credential.getCredentialStatus()),
                credential.getIssuerDid(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                credential.getWalletSavedAt(),
                credential.getHolderDid()
        );
    }

    // Wallet Credential 상세 응답 변환
    private WalletCredentialDetailResponse toWalletDetailResponse(
            Credential credential // Credential 엔티티
    ) {
        return new WalletCredentialDetailResponse(
                credential.getCredentialId(),
                credential.getCredentialExternalId(),
                credential.getCredentialTypeCode(),
                enumName(credential.getCredentialStatus()),
                credential.getIssuerDid(),
                credential.getVcHash(),
                credential.getXrplTxHash(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                credential.getWalletSavedAt(),
                credential.getHolderDid(),
                credential.getHolderXrplAddress(),
                credential.getCredentialStatusId(),
                credential.getCredentialStatusPurposeCode(),
                credential.getKycLevelCode(),
                credential.getJurisdictionCode()
        );
    }

    // Wallet 표시용 Credential payload 생성
    private Map<String, Object> createCredentialPayload(
            Credential credential // Credential 엔티티
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("credentialId", credential.getCredentialId());
        payload.put("credentialType", credential.getCredentialTypeCode());
        payload.put("issuerDid", credential.getIssuerDid());
        payload.put("vcHash", credential.getVcHash());
        return payload;
    }

    // 인증 컨텍스트 조회
    private AuthContext resolveAuthContext(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        Corporate corporate = corporateRepository.findByUserId(userDetails.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
        return new AuthContext(userDetails.getUserId(), corporate.getCorporateId());
    }

    // Credential 소유권 검증
    private void validateCredentialOwnership(
            Long corporateId, // 법인 ID
            Credential credential // Credential 엔티티
    ) {
        if (!credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
    }

    // Credential ID 검증
    private void validateCredentialId(
            Long credentialId // Credential ID
    ) {
        if (credentialId == null || credentialId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 수락 요청 검증
    private void validateAcceptRequest(
            WalletCredentialAcceptRequest request // Offer 수락 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.qrToken())
                || !StringUtils.hasText(request.deviceId())
                || request.accepted() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 선택 문자열 정규화
    private String normalizeOptional(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    // 공통 로그 필드 생성
    private Map<String, Object> createWalletLogFields(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long credentialId, // Credential ID
            Long offerId // Offer ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("userId", userId);
        fields.put("corporateId", corporateId);
        fields.put("credentialId", credentialId);
        fields.put("offerId", offerId);
        return fields;
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    // 인증 컨텍스트
    private record AuthContext(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
    }
}
