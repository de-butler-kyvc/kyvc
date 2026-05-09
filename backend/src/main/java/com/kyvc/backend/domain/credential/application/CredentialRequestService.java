package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import com.kyvc.backend.domain.credential.dto.CredentialOperationResponse;
import com.kyvc.backend.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backend.domain.credential.dto.CredentialRequestHistoryResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRequestListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestQueryRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestRepository;
import com.kyvc.backend.domain.credential.repository.CredentialStatusHistoryRepository;
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

// Credential 재발급/폐기 요청 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CredentialRequestService {

    private static final String PENDING_REISSUE_EXTERNAL_ID_PREFIX = "pending-reissue-";

    private final CredentialRepository credentialRepository;
    private final CredentialRequestRepository credentialRequestRepository;
    private final CredentialRequestQueryRepository credentialRequestQueryRepository;
    private final CredentialStatusHistoryRepository credentialStatusHistoryRepository;
    private final CorporateRepository corporateRepository;
    private final KycApplicationRepository kycApplicationRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final CoreProperties coreProperties;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // VC 재발급 요청
    public CredentialOperationResponse requestReissue(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId, // Credential ID
            CredentialReissueRequest request // 재발급 요청
    ) {
        validateCredentialId(credentialId);
        validateRequiredText(request == null ? null : request.reason());

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        Credential sourceCredential = resolveOwnedCredential(credentialId, corporateId); // 원본 Credential
        validateOperableStatus(sourceCredential);
        validateNoInProgressRequest(sourceCredential.getCredentialId(), KyvcEnums.CredentialRequestType.REISSUE);

        String reason = mergeReason(request.reason(), request.requestMessage()); // 요청 사유
        CredentialRequest credentialRequest = credentialRequestRepository.save(CredentialRequest.create(
                sourceCredential.getCredentialId(),
                KyvcEnums.CredentialRequestType.REISSUE,
                KyvcEnums.ActorType.USER,
                userId,
                reason
        ));
        CoreRequest coreRequest = coreRequestService.createVcIssuanceRequest(sourceCredential.getCredentialId(), null);
        credentialRequest.markProcessing(coreRequest.getCoreRequestId());
        credentialRequestRepository.save(credentialRequest);

        try {
            KycApplication kycApplication = kycApplicationRepository.findById(sourceCredential.getKycId())
                    .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
            CoreVcIssuanceRequest coreIssueRequest = buildReissueCoreRequest(
                    kycApplication,
                    sourceCredential,
                    credentialRequest.getCredentialRequestId(),
                    coreRequest.getCoreRequestId()
            );
            coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreIssueRequest));
            coreRequestService.markProcessing(coreRequest.getCoreRequestId());

            CoreVcIssuanceResponse coreResponse = coreAdapter.requestVcIssuance(coreIssueRequest);
            KyvcEnums.CredentialStatus newCredentialStatus = resolveCredentialStatus(coreResponse.status());
            if (KyvcEnums.CredentialStatus.VALID != newCredentialStatus) {
                return failCredentialRequest(
                        credentialRequest,
                        coreRequest,
                        sourceCredential.getCredentialId(),
                        sourceCredential.getCredentialStatus(),
                        ErrorCode.CORE_API_CALL_FAILED.getCode()
                );
            }

            Credential newCredential = createReissuedCredential(
                    kycApplication,
                    sourceCredential,
                    credentialRequest.getCredentialRequestId(),
                    coreResponse,
                    newCredentialStatus
            );
            Credential savedCredential = credentialRepository.save(newCredential);
            credentialStatusHistoryRepository.save(CredentialStatusHistory.create(
                    savedCredential.getCredentialId(),
                    null,
                    newCredentialStatus,
                    KyvcEnums.ActorType.USER,
                    userId,
                    reason
            ));
            credentialRequest.markCompleted();
            credentialRequestRepository.save(credentialRequest);
            coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(coreResponse));

            logEventLogger.info(
                    "credential.reissue.completed",
                    "Credential reissue completed",
                    createLogFields(userId, corporateId, savedCredential.getCredentialId(), credentialRequest.getCredentialRequestId())
            );

            return new CredentialOperationResponse(
                    credentialRequest.getCredentialRequestId(),
                    savedCredential.getCredentialId(),
                    credentialRequest.getRequestStatusCode().name(),
                    savedCredential.getCredentialStatus().name(),
                    KyvcEnums.XrplTransactionStatus.CONFIRMED.name(),
                    null
            );
        } catch (ApiException exception) {
            return failCredentialRequest(
                    credentialRequest,
                    coreRequest,
                    sourceCredential.getCredentialId(),
                    sourceCredential.getCredentialStatus(),
                    exception.getErrorCode().getCode()
            );
        }
    }

    // VC 폐기 요청
    public CredentialOperationResponse requestRevoke(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId, // Credential ID
            CredentialRevokeRequest request // 폐기 요청
    ) {
        validateCredentialId(credentialId);
        validateRequiredText(request == null ? null : request.reason());

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        Credential credential = resolveOwnedCredential(credentialId, corporateId); // 폐기 대상 Credential
        validateOperableStatus(credential);
        validateNoInProgressRequest(credential.getCredentialId(), KyvcEnums.CredentialRequestType.REVOKE);

        String reason = mergeReason(request.reason(), request.requestMessage()); // 요청 사유
        CredentialRequest credentialRequest = credentialRequestRepository.save(CredentialRequest.create(
                credential.getCredentialId(),
                KyvcEnums.CredentialRequestType.REVOKE,
                KyvcEnums.ActorType.USER,
                userId,
                reason
        ));
        CoreRequest coreRequest = coreRequestService.createVcRevokeRequest(credential.getCredentialId(), null);
        credentialRequest.markProcessing(coreRequest.getCoreRequestId());
        credentialRequestRepository.save(credentialRequest);

        try {
            CoreRevokeCredentialRequest coreRevokeRequest = buildRevokeCoreRequest(credential, reason);
            coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreRevokeRequest));
            coreRequestService.markProcessing(coreRequest.getCoreRequestId());

            CoreRevokeCredentialResponse coreResponse = coreAdapter.revokeCredential(coreRevokeRequest);
            if (!coreResponse.revoked()) {
                return failCredentialRequest(
                        credentialRequest,
                        coreRequest,
                        credential.getCredentialId(),
                        credential.getCredentialStatus(),
                        ErrorCode.CORE_API_CALL_FAILED.getCode()
                );
            }

            KyvcEnums.CredentialStatus beforeStatus = credential.getCredentialStatus(); // 이전 Credential 상태
            credential.revoke(LocalDateTime.now());
            credentialRepository.save(credential);
            credentialStatusHistoryRepository.save(CredentialStatusHistory.create(
                    credential.getCredentialId(),
                    beforeStatus,
                    credential.getCredentialStatus(),
                    KyvcEnums.ActorType.USER,
                    userId,
                    reason
            ));
            credentialRequest.markCompleted();
            credentialRequestRepository.save(credentialRequest);
            coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(coreResponse));

            logEventLogger.info(
                    "credential.revoke.completed",
                    "Credential revoke completed",
                    createLogFields(userId, corporateId, credential.getCredentialId(), credentialRequest.getCredentialRequestId())
            );

            return new CredentialOperationResponse(
                    credentialRequest.getCredentialRequestId(),
                    credential.getCredentialId(),
                    credentialRequest.getRequestStatusCode().name(),
                    credential.getCredentialStatus().name(),
                    KyvcEnums.XrplTransactionStatus.CONFIRMED.name(),
                    null
            );
        } catch (ApiException exception) {
            return failCredentialRequest(
                    credentialRequest,
                    coreRequest,
                    credential.getCredentialId(),
                    credential.getCredentialStatus(),
                    exception.getErrorCode().getCode()
            );
        }
    }

    // Credential 요청 이력 목록 조회
    @Transactional(readOnly = true)
    public CredentialRequestListResponse getCredentialRequests(
            CustomUserDetails userDetails, // 인증 사용자 정보
            KyvcEnums.CredentialRequestType requestTypeCode, // 요청 유형
            KyvcEnums.CredentialRequestStatus requestStatusCode // 요청 상태
    ) {
        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        List<CredentialRequestHistoryResponse> requests = credentialRequestQueryRepository
                .findByCorporateId(corporateId, requestTypeCode, requestStatusCode)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
        return new CredentialRequestListResponse(requests, requests.size());
    }

    // Credential 요청 이력 상세 조회
    @Transactional(readOnly = true)
    public CredentialRequestHistoryResponse getCredentialRequest(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialRequestId // Credential 요청 ID
    ) {
        validateCredentialRequestId(credentialRequestId);

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        CredentialRequest credentialRequest = credentialRequestQueryRepository
                .findByCredentialRequestIdAndCorporateId(credentialRequestId, corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_REQUEST_NOT_FOUND));
        return toHistoryResponse(credentialRequest);
    }

    private CredentialOperationResponse failCredentialRequest(
            CredentialRequest credentialRequest, // Credential 요청
            CoreRequest coreRequest, // Core 요청
            Long responseCredentialId, // 응답 Credential ID
            KyvcEnums.CredentialStatus credentialStatus, // Credential 상태
            String failureReason // 실패 사유
    ) {
        credentialRequest.markFailed();
        credentialRequestRepository.save(credentialRequest);
        markCoreRequestFailure(coreRequest.getCoreRequestId(), failureReason);

        logEventLogger.warn(
                "credential.operation.failed",
                "Credential operation failed",
                createLogFields(null, null, responseCredentialId, credentialRequest.getCredentialRequestId())
        );

        return new CredentialOperationResponse(
                credentialRequest.getCredentialRequestId(),
                responseCredentialId,
                credentialRequest.getRequestStatusCode().name(),
                enumName(credentialStatus),
                null,
                failureReason
        );
    }

    private CoreVcIssuanceRequest buildReissueCoreRequest(
            KycApplication kycApplication, // KYC 신청
            Credential sourceCredential, // 원본 Credential
            Long credentialRequestId, // Credential 요청 ID
            String coreRequestId // Core 요청 ID
    ) {
        IssuanceSeed seed = resolveIssuanceSeed(sourceCredential);
        LocalDateTime now = LocalDateTime.now(); // 요청 시각
        return new CoreVcIssuanceRequest(
                coreRequestId,
                sourceCredential.getCredentialId(),
                kycApplication.getKycId(),
                kycApplication.getCorporateId(),
                seed.issuerAccount(),
                seed.issuerDid(),
                seed.issuerVerificationMethodId(),
                seed.holderAccount(),
                seed.holderDid(),
                resolveText(sourceCredential.getCredentialTypeCode(), CoreMockSeedData.DEV_CREDENTIAL_TYPE),
                resolveText(sourceCredential.getKycLevelCode(), CoreMockSeedData.DEV_KYC_LEVEL),
                resolveText(sourceCredential.getJurisdictionCode(), CoreMockSeedData.DEV_JURISDICTION),
                resolveClaims(kycApplication, sourceCredential),
                now,
                CoreMockSeedData.validUntil(),
                now
        );
    }

    private Credential createReissuedCredential(
            KycApplication kycApplication, // KYC 신청
            Credential sourceCredential, // 원본 Credential
            Long credentialRequestId, // Credential 요청 ID
            CoreVcIssuanceResponse response, // Core 발급 응답
            KyvcEnums.CredentialStatus credentialStatus // 신규 Credential 상태
    ) {
        IssuanceSeed seed = resolveIssuanceSeed(sourceCredential);
        Credential credential = Credential.createIssuing(
                kycApplication.getCorporateId(),
                kycApplication.getKycId(),
                PENDING_REISSUE_EXTERNAL_ID_PREFIX + credentialRequestId,
                resolveText(sourceCredential.getCredentialTypeCode(), CoreMockSeedData.DEV_CREDENTIAL_TYPE),
                seed.issuerDid(),
                resolveText(sourceCredential.getCredentialStatusPurposeCode(), CoreMockSeedData.DEV_CREDENTIAL_STATUS_PURPOSE),
                resolveText(sourceCredential.getKycLevelCode(), CoreMockSeedData.DEV_KYC_LEVEL),
                resolveText(sourceCredential.getJurisdictionCode(), CoreMockSeedData.DEV_JURISDICTION),
                seed.holderDid(),
                seed.holderAccount()
        );
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
        return credential;
    }

    private CoreRevokeCredentialRequest buildRevokeCoreRequest(
            Credential credential, // 폐기 대상 Credential
            String reason // 폐기 사유
    ) {
        String issuerAccount = coreProperties.isDevSeedEnabled()
                ? CoreMockSeedData.DEV_ISSUER_ACCOUNT
                : accountFromDid(credential.getIssuerDid()); // Issuer XRPL Account
        String holderAccount = coreProperties.isDevSeedEnabled()
                ? resolveText(credential.getHolderXrplAddress(), CoreMockSeedData.DEV_HOLDER_ACCOUNT)
                : credential.getHolderXrplAddress(); // Holder XRPL Account
        if (!StringUtils.hasText(issuerAccount)
                || !StringUtils.hasText(holderAccount)
                || !StringUtils.hasText(credential.getCredentialExternalId())
                || !StringUtils.hasText(credential.getCredentialTypeCode())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING);
        }
        return new CoreRevokeCredentialRequest(
                issuerAccount,
                holderAccount,
                credential.getCredentialTypeCode(),
                credential.getCredentialStatusId(),
                credential.getCredentialExternalId(),
                reason
        );
    }

    private IssuanceSeed resolveIssuanceSeed(
            Credential credential // 기준 Credential
    ) {
        if (!coreProperties.isDevSeedEnabled()) {
            if (!StringUtils.hasText(credential.getIssuerDid())
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
                resolveText(credential.getIssuerDid(), CoreMockSeedData.DEV_ISSUER_DID),
                CoreMockSeedData.DEV_ISSUER_VERIFICATION_METHOD_ID,
                resolveText(credential.getHolderXrplAddress(), CoreMockSeedData.DEV_HOLDER_ACCOUNT),
                resolveText(credential.getHolderDid(), CoreMockSeedData.DEV_HOLDER_DID)
        );
    }

    private Map<String, Object> resolveClaims(
            KycApplication kycApplication, // KYC 신청
            Credential credential // 기준 Credential
    ) {
        if (coreProperties.isDevSeedEnabled()) {
            return CoreMockSeedData.legalEntityClaims();
        }

        Corporate corporate = corporateRepository.findById(kycApplication.getCorporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        if (!StringUtils.hasText(corporate.getCorporateName())
                || !StringUtils.hasText(corporate.getBusinessRegistrationNo())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING);
        }

        Map<String, Object> kyc = new LinkedHashMap<>(); // KYC Claim
        kyc.put("jurisdiction", resolveText(credential.getJurisdictionCode(), CoreMockSeedData.DEV_JURISDICTION));
        kyc.put("assuranceLevel", resolveText(credential.getKycLevelCode(), CoreMockSeedData.DEV_KYC_LEVEL));

        Map<String, Object> legalEntity = new LinkedHashMap<>(); // 법인 Claim
        putIfHasText(legalEntity, "type", corporate.getCorporateTypeCode());
        putIfHasText(legalEntity, "name", corporate.getCorporateName());
        putIfHasText(legalEntity, "registrationNumber", corporate.getBusinessRegistrationNo());
        putIfHasText(legalEntity, "corporateRegistrationNumber", corporate.getCorporateRegistrationNo());

        Map<String, Object> representative = new LinkedHashMap<>(); // 대표자 Claim
        putIfHasText(representative, "name", corporate.getRepresentativeName());
        putIfHasText(representative, "phone", corporate.getRepresentativePhone());
        putIfHasText(representative, "email", corporate.getRepresentativeEmail());

        Map<String, Object> claims = new LinkedHashMap<>(); // Core 전달 Claim
        claims.put("kyc", kyc);
        claims.put("legalEntity", legalEntity);
        if (!representative.isEmpty()) {
            claims.put("representative", representative);
        }
        return claims;
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
            String failureReason // 실패 사유
    ) {
        if (ErrorCode.CORE_API_TIMEOUT.getCode().equals(failureReason)) {
            coreRequestService.markTimeout(coreRequestId, failureReason);
            return;
        }
        coreRequestService.markFailed(coreRequestId, failureReason);
    }

    private CredentialRequestHistoryResponse toHistoryResponse(
            CredentialRequest credentialRequest // Credential 요청
    ) {
        return new CredentialRequestHistoryResponse(
                credentialRequest.getCredentialRequestId(),
                credentialRequest.getCredentialId(),
                enumName(credentialRequest.getRequestTypeCode()),
                enumName(credentialRequest.getRequestStatusCode()),
                credentialRequest.getReason(),
                credentialRequest.getRequestedAt(),
                credentialRequest.getCompletedAt()
        );
    }

    private Credential resolveOwnedCredential(
            Long credentialId, // Credential ID
            Long corporateId // 법인 ID
    ) {
        Credential credential = credentialRepository.getById(credentialId);
        if (!credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
        return credential;
    }

    private void validateOperableStatus(
            Credential credential // Credential
    ) {
        if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_VALID);
        }
    }

    private void validateNoInProgressRequest(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestType // 요청 유형
    ) {
        if (credentialRequestRepository.existsInProgressByCredentialIdAndType(credentialId, requestType)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE);
        }
    }

    private Long resolveUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }

    private Long resolveCorporateId(
            Long userId // 사용자 ID
    ) {
        Corporate corporate = corporateRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
        return corporate.getCorporateId();
    }

    private void validateCredentialId(
            Long credentialId // Credential ID
    ) {
        if (credentialId == null || credentialId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateCredentialRequestId(
            Long credentialRequestId // Credential 요청 ID
    ) {
        if (credentialRequestId == null || credentialRequestId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRequiredText(
            String value // 필수 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String mergeReason(
            String reason, // 요청 사유
            String requestMessage // 요청 메시지
    ) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (!StringUtils.hasText(requestMessage)) {
            return normalizedReason;
        }
        return normalizedReason + " / " + requestMessage.trim();
    }

    private String resolveFailureReason(
            String message, // Core 응답 메시지
            String fallback // 기본 실패 사유
    ) {
        return StringUtils.hasText(message) ? message.trim() : fallback;
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
        String prefix = "did:xrpl:1:"; // XRPL DID prefix
        String normalized = did.trim();
        return normalized.startsWith(prefix) ? normalized.substring(prefix.length()) : null;
    }

    private String resolveText(
            String value, // 원본 문자열
            String fallback // 대체 문자열
    ) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private void putIfHasText(
            Map<String, Object> values, // 대상 Map
            String key, // Claim key
            String value // Claim value
    ) {
        if (StringUtils.hasText(value)) {
            values.put(key, value.trim());
        }
    }

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    private Map<String, Object> createLogFields(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long credentialId, // Credential ID
            Long credentialRequestId // Credential 요청 ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("userId", userId);
        fields.put("corporateId", corporateId);
        fields.put("credentialId", credentialId);
        fields.put("credentialRequestId", credentialRequestId);
        return fields;
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
