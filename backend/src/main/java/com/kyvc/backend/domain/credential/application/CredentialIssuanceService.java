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
import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import com.kyvc.backend.domain.credential.repository.CredentialRequestRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.credential.repository.CredentialStatusHistoryRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

// KYC 승인 후 VC 발급 요청 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CredentialIssuanceService {

    private static final String PENDING_EXTERNAL_ID_PREFIX = "pending-kyc-";
    private static final String CORE_STATUS_MODE_XRPL = "xrpl";
    private static final String CORE_CREDENTIAL_FORMAT_JWT = "jwt";
    private static final String CORE_VC_FORMAT_JWT = "vc+jwt";

    private final CredentialRepository credentialRepository;
    private final CredentialRequestRepository credentialRequestRepository;
    private final CredentialStatusHistoryRepository credentialStatusHistoryRepository;
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
        return issueKycCredentialIfRequired(kycApplication, KyvcEnums.ActorType.SYSTEM, null);
    }

    // 사용자 요청 기반 VC 발급 요청
    public Credential issueKycCredentialForUser(
            KycApplication kycApplication, // KYC 신청
            Long userId // 사용자 ID
    ) {
        return issueKycCredentialIfRequired(kycApplication, KyvcEnums.ActorType.USER, userId);
    }

    // Holder 정보 기반 KYC Credential 발급 요청
    public Credential issueKycCredentialForHolder(
            KycApplication kycApplication, // KYC 신청
            Long requestedByUserId, // 요청 사용자 ID
            String holderDid, // Holder DID
            String holderXrplAddress, // Holder XRPL 주소
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        validateHolderIssuanceInput(kycApplication, holderDid, holderXrplAddress, claims, issuer);
        Credential credential = createIssuingCredentialForHolder(
                kycApplication,
                requestedByUserId,
                holderDid,
                holderXrplAddress,
                issuer
        );
        return requestVcIssuance(
                kycApplication,
                credential,
                KyvcEnums.ActorType.USER,
                requestedByUserId,
                claims,
                issuer
        );
    }

    private Credential issueKycCredentialIfRequired(
            KycApplication kycApplication, // KYC 신청
            KyvcEnums.ActorType actorType, // 요청자 유형
            Long actorId // 요청자 ID
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
                    return requestVcIssuance(kycApplication, credential, actorType, actorId);
                })
                .orElseGet(() -> requestVcIssuance(
                        kycApplication,
                        createIssuingCredential(kycApplication, actorType, actorId),
                        actorType,
                        actorId
                ));
    }

    private Credential createIssuingCredential(
            KycApplication kycApplication, // KYC 신청
            KyvcEnums.ActorType actorType, // 요청자 유형
            Long actorId // 요청자 ID
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
        Credential saved = credentialRepository.save(credential);
        saveStatusHistory(
                saved.getCredentialId(),
                null,
                saved.getCredentialStatus(),
                actorType,
                actorId,
                KyvcEnums.CredentialRequestType.ISSUE.name(),
                "VC 발급 요청 생성"
        );
        return saved;
    }

    private Credential createIssuingCredentialForHolder(
            KycApplication kycApplication, // KYC 신청
            Long requestedByUserId, // 요청 사용자 ID
            String holderDid, // Holder DID
            String holderXrplAddress, // Holder XRPL 주소
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        Credential credential = Credential.createIssuing(
                kycApplication.getCorporateId(),
                kycApplication.getKycId(),
                PENDING_EXTERNAL_ID_PREFIX + kycApplication.getKycId() + "-" + UUID.randomUUID(),
                issuer.credentialType(),
                issuer.issuerDid(),
                CoreMockSeedData.DEV_CREDENTIAL_STATUS_PURPOSE,
                KyvcEnums.KycLevel.STANDARD.name(),
                KyvcEnums.Jurisdiction.KR.name(),
                holderDid,
                holderXrplAddress
        );
        Credential saved = credentialRepository.save(credential);
        saveStatusHistory(
                saved.getCredentialId(),
                null,
                saved.getCredentialStatus(),
                KyvcEnums.ActorType.USER,
                requestedByUserId,
                KyvcEnums.CredentialRequestType.ISSUE.name(),
                "Wallet 수령 VC 발급 요청 생성"
        );
        return saved;
    }

    private Credential requestVcIssuance(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // 발급 대상 Credential
            KyvcEnums.ActorType actorType, // 요청자 유형
            Long actorId // 요청자 ID
    ) {
        return requestVcIssuance(kycApplication, credential, actorType, actorId, null, null);
    }

    private Credential requestVcIssuance(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // 발급 대상 Credential
            KyvcEnums.ActorType actorType, // 요청자 유형
            Long actorId, // 요청자 ID
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        CredentialRequest credentialRequest = credentialRequestRepository.save(CredentialRequest.create(
                credential.getCredentialId(),
                KyvcEnums.CredentialRequestType.ISSUE,
                actorType,
                actorId,
                null,
                "VC 발급 요청"
        ));
        CoreRequest coreRequest = coreRequestService.createVcIssuanceRequest(credential.getCredentialId(), null);
        credentialRequest.markProcessing(coreRequest.getCoreRequestId());
        credentialRequestRepository.save(credentialRequest);
        CoreVcIssuanceRequest request = buildVcIssuanceRequest(
                kycApplication,
                credential,
                coreRequest.getCoreRequestId(),
                claims,
                issuer
        );
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(request));
        coreRequestService.markRunning(coreRequest.getCoreRequestId());

        try {
            logEventLogger.info(
                    "core.call.started",
                    "Core VC issuance call started",
                    Map.of(
                            "coreRequestId", coreRequest.getCoreRequestId(),
                            "credentialId", credential.getCredentialId(),
                            "kycId", kycApplication.getKycId()
                    )
            );
            CoreVcIssuanceResponse response = coreAdapter.requestVcIssuance(request);
            logEventLogger.info(
                    "core.call.completed",
                    "Core VC issuance call completed",
                    Map.of(
                            "coreRequestId", coreRequest.getCoreRequestId(),
                            "credentialId", credential.getCredentialId(),
                            "kycId", kycApplication.getKycId()
                    )
            );
            KyvcEnums.CredentialStatus credentialStatus = resolveCredentialStatus(response.status());
            KyvcEnums.CredentialStatus beforeStatus = credential.getCredentialStatus();
            credential.applyIssuanceMetadata(
                    response.credentialExternalId(),
                    response.credentialType(),
                    response.issuerDid(),
                    credentialStatus,
                    response.vcHash(),
                    response.xrplTxHash(),
                    response.credentialStatusId(),
                    response.issuedAt(),
                    response.expiresAt()
            );

            if (KyvcEnums.CredentialStatus.VALID == credentialStatus) {
                credential.applyCredentialPayload(
                        response.format(),
                        response.credentialPayloadJson(),
                        response.credentialJwt()
                );
                coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(response));
                credentialRequest.markCompleted(null);
                kycApplication.markVcIssued(response.issuedAt() == null ? LocalDateTime.now() : response.issuedAt());
                kycApplicationRepository.save(kycApplication);
            } else {
                credential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
                credentialRequest.markFailed(ErrorCode.CORE_API_CALL_FAILED.getCode());
                coreRequestService.markFailed(coreRequest.getCoreRequestId(), response.message());
            }
            saveStatusHistoryIfChanged(
                    credential.getCredentialId(),
                    beforeStatus,
                    credential.getCredentialStatus(),
                    actorType,
                    actorId,
                    KyvcEnums.CredentialRequestType.ISSUE.name(),
                    "VC 발급 Core 응답 반영"
            );
            credentialRequestRepository.save(credentialRequest);

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
            credentialRequest.markFailed(exception.getErrorCode().getCode());
            credentialRequestRepository.save(credentialRequest);
            KyvcEnums.CredentialStatus beforeStatus = credential.getCredentialStatus();
            credential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
            saveStatusHistoryIfChanged(
                    credential.getCredentialId(),
                    beforeStatus,
                    credential.getCredentialStatus(),
                    actorType,
                    actorId,
                    exception.getErrorCode().getCode(),
                    "VC 발급 Core 호출 실패"
            );
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
            credentialRequest.markFailed(ErrorCode.CORE_API_CALL_FAILED.getCode());
            credentialRequestRepository.save(credentialRequest);
            KyvcEnums.CredentialStatus beforeStatus = credential.getCredentialStatus();
            credential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
            saveStatusHistoryIfChanged(
                    credential.getCredentialId(),
                    beforeStatus,
                    credential.getCredentialStatus(),
                    actorType,
                    actorId,
                    ErrorCode.CORE_API_CALL_FAILED.getCode(),
                    "VC 발급 Core 요청 처리 오류"
            );
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
            String coreRequestId, // Core 요청 ID
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        IssuanceSeed seed = issuer == null
                ? resolveIssuanceSeed(credential)
                : resolveIssuanceSeed(credential, issuer);
        OffsetDateTime validFrom = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime validUntil = validFrom.plusYears(1);
        String credentialType = resolveText(
                issuer == null ? credential.getCredentialTypeCode() : issuer.credentialType(),
                CoreMockSeedData.DEV_CREDENTIAL_TYPE
        );
        return new CoreVcIssuanceRequest(
                coreRequestId,
                credential.getCredentialId(),
                kycApplication.getKycId(),
                kycApplication.getCorporateId(),
                seed.issuerAccount(),
                null,
                null,
                seed.issuerDid(),
                seed.issuerVerificationMethodId(),
                issuer == null ? null : issuer.signingKeyRef(),
                seed.holderAccount(),
                seed.holderDid(),
                credentialType,
                resolveText(credential.getKycLevelCode(), CoreMockSeedData.DEV_KYC_LEVEL),
                resolveText(credential.getJurisdictionCode(), CoreMockSeedData.DEV_JURISDICTION),
                claims == null ? resolveClaims() : claims,
                validFrom,
                validUntil,
                true,
                true,
                false,
                true,
                null,
                null,
                false,
                CORE_STATUS_MODE_XRPL,
                CORE_CREDENTIAL_FORMAT_JWT,
                CORE_VC_FORMAT_JWT,
                issuer == null && coreProperties.isDevSeedEnabled() ? CoreMockSeedData.DEV_HOLDER_KEY_ID : null,
                credentialType,
                validFrom
        );
    }

    private IssuanceSeed resolveIssuanceSeed(
            Credential credential, // 발급 대상 Credential
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        if (credential == null
                || issuer == null
                || !StringUtils.hasText(issuer.issuerAccount())
                || !StringUtils.hasText(issuer.issuerDid())
                || !StringUtils.hasText(issuer.issuerVerificationMethodId())
                || !StringUtils.hasText(credential.getHolderDid())
                || !StringUtils.hasText(credential.getHolderXrplAddress())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING);
        }
        return new IssuanceSeed(
                issuer.issuerAccount(),
                issuer.issuerDid(),
                issuer.issuerVerificationMethodId(),
                credential.getHolderXrplAddress(),
                credential.getHolderDid()
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

    private void validateHolderIssuanceInput(
            KycApplication kycApplication, // KYC 신청
            String holderDid, // Holder DID
            String holderXrplAddress, // Holder XRPL 주소
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        if (kycApplication == null || kycApplication.getKycId() == null) {
            throw new ApiException(ErrorCode.KYC_NOT_FOUND);
        }
        if (!kycApplication.isCredentialIssuable()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }
        if (!StringUtils.hasText(holderDid)
                || !StringUtils.hasText(holderXrplAddress)
                || claims == null
                || claims.isEmpty()
                || issuer == null
                || !StringUtils.hasText(issuer.issuerAccount())
                || !StringUtils.hasText(issuer.issuerDid())
                || !StringUtils.hasText(issuer.issuerVerificationMethodId())
                || !StringUtils.hasText(issuer.credentialType())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING);
        }
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

    private void saveStatusHistoryIfChanged(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialStatus beforeStatus, // 변경 전 상태
            KyvcEnums.CredentialStatus afterStatus, // 변경 후 상태
            KyvcEnums.ActorType actorType, // 변경자 유형
            Long actorId, // 변경자 ID
            String reasonCode, // 변경 사유 코드
            String reason // 변경 사유
    ) {
        if (afterStatus == null || beforeStatus == afterStatus) {
            return;
        }
        saveStatusHistory(credentialId, beforeStatus, afterStatus, actorType, actorId, reasonCode, reason);
    }

    private void saveStatusHistory(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialStatus beforeStatus, // 변경 전 상태
            KyvcEnums.CredentialStatus afterStatus, // 변경 후 상태
            KyvcEnums.ActorType actorType, // 변경자 유형
            Long actorId, // 변경자 ID
            String reasonCode, // 변경 사유 코드
            String reason // 변경 사유
    ) {
        credentialStatusHistoryRepository.save(CredentialStatusHistory.create(
                credentialId,
                beforeStatus,
                afterStatus,
                actorType,
                actorId,
                reasonCode,
                reason
        ));
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
