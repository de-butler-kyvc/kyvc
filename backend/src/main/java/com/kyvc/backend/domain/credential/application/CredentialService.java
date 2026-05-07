package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.dto.CredentialDetailResponse;
import com.kyvc.backend.domain.credential.dto.CredentialListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialOfferResponse;
import com.kyvc.backend.domain.credential.dto.CredentialSummaryResponse;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
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
import java.util.UUID;

// Credential 조회 서비스
@Service
@RequiredArgsConstructor
public class CredentialService {

    // TODO(config): Credential Offer 만료 시간을 환경설정으로 분리한다.
    private static final long CREDENTIAL_OFFER_EXPIRE_MINUTES = 30L;

    private final CredentialRepository credentialRepository;
    private final CorporateRepository corporateRepository;
    private final KycApplicationRepository kycApplicationRepository;
    private final LogEventLogger logEventLogger;

    // 사용자 Credential 목록 조회
    @Transactional(readOnly = true)
    public CredentialListResponse getCredentials(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID

        List<CredentialSummaryResponse> credentials = credentialRepository.findByCorporateId(corporateId).stream()
                .map(this::toSummaryResponse)
                .toList();

        logEventLogger.info(
                "credential.list.requested",
                "Credential list requested",
                createBaseLogFields(userId, corporateId, null, null)
        );

        return new CredentialListResponse(credentials, credentials.size());
    }

    // 사용자 Credential 상세 조회
    @Transactional(readOnly = true)
    public CredentialDetailResponse getCredentialDetail(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId // Credential ID
    ) {
        validateCredentialId(credentialId);

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        Credential credential = credentialRepository.getById(credentialId); // Credential 정보
        if (!credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }

        logEventLogger.info(
                "credential.detail.requested",
                "Credential detail requested",
                createBaseLogFields(userId, corporateId, credentialId, null)
        );

        return toDetailResponse(credential);
    }

    // KYC 기준 Credential Offer 생성/조회
    @Transactional
    public CredentialOfferResponse getCredentialOffer(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId // KYC 신청 ID
    ) {
        validateKycId(kycId);

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!corporateId.equals(kycApplication.getCorporateId())) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }

        Credential credential = credentialRepository.findLatestByKycId(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND));
        if (!credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }

        LocalDateTime now = LocalDateTime.now(); // 기준 일시
        if (!credential.isValid(now)) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_VALID);
        }

        boolean offerCreated = false; // Offer 신규 생성 여부
        if (!StringUtils.hasText(credential.getQrToken())) {
            issueNewOffer(credential, now);
            offerCreated = true;
        } else if (credential.isOfferExpired(now)) {
            logEventLogger.info(
                    "credential.offer.expired",
                    "Credential offer expired",
                    createBaseLogFields(userId, corporateId, credential.getCredentialId(), kycId)
            );
            issueNewOffer(credential, now);
            offerCreated = true;
        }

        if (offerCreated) {
            logEventLogger.info(
                    "credential.offer.created",
                    "Credential offer created",
                    createBaseLogFields(userId, corporateId, credential.getCredentialId(), kycId)
            );
        } else {
            logEventLogger.info(
                    "credential.offer.reused",
                    "Credential offer reused",
                    createBaseLogFields(userId, corporateId, credential.getCredentialId(), kycId)
            );
        }

        return toOfferResponse(credential);
    }

    // Credential 요약 응답 변환
    private CredentialSummaryResponse toSummaryResponse(
            Credential credential // Credential 엔티티
    ) {
        return new CredentialSummaryResponse(
                credential.getCredentialId(),
                credential.getKycId(),
                credential.getCredentialTypeCode(),
                enumName(credential.getCredentialStatus()),
                credential.getIssuerDid(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                credential.isWalletSaved(),
                credential.getWalletSavedAt()
        );
    }

    // Credential 상세 응답 변환
    private CredentialDetailResponse toDetailResponse(
            Credential credential // Credential 엔티티
    ) {
        return new CredentialDetailResponse(
                credential.getCredentialId(),
                credential.getCorporateId(),
                credential.getKycId(),
                credential.getCredentialExternalId(),
                credential.getCredentialTypeCode(),
                enumName(credential.getCredentialStatus()),
                credential.getIssuerDid(),
                credential.getVcHash(),
                credential.getXrplTxHash(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                credential.getWalletSavedYn(),
                credential.getWalletSavedAt(),
                credential.getHolderDid(),
                credential.getHolderXrplAddress(),
                credential.getCredentialStatusId(),
                credential.getCredentialStatusPurposeCode(),
                credential.getKycLevelCode(),
                credential.getJurisdictionCode()
        );
    }

    // Credential Offer 응답 변환
    private CredentialOfferResponse toOfferResponse(
            Credential credential // Credential 엔티티
    ) {
        Long credentialId = credential.getCredentialId(); // Credential ID
        Map<String, Object> qrPayload = new LinkedHashMap<>(); // QR payload 데이터
        qrPayload.put("type", KyvcEnums.QrType.CREDENTIAL_OFFER.name());
        qrPayload.put("offerId", credentialId);
        qrPayload.put("qrToken", credential.getQrToken());
        qrPayload.put("expiresAt", credential.getQrExpiresAt());

        return new CredentialOfferResponse(
                credentialId,
                credentialId,
                credential.getQrToken(),
                credential.getQrExpiresAt(),
                qrPayload
        );
    }

    // Credential Offer 발급 처리
    private void issueNewOffer(
            Credential credential, // Credential 엔티티
            LocalDateTime now // 기준 일시
    ) {
        String qrToken = UUID.randomUUID().toString(); // QR 토큰
        LocalDateTime qrExpiresAt = now.plusMinutes(CREDENTIAL_OFFER_EXPIRE_MINUTES); // QR 만료 일시
        credential.issueOffer(qrToken, qrExpiresAt);
        credentialRepository.save(credential);
    }

    // 인증 사용자 ID 조회
    private Long resolveUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }

    // 사용자 소유 법인 ID 조회
    private Long resolveCorporateId(
            Long userId // 사용자 ID
    ) {
        Corporate corporate = corporateRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
        return corporate.getCorporateId();
    }

    // Credential ID 검증
    private void validateCredentialId(
            Long credentialId // Credential ID
    ) {
        if (credentialId == null || credentialId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // KYC ID 검증
    private void validateKycId(
            Long kycId // KYC ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 공통 로그 필드 생성
    private Map<String, Object> createBaseLogFields(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long credentialId, // Credential ID
            Long kycId // KYC ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("userId", userId);
        fields.put("corporateId", corporateId);
        fields.put("credentialId", credentialId);
        fields.put("kycId", kycId);
        return fields;
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
