package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.LinkedHashMap;
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
    private static final String CORE_VC_FORMAT_DC_SD_JWT = "dc+sd-jwt";

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
            String holderKeyId, // Holder 키 ID
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        return issueKycCredentialForHolderPayload(
                kycApplication,
                requestedByUserId,
                holderDid,
                holderXrplAddress,
                holderKeyId,
                claims,
                issuer
        ).credential();
    }

    // Holder 전달용 KYC Credential 발급 결과
    public CredentialIssuanceResult issueKycCredentialForHolderPayload(
            KycApplication kycApplication, // KYC 신청
            Long requestedByUserId, // 요청 사용자 ID
            String holderDid, // Holder DID
            String holderXrplAddress, // Holder XRPL 주소
            String holderKeyId, // Holder 키 ID
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        validateHolderIssuanceInput(kycApplication, holderDid, holderXrplAddress, holderKeyId, claims, issuer);
        Credential credential = createIssuingCredentialForHolder(
                kycApplication,
                requestedByUserId,
                holderDid,
                holderXrplAddress,
                issuer
        );
        return requestVcIssuancePayload(
                kycApplication,
                credential,
                KyvcEnums.ActorType.USER,
                requestedByUserId,
                claims,
                issuer,
                holderKeyId
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
        return requestVcIssuancePayload(kycApplication, credential, actorType, actorId, null, null, null).credential();
    }

    private Credential requestVcIssuance(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // 발급 대상 Credential
            KyvcEnums.ActorType actorType, // 요청자 유형
            Long actorId, // 요청자 ID
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        return requestVcIssuancePayload(kycApplication, credential, actorType, actorId, claims, issuer, null).credential();
    }

    private CredentialIssuanceResult requestVcIssuancePayload(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // 발급 대상 Credential
            KyvcEnums.ActorType actorType, // 요청자 유형
            Long actorId, // 요청자 ID
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer, // 발급 Issuer
            String holderKeyId // Holder 키 ID
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
                issuer,
                holderKeyId
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
                    resolveIssuerDid(response),
                    credentialStatus,
                    response.vcHash(),
                    response.xrplTxHash(),
                    response.credentialStatusId(),
                    response.issuedAt(),
                    response.expiresAt()
            );

            if (KyvcEnums.CredentialStatus.VALID == credentialStatus) {
                credential.applyCredentialFormat(resolveCredentialFormat(response, request));
                coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(toStoredCoreIssuanceMetadata(response)));
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
            Credential savedCredential = credentialRepository.save(credential);
            return KyvcEnums.CredentialStatus.VALID == credentialStatus
                    ? toIssuanceResult(savedCredential, request, response)
                    : new CredentialIssuanceResult(savedCredential, null, null, null, null, null);
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
            return new CredentialIssuanceResult(credentialRepository.save(credential), null, null, null, null, null);
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
            return new CredentialIssuanceResult(credentialRepository.save(credential), null, null, null, null, null);
        }
    }

    private CoreVcIssuanceRequest buildVcIssuanceRequest(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // 발급 대상 Credential
            String coreRequestId, // Core 요청 ID
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer, // 발급 Issuer
            String holderKeyId // Holder 키 ID
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
                CORE_VC_FORMAT_DC_SD_JWT,
                resolveHolderKeyId(holderKeyId, credential, issuer),
                credentialType,
                validFrom
        );
    }

    // Holder 전달용 발급 결과 생성
    private CredentialIssuanceResult toIssuanceResult(
            Credential credential, // 저장된 Credential 메타데이터
            CoreVcIssuanceRequest request, // Core 발급 요청
            CoreVcIssuanceResponse response // Core 발급 응답
    ) {
        String format = resolveCredentialFormat(response, request); // VC format
        return new CredentialIssuanceResult(
                credential,
                format,
                resolveIssuerAccount(response),
                response.credentialJwt(),
                parseCredentialObject(response.credentialPayloadJson()),
                response.selectiveDisclosure()
        );
    }

    // Core 발급 저장용 메타데이터 생성
    private Map<String, Object> toStoredCoreIssuanceMetadata(
            CoreVcIssuanceResponse response // Core 발급 응답
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("coreRequestId", response.coreRequestId());
        metadata.put("status", response.status());
        metadata.put("format", response.format());
        metadata.put("credentialExternalId", response.credentialExternalId());
        metadata.put("credentialType", response.credentialType());
        metadata.put("issuerDid", resolveIssuerDid(response));
        metadata.put("vcHash", response.vcHash());
        metadata.put("xrplTxHash", response.xrplTxHash());
        metadata.put("credentialStatusId", response.credentialStatusId());
        metadata.put("issuedAt", response.issuedAt());
        metadata.put("expiresAt", response.expiresAt());
        metadata.put("hasCredentialPayload",
                StringUtils.hasText(response.credentialJwt())
                        || StringUtils.hasText(response.credentialPayloadJson()));
        metadata.put("hasSelectiveDisclosure",
                response.selectiveDisclosure() != null && !response.selectiveDisclosure().isEmpty());
        return metadata;
    }

    // Core 발급 format 결정
    private String resolveCredentialFormat(
            CoreVcIssuanceResponse response, // Core 발급 응답
            CoreVcIssuanceRequest request // Core 발급 요청
    ) {
        return StringUtils.hasText(response.format()) ? response.format() : request.format();
    }

    private String resolveIssuerDid(
            CoreVcIssuanceResponse response // Core 발급 응답
    ) {
        String issuerAccount = resolveIssuerAccount(response);
        if (StringUtils.hasText(issuerAccount)) {
            return "did:xrpl:1:" + issuerAccount;
        }
        return response.issuerDid();
    }

    private String resolveIssuerAccount(
            CoreVcIssuanceResponse response // Core 발급 응답
    ) {
        String issuerAccount = normalizeXrplAccount(response.issuerAccount());
        if (StringUtils.hasText(issuerAccount)) {
            return issuerAccount;
        }
        issuerAccount = issuerAccountFromCredentialStatusId(response.credentialStatusId());
        if (StringUtils.hasText(issuerAccount)) {
            return issuerAccount;
        }
        return normalizeXrplAccount(accountFromDid(response.issuerDid()));
    }

    private String issuerAccountFromCredentialStatusId(
            String credentialStatusId // Credential Status ID
    ) {
        if (!StringUtils.hasText(credentialStatusId)) {
            return null;
        }
        String[] parts = credentialStatusId.trim().split(":");
        if (parts.length < 5 || !CORE_STATUS_MODE_XRPL.equalsIgnoreCase(parts[0]) || !"credential".equalsIgnoreCase(parts[1])) {
            return null;
        }
        return normalizeXrplAccount(parts[2]);
    }

    private String normalizeXrplAccount(
            String account // XRPL Account
    ) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        String normalized = account.trim();
        return normalized.startsWith("r") && normalized.length() >= 25 && normalized.length() <= 35 ? normalized : null;
    }

    // legacy credential object 변환
    private Map<String, Object> parseCredentialObject(
            String credentialPayloadJson // Core credential JSON 원문
    ) {
        if (!StringUtils.hasText(credentialPayloadJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(credentialPayloadJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CORE_API_RESPONSE_INVALID, exception);
        }
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
            String holderKeyId, // Holder 키 ID
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
                || !StringUtils.hasText(holderKeyId)
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

    private String resolveHolderKeyId(
            String holderKeyId, // Holder 키 ID
            Credential credential, // 발급 대상 Credential
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        if (StringUtils.hasText(holderKeyId)) {
            return holderKeyId.trim();
        }
        if (credential != null && StringUtils.hasText(credential.getHolderDid())) {
            return credential.getHolderDid().trim() + "#holder-key-1";
        }
        if (issuer == null && coreProperties.isDevSeedEnabled()) {
            return CoreMockSeedData.DEV_HOLDER_KEY_ID;
        }
        throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "VC 발급 Holder 키 식별자가 없습니다.");
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
