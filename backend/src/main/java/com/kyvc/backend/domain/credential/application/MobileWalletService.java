package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.dto.CoreCredentialStatusResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
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
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

// 모바일 Wallet Credential 서비스
@Service
@RequiredArgsConstructor
public class MobileWalletService {

    private static final String REFRESH_STATUS_DB_FALLBACK_MESSAGE = "Core 상태조회에 필요한 holder/issuer 정보가 없어 DB 상태를 반환했습니다.";
    private static final String REFRESH_STATUS_SYNCED_MESSAGE = "Core credential status를 동기화했습니다.";
    private static final String REFRESH_STATUS_NOT_FOUND_MESSAGE = "Core 상태 엔트리가 없어 DB 상태를 유지했습니다.";
    private static final String REFRESH_STATUS_CORE_FAILED_MESSAGE = "Core 상태조회 실패로 DB 상태를 반환했습니다.";

    private final CredentialRepository credentialRepository;
    private final MobileDeviceService mobileDeviceService;
    private final CorporateRepository corporateRepository;
    private final CoreAdapter coreAdapter;
    private final CoreProperties coreProperties;
    private final ObjectMapper objectMapper;
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
                credential.getKycId(),
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

        if (!hasOfferToken(credential)) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_FOUND);
        }
        if (!isValidOfferToken(credential, request.qrToken().trim())) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_INVALID_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now(); // 기준 시각
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
                "Credential가 Wallet에 저장되었습니다."
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
    @Transactional
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

        String issuerAccount = resolveIssuerAccount(credential); // Issuer XRPL Account
        String holderAccount = resolveHolderAccount(credential); // Holder XRPL Account
        String credentialType = resolveCredentialType(credential); // Credential 유형

        if (!StringUtils.hasText(issuerAccount)
                || !StringUtils.hasText(holderAccount)
                || !StringUtils.hasText(credentialType)) {
            return new WalletCredentialStatusRefreshResponse(
                    credential.getCredentialId(),
                    enumName(credential.getCredentialStatus()),
                    credential.getXrplTxHash(),
                    false,
                    LocalDateTime.now(),
                    REFRESH_STATUS_DB_FALLBACK_MESSAGE
            );
        }

        try {
            CoreCredentialStatusResponse coreStatus = coreAdapter.getCredentialStatus(
                    issuerAccount,
                    holderAccount,
                    credentialType
            );

            KyvcEnums.CredentialStatus mappedStatus = parseCredentialStatus(coreStatus.credentialStatusCode()); // Core 매핑 상태
            if (coreStatus.found() && mappedStatus != null && mappedStatus != credential.getCredentialStatus()) {
                credential.refreshStatus(mappedStatus);
                credentialRepository.save(credential);
            }

            logEventLogger.info(
                    "wallet.credential.status.refreshed",
                    "Wallet credential status refreshed",
                    createWalletLogFields(authContext.userId(), authContext.corporateId(), credentialId, null)
            );

            String message = coreStatus.found() ? REFRESH_STATUS_SYNCED_MESSAGE : REFRESH_STATUS_NOT_FOUND_MESSAGE;
            return new WalletCredentialStatusRefreshResponse(
                    credential.getCredentialId(),
                    enumName(credential.getCredentialStatus()),
                    credential.getXrplTxHash(),
                    true,
                    LocalDateTime.now(),
                    message
            );
        } catch (ApiException exception) {
            if (isCoreRefreshFallbackError(exception.getErrorCode())) {
                logEventLogger.warn(
                        "wallet.credential.status.refresh.failed",
                        exception.getMessage(),
                        createWalletLogFields(authContext.userId(), authContext.corporateId(), credentialId, null)
                );
                return new WalletCredentialStatusRefreshResponse(
                        credential.getCredentialId(),
                        enumName(credential.getCredentialStatus()),
                        credential.getXrplTxHash(),
                        false,
                        LocalDateTime.now(),
                        REFRESH_STATUS_CORE_FAILED_MESSAGE
                );
            }
            throw exception;
        }
    }

    // Offer 상태 검증
    private void validateOfferState(
            Credential credential, // Credential 엔티티
            LocalDateTime now // 기준 시각
    ) {
        if (!hasOfferToken(credential)) {
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

    // Offer 토큰 존재 여부
    private boolean hasOfferToken(
            Credential credential // Credential 엔티티
    ) {
        return StringUtils.hasText(credential.getOfferTokenHash()) || StringUtils.hasText(credential.getQrToken());
    }

    // Offer 토큰 검증
    private boolean isValidOfferToken(
            Credential credential, // Credential 엔티티
            String rawToken // 원본 QR 토큰
    ) {
        if (StringUtils.hasText(credential.getOfferTokenHash())) {
            return credential.getOfferTokenHash().equals(TokenHashUtil.sha256(rawToken));
        }
        return credential.getQrToken().equals(rawToken);
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
                credential.getJurisdictionCode(),
                createCredentialPayload(credential)
        );
    }

    // Wallet 표시용 Credential payload 생성
    private Map<String, Object> createCredentialPayload(
            Credential credential // Credential 엔티티
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("format", credential.getVcFormat());
        payload.put("credentialJwt", credential.getVcJwt());
        payload.put("credential", parseCredentialPayloadJson(credential.getVcPayloadJson()));
        payload.put("metadata", createCredentialMetadata(credential));
        return payload;
    }

    private Map<String, Object> createCredentialMetadata(
            Credential credential // Credential 엔티티
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("credentialId", credential.getCredentialId());
        metadata.put("credentialType", credential.getCredentialTypeCode());
        metadata.put("issuerDid", credential.getIssuerDid());
        metadata.put("holderDid", credential.getHolderDid());
        metadata.put("holderXrplAddress", credential.getHolderXrplAddress());
        metadata.put("vcHash", credential.getVcHash());
        metadata.put("xrplTxHash", credential.getXrplTxHash());
        metadata.put("credentialStatusId", credential.getCredentialStatusId());
        metadata.put("issuedAt", credential.getIssuedAt());
        metadata.put("expiresAt", credential.getExpiresAt());
        return metadata;
    }

    private Map<String, Object> parseCredentialPayloadJson(
            String vcPayloadJson // VC JSON 원문
    ) {
        if (!StringUtils.hasText(vcPayloadJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(vcPayloadJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CORE_API_RESPONSE_INVALID, exception);
        }
    }

    // Issuer XRPL Account 결정
    private String resolveIssuerAccount(
            Credential credential // Credential 엔티티
    ) {
        String issuerDid = normalizeOptional(credential.getIssuerDid());
        String accountFromDid = resolveAccountFromDid(issuerDid); // DID 기반 XRPL Account
        if (StringUtils.hasText(accountFromDid)) {
            return accountFromDid;
        }
        return useDevSeedIfEnabled("issuerAccount", CoreMockSeedData.DEV_ISSUER_ACCOUNT);
    }

    // Holder XRPL Account 결정
    private String resolveHolderAccount(
            Credential credential // Credential 엔티티
    ) {
        String holderAccount = normalizeOptional(credential.getHolderXrplAddress());
        if (StringUtils.hasText(holderAccount)) {
            return holderAccount;
        }
        return useDevSeedIfEnabled("holderAccount", CoreMockSeedData.DEV_HOLDER_ACCOUNT);
    }

    // Credential 유형 결정
    private String resolveCredentialType(
            Credential credential // Credential 엔티티
    ) {
        String credentialType = normalizeOptional(credential.getCredentialTypeCode());
        if (StringUtils.hasText(credentialType)) {
            return credentialType;
        }
        return useDevSeedIfEnabled("credentialType", CoreMockSeedData.DEV_CREDENTIAL_TYPE);
    }

    // 개발 seed 사용
    private String useDevSeedIfEnabled(
            String fieldName, // 누락 필드명
            String seedValue // seed 값
    ) {
        if (!coreProperties.isDevSeedEnabled()) {
            return null;
        }
        logEventLogger.warn(
                "core.dev-seed.used",
                "Core 상태조회에 개발 seed 값을 사용합니다.",
                Map.of("fieldName", fieldName, "devSeedUsed", true)
        );
        return seedValue;
    }

    // DID에서 XRPL Account 추출
    private String resolveAccountFromDid(
            String did // DID 문자열
    ) {
        if (!StringUtils.hasText(did)) {
            return null;
        }
        String normalized = did.trim();
        String prefix = "did:xrpl:1:";
        if (!normalized.startsWith(prefix)) {
            return null;
        }
        String account = normalized.substring(prefix.length()).trim();
        return StringUtils.hasText(account) ? account : null;
    }

    // Core refresh fallback 대상 에러 여부
    private boolean isCoreRefreshFallbackError(
            ErrorCode errorCode // 에러 코드
    ) {
        return ErrorCode.CORE_API_CALL_FAILED == errorCode
                || ErrorCode.CORE_API_TIMEOUT == errorCode
                || ErrorCode.CORE_API_RESPONSE_INVALID == errorCode
                || ErrorCode.CORE_REQUIRED_DATA_MISSING == errorCode
                || ErrorCode.CORE_DEV_SEED_DISABLED == errorCode;
    }

    // Credential 상태 코드 파싱
    private KyvcEnums.CredentialStatus parseCredentialStatus(
            String statusCode // 상태 코드 문자열
    ) {
        if (!StringUtils.hasText(statusCode)) {
            return null;
        }
        try {
            return KyvcEnums.CredentialStatus.valueOf(statusCode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
