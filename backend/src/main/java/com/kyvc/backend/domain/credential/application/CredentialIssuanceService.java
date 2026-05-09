package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

// KYC 승인 후 VC 발급 요청 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CredentialIssuanceService {

    private static final String PENDING_EXTERNAL_ID_PREFIX = "pending-kyc-";

    private final CredentialRepository credentialRepository;
    private final KycApplicationRepository kycApplicationRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final CoreProperties coreProperties;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // KYC ID 기준 VC 발급 요청
    public Credential issueKycCredentialIfRequired(
            Long kycId // KYC 신청 ID
    ) {
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        return issueKycCredentialIfRequired(kycApplication);
    }

    // KYC 신청 기준 VC 발급 요청
    public Credential issueKycCredentialIfRequired(
            KycApplication kycApplication // KYC 신청
    ) {
        if (kycApplication == null || kycApplication.getKycId() == null) {
            throw new ApiException(ErrorCode.KYC_NOT_FOUND);
        }
        if (!kycApplication.isCredentialIssuable()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS, "KYC 최종 승인 상태에서만 VC 발급을 요청할 수 있습니다.");
        }

        return credentialRepository.findLatestByKycId(kycApplication.getKycId())
                .map(credential -> {
                    if (credential.isIssued() || credential.isIssuing()) {
                        return credential;
                    }
                    return requestVcIssuance(kycApplication, credential);
                })
                .orElseGet(() -> requestVcIssuance(kycApplication, createIssuingCredential(kycApplication)));
    }

    private Credential createIssuingCredential(
            KycApplication kycApplication // KYC 신청
    ) {
        IssuanceSeed seed = resolveIssuanceSeed(null);
        Credential credential = Credential.createIssuing(
                kycApplication.getCorporateId(),
                kycApplication.getKycId(),
                PENDING_EXTERNAL_ID_PREFIX + kycApplication.getKycId(),
                CoreMockSeedData.DEV_CREDENTIAL_TYPE,
                seed.issuerDid(),
                CoreMockSeedData.DEV_CREDENTIAL_STATUS_PURPOSE,
                CoreMockSeedData.DEV_KYC_LEVEL,
                CoreMockSeedData.DEV_JURISDICTION,
                seed.holderDid(),
                seed.holderAccount()
        );
        return credentialRepository.save(credential);
    }

    private Credential requestVcIssuance(
            KycApplication kycApplication, // KYC 신청
            Credential credential // 발급 대상 Credential
    ) {
        CoreRequest coreRequest = coreRequestService.createVcIssuanceRequest(credential.getCredentialId(), null);
        CoreVcIssuanceRequest request = buildVcIssuanceRequest(kycApplication, credential, coreRequest.getCoreRequestId());
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(request));

        try {
            CoreVcIssuanceResponse response = coreAdapter.requestVcIssuance(request);
            KyvcEnums.CredentialStatus credentialStatus = resolveCredentialStatus(response.status());
            credential.applyIssuanceMetadata(
                    response.credentialExternalId(),
                    response.issuerDid(),
                    credentialStatus,
                    response.vcHash(),
                    response.xrplTxHash(),
                    response.credentialStatusId(),
                    response.issuedAt(),
                    response.expiresAt()
            );

            if (KyvcEnums.CredentialStatus.VALID == credentialStatus) {
                coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(response));
                kycApplication.markVcIssued(response.issuedAt() == null ? LocalDateTime.now() : response.issuedAt());
                kycApplicationRepository.save(kycApplication);
            } else {
                credential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
                coreRequestService.markFailed(coreRequest.getCoreRequestId(), response.message());
            }

            logEventLogger.info(
                    "credential.issuance.completed",
                    "Credential issuance completed",
                    Map.of(
                            "credentialId", credential.getCredentialId(),
                            "kycId", kycApplication.getKycId()
                    )
            );
            return credentialRepository.save(credential);
        } catch (ApiException exception) {
            markCoreRequestFailure(coreRequest.getCoreRequestId(), exception);
            credential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
            logEventLogger.warn(
                    "credential.issuance.failed",
                    exception.getMessage(),
                    Map.of(
                            "credentialId", credential.getCredentialId(),
                            "kycId", kycApplication.getKycId()
                    )
            );
            return credentialRepository.save(credential);
        } catch (Exception exception) {
            coreRequestService.markFailed(coreRequest.getCoreRequestId(), "VC 발급 Core 요청 처리 중 오류가 발생했습니다.");
            credential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
            logEventLogger.error(
                    "credential.issuance.failed",
                    "VC 발급 Core 요청 처리 중 오류가 발생했습니다.",
                    Map.of(
                            "credentialId", credential.getCredentialId(),
                            "kycId", kycApplication.getKycId()
                    ),
                    exception
            );
            return credentialRepository.save(credential);
        }
    }

    private CoreVcIssuanceRequest buildVcIssuanceRequest(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // 발급 대상 Credential
            String coreRequestId // Core 요청 ID
    ) {
        IssuanceSeed seed = resolveIssuanceSeed(credential);
        LocalDateTime now = LocalDateTime.now();
        return new CoreVcIssuanceRequest(
                coreRequestId,
                credential.getCredentialId(),
                kycApplication.getKycId(),
                kycApplication.getCorporateId(),
                seed.issuerAccount(),
                seed.issuerDid(),
                seed.issuerVerificationMethodId(),
                seed.holderAccount(),
                seed.holderDid(),
                resolveText(credential.getCredentialTypeCode(), CoreMockSeedData.DEV_CREDENTIAL_TYPE),
                resolveText(credential.getKycLevelCode(), CoreMockSeedData.DEV_KYC_LEVEL),
                resolveText(credential.getJurisdictionCode(), CoreMockSeedData.DEV_JURISDICTION),
                resolveClaims(),
                now,
                CoreMockSeedData.validUntil(),
                now
        );
    }

    private IssuanceSeed resolveIssuanceSeed(
            Credential credential // 발급 대상 Credential
    ) {
        if (!coreProperties.isDevSeedEnabled()) {
            if (credential == null
                    || !StringUtils.hasText(credential.getIssuerDid())
                    || !StringUtils.hasText(credential.getHolderDid())
                    || !StringUtils.hasText(credential.getHolderXrplAddress())) {
                throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING);
            }
            String issuerAccount = accountFromDid(credential.getIssuerDid());
            if (!StringUtils.hasText(issuerAccount)) {
                throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING);
            }
            return new IssuanceSeed(
                    issuerAccount,
                    credential.getIssuerDid(),
                    credential.getIssuerDid() + "#issuer-key-1",
                    credential.getHolderXrplAddress(),
                    credential.getHolderDid()
            );
        }

        return new IssuanceSeed(
                CoreMockSeedData.DEV_ISSUER_ACCOUNT,
                resolveText(credential == null ? null : credential.getIssuerDid(), CoreMockSeedData.DEV_ISSUER_DID),
                CoreMockSeedData.DEV_ISSUER_VERIFICATION_METHOD_ID,
                resolveText(credential == null ? null : credential.getHolderXrplAddress(), CoreMockSeedData.DEV_HOLDER_ACCOUNT),
                resolveText(credential == null ? null : credential.getHolderDid(), CoreMockSeedData.DEV_HOLDER_DID)
        );
    }

    private Map<String, Object> resolveClaims() {
        if (!coreProperties.isDevSeedEnabled()) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "VC 발급 claims 데이터가 부족합니다.");
        }
        return CoreMockSeedData.legalEntityClaims();
    }

    private KyvcEnums.CredentialStatus resolveCredentialStatus(
            String status // Core 응답 상태
    ) {
        if (!StringUtils.hasText(status)) {
            return KyvcEnums.CredentialStatus.FAILED;
        }
        try {
            KyvcEnums.CredentialStatus resolvedStatus = KyvcEnums.CredentialStatus.valueOf(status.trim().toUpperCase());
            return KyvcEnums.CredentialStatus.ISSUING == resolvedStatus
                    ? KyvcEnums.CredentialStatus.FAILED
                    : resolvedStatus;
        } catch (IllegalArgumentException exception) {
            return KyvcEnums.CredentialStatus.FAILED;
        }
    }

    private void markCoreRequestFailure(
            String coreRequestId, // Core 요청 ID
            ApiException exception // Core 호출 예외
    ) {
        if (ErrorCode.CORE_API_TIMEOUT == exception.getErrorCode()) {
            coreRequestService.markTimeout(coreRequestId, exception.getMessage());
            return;
        }
        coreRequestService.markFailed(coreRequestId, exception.getMessage());
    }

    private String toJson(
            Object value // JSON 변환 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String accountFromDid(
            String did // DID 문자열
    ) {
        if (!StringUtils.hasText(did)) {
            return null;
        }
        String prefix = "did:xrpl:1:";
        String normalized = did.trim();
        return normalized.startsWith(prefix) ? normalized.substring(prefix.length()) : null;
    }

    private String resolveText(
            String value, // 원본 문자열
            String fallback // 대체 문자열
    ) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private record IssuanceSeed(
            String issuerAccount, // Issuer XRPL Account
            String issuerDid, // Issuer DID
            String issuerVerificationMethodId, // Issuer Verification Method ID
            String holderAccount, // Holder XRPL Account
            String holderDid // Holder DID
    ) {
    }
}
