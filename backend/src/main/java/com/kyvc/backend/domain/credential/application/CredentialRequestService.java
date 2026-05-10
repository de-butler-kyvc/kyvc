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
import com.kyvc.backend.domain.credential.dto.CredentialRequestDetailResponse;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Credential 재발급/폐기 요청 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CredentialRequestService {

    private static final String PENDING_REISSUE_EXTERNAL_ID_PREFIX = "pending-reissue-";
    private static final String CORE_STATUS_MODE_XRPL = "xrpl";
    private static final String CORE_CREDENTIAL_FORMAT_JWT = "jwt";
    private static final String CORE_VC_FORMAT_JWT = "vc+jwt";

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
            CredentialReissueRequest request // VC 재발급 요청
    ) {
        validateCredentialId(credentialId);
        validateReissueRequest(request);

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        Credential sourceCredential = resolveOwnedCredential(credentialId, corporateId);
        validateReissueAllowed(sourceCredential);
        validateNoInProgressRequest(sourceCredential.getCredentialId(), KyvcEnums.CredentialRequestType.REISSUE);

        CredentialRequest credentialRequest = credentialRequestRepository.save(CredentialRequest.create(
                sourceCredential.getCredentialId(),
                KyvcEnums.CredentialRequestType.REISSUE,
                KyvcEnums.ActorType.USER,
                userId,
                KyvcEnums.CredentialRequestType.REISSUE.name(),
                normalizeReason(request.reason(), request.requestMessage())
        ));

        CoreRequest coreRequest = coreRequestService.createVcIssuanceRequest(sourceCredential.getCredentialId(), null);
        credentialRequest.markProcessing(coreRequest.getCoreRequestId());
        credentialRequestRepository.save(credentialRequest);

        KycApplication kycApplication = kycApplicationRepository.findById(sourceCredential.getKycId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        Credential reissuedCredential = credentialRepository.save(
                createReissueCredential(sourceCredential, credentialRequest.getCredentialRequestId())
        );
        CoreVcIssuanceRequest coreRequestPayload = buildVcIssuanceRequest(
                kycApplication,
                reissuedCredential,
                coreRequest.getCoreRequestId()
        );
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreRequestPayload));
        coreRequestService.markProcessing(coreRequest.getCoreRequestId());

        try {
            CoreVcIssuanceResponse coreResponse = coreAdapter.requestVcIssuance(coreRequestPayload);
            KyvcEnums.CredentialStatus beforeStatus = reissuedCredential.getCredentialStatus();
            KyvcEnums.CredentialStatus credentialStatus = resolveCredentialStatus(coreResponse.status());
            reissuedCredential.applyIssuanceMetadata(
                    coreResponse.credentialExternalId(),
                    coreResponse.issuerDid(),
                    credentialStatus,
                    coreResponse.vcHash(),
                    coreResponse.xrplTxHash(),
                    coreResponse.credentialStatusId(),
                    coreResponse.issuedAt(),
                    coreResponse.expiresAt()
            );
            if (KyvcEnums.CredentialStatus.VALID == credentialStatus) {
                reissuedCredential.applyCredentialPayload(
                        coreResponse.format(),
                        coreResponse.credentialPayloadJson(),
                        coreResponse.credentialJwt()
                );
                credentialRequest.markCompleted(null);
                coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(coreResponse));
            } else {
                reissuedCredential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
                credentialRequest.markFailed(ErrorCode.CORE_API_CALL_FAILED.getCode());
                coreRequestService.markFailed(
                        coreRequest.getCoreRequestId(),
                        resolveFailureReason(coreResponse.message(), ErrorCode.CORE_API_CALL_FAILED.getCode())
                );
            }
            credentialRequestRepository.save(credentialRequest);
            saveStatusHistoryIfChanged(
                    reissuedCredential.getCredentialId(),
                    beforeStatus,
                    reissuedCredential.getCredentialStatus(),
                    KyvcEnums.ActorType.USER,
                    userId,
                    KyvcEnums.CredentialRequestType.REISSUE.name(),
                    "VC 재발급 Core 응답 반영"
            );
            credentialRepository.save(reissuedCredential);
            logEventLogger.info(
                    "credential.reissue.completed",
                    "Credential reissue completed",
                    createBaseLogFields(userId, corporateId, reissuedCredential.getCredentialId(), credentialRequest.getCredentialRequestId())
            );
            return toOperationResponse(
                    credentialRequest,
                    reissuedCredential.getCredentialId(),
                    reissuedCredential.getCredentialStatus(),
                    failureReason(credentialRequest)
            );
        } catch (ApiException exception) {
            return failCredentialRequest(
                    credentialRequest,
                    coreRequest,
                    reissuedCredential.getCredentialId(),
                    reissuedCredential.getCredentialStatus(),
                    exception.getErrorCode().getCode()
            );
        } catch (Exception exception) {
            return failCredentialRequest(
                    credentialRequest,
                    coreRequest,
                    reissuedCredential.getCredentialId(),
                    reissuedCredential.getCredentialStatus(),
                    ErrorCode.CORE_API_CALL_FAILED.getCode()
            );
        }
    }

    // VC 폐기 요청
    public CredentialOperationResponse requestRevoke(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId, // Credential ID
            CredentialRevokeRequest request // VC 폐기 요청
    ) {
        validateCredentialId(credentialId);
        validateRevokeRequest(request);

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        Credential credential = resolveOwnedCredential(credentialId, corporateId);
        validateRevokeAllowed(credential);
        validateNoInProgressRequest(credential.getCredentialId(), KyvcEnums.CredentialRequestType.REVOKE);

        CredentialRequest credentialRequest = credentialRequestRepository.save(CredentialRequest.create(
                credential.getCredentialId(),
                KyvcEnums.CredentialRequestType.REVOKE,
                KyvcEnums.ActorType.USER,
                userId,
                KyvcEnums.CredentialRequestType.REVOKE.name(),
                normalizeReason(request.reason(), request.requestMessage())
        ));

        CoreRequest coreRequest = coreRequestService.createVcRevokeRequest(credential.getCredentialId(), null);
        credentialRequest.markProcessing(coreRequest.getCoreRequestId());
        credentialRequestRepository.save(credentialRequest);

        CoreRevokeCredentialRequest coreRequestPayload = buildCoreRevokeCredentialRequest(credential, request);
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreRequestPayload));
        coreRequestService.markProcessing(coreRequest.getCoreRequestId());

        try {
            CoreRevokeCredentialResponse coreResponse = coreAdapter.revokeCredential(coreRequestPayload);
            if (coreResponse.revoked()) {
                KyvcEnums.CredentialStatus beforeStatus = credential.getCredentialStatus();
                credential.revoke(LocalDateTime.now());
                credentialRequest.markCompleted(null);
                coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(coreResponse));
                credentialRequestRepository.save(credentialRequest);
                saveStatusHistoryIfChanged(
                        credential.getCredentialId(),
                        beforeStatus,
                        credential.getCredentialStatus(),
                        KyvcEnums.ActorType.USER,
                        userId,
                        KyvcEnums.CredentialRequestType.REVOKE.name(),
                        "VC 폐기 Core 응답 반영"
                );
                credentialRepository.save(credential);
                logEventLogger.info(
                        "credential.revoke.completed",
                        "Credential revoke completed",
                        createBaseLogFields(userId, corporateId, credential.getCredentialId(), credentialRequest.getCredentialRequestId())
                );
                return toOperationResponse(credentialRequest, credential.getCredentialId(), credential.getCredentialStatus(), null);
            }
            return failCredentialRequest(
                    credentialRequest,
                    coreRequest,
                    credential.getCredentialId(),
                    credential.getCredentialStatus(),
                    resolveFailureReason(coreResponse.message(), ErrorCode.CORE_API_CALL_FAILED.getCode())
            );
        } catch (ApiException exception) {
            return failCredentialRequest(
                    credentialRequest,
                    coreRequest,
                    credential.getCredentialId(),
                    credential.getCredentialStatus(),
                    exception.getErrorCode().getCode()
            );
        } catch (Exception exception) {
            return failCredentialRequest(
                    credentialRequest,
                    coreRequest,
                    credential.getCredentialId(),
                    credential.getCredentialStatus(),
                    ErrorCode.CORE_API_CALL_FAILED.getCode()
            );
        }
    }

    // Credential 요청 이력 목록 조회
    @Transactional(readOnly = true)
    public CredentialRequestListResponse getCredentialRequests(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String type, // 요청 유형 필터
            String status // 요청 상태 필터
    ) {
        return getCredentialRequests(userDetails, parseRequestType(type), parseRequestStatus(status));
    }

    // Credential 요청 이력 목록 조회
    @Transactional(readOnly = true)
    public CredentialRequestListResponse getCredentialRequests(
            CustomUserDetails userDetails, // 인증 사용자 정보
            KyvcEnums.CredentialRequestType requestType, // 요청 유형
            KyvcEnums.CredentialRequestStatus status // 요청 상태
    ) {
        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        List<CredentialRequestHistoryResponse> requests = credentialRequestQueryRepository
                .findByCorporateId(corporateId, requestType, status)
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

    // Credential 요청 이력 상세 조회
    @Transactional(readOnly = true)
    public CredentialRequestDetailResponse getCredentialRequestDetail(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialRequestId // Credential 요청 ID
    ) {
        validateCredentialRequestId(credentialRequestId);

        Long userId = resolveUserId(userDetails); // 사용자 ID
        Long corporateId = resolveCorporateId(userId); // 법인 ID
        CredentialRequest credentialRequest = credentialRequestQueryRepository
                .findByCredentialRequestIdAndCorporateId(credentialRequestId, corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_REQUEST_NOT_FOUND));
        Credential credential = resolveOwnedCredential(credentialRequest.getCredentialId(), corporateId);
        return new CredentialRequestDetailResponse(
                credentialRequest.getCredentialRequestId(),
                credentialRequest.getCredentialId(),
                enumName(credentialRequest.getRequestType()),
                enumName(credentialRequest.getRequestStatus()),
                enumName(credential.getCredentialStatus()),
                credentialRequest.getReason(),
                failureReason(credentialRequest),
                credentialRequest.getRequestedAt(),
                credentialRequest.getCompletedAt()
        );
    }

    private CredentialOperationResponse failCredentialRequest(
            CredentialRequest credentialRequest, // Credential 요청
            CoreRequest coreRequest, // Core 요청
            Long responseCredentialId, // 응답 Credential ID
            KyvcEnums.CredentialStatus credentialStatus, // Credential 상태
            String failureReason // 실패 사유
    ) {
        credentialRequest.markFailed(failureReason);
        credentialRequestRepository.save(credentialRequest);
        markCoreRequestFailure(coreRequest.getCoreRequestId(), failureReason);
        return toOperationResponse(credentialRequest, responseCredentialId, credentialStatus, failureReason);
    }

    private Credential createReissueCredential(
            Credential sourceCredential, // 원본 Credential
            Long credentialRequestId // Credential 요청 ID
    ) {
        return Credential.createIssuing(
                sourceCredential.getCorporateId(),
                sourceCredential.getKycId(),
                PENDING_REISSUE_EXTERNAL_ID_PREFIX + credentialRequestId,
                sourceCredential.getCredentialTypeCode(),
                sourceCredential.getIssuerDid(),
                sourceCredential.getCredentialStatusPurposeCode(),
                sourceCredential.getKycLevelCode(),
                sourceCredential.getJurisdictionCode(),
                sourceCredential.getHolderDid(),
                sourceCredential.getHolderXrplAddress()
        );
    }

    private CoreVcIssuanceRequest buildVcIssuanceRequest(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // 발급 대상 Credential
            String coreRequestId // Core 요청 ID
    ) {
        IssuanceSeed seed = resolveIssuanceSeed(credential);
        OffsetDateTime validFrom = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime validUntil = validFrom.plusYears(1);
        String credentialType = resolveText(credential.getCredentialTypeCode(), CoreMockSeedData.DEV_CREDENTIAL_TYPE);
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
                null,
                seed.holderAccount(),
                seed.holderDid(),
                credentialType,
                resolveText(credential.getKycLevelCode(), CoreMockSeedData.DEV_KYC_LEVEL),
                resolveText(credential.getJurisdictionCode(), CoreMockSeedData.DEV_JURISDICTION),
                resolveClaims(),
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
                coreProperties.isDevSeedEnabled() ? CoreMockSeedData.DEV_HOLDER_KEY_ID : null,
                credentialType,
                validFrom
        );
    }

    private CoreRevokeCredentialRequest buildCoreRevokeCredentialRequest(
            Credential credential, // Credential
            CredentialRevokeRequest request // 폐기 요청
    ) {
        return new CoreRevokeCredentialRequest(
                accountFromDid(credential.getIssuerDid()),
                credential.getHolderXrplAddress(),
                credential.getCredentialTypeCode(),
                credential.getCredentialStatusId(),
                credential.getCredentialExternalId(),
                normalizeReason(request.reason(), request.requestMessage())
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

    private void validateReissueAllowed(
            Credential credential // Credential
    ) {
        if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()
                && KyvcEnums.CredentialStatus.EXPIRED != credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_REISSUE_NOT_ALLOWED);
        }
    }

    private void validateRevokeAllowed(
            Credential credential // Credential
    ) {
        if (KyvcEnums.CredentialStatus.REVOKED == credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_REVOKE_NOT_ALLOWED);
        }
        if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()
                && KyvcEnums.CredentialStatus.SUSPENDED != credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_REVOKE_NOT_ALLOWED);
        }
    }

    private void validateNoInProgressRequest(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestType // 요청 유형
    ) {
        if (credentialRequestRepository.existsInProgressByCredentialIdAndType(credentialId, requestType)) {
            throw new ApiException(ErrorCode.CREDENTIAL_REQUEST_DUPLICATED);
        }
    }

    private void validateReissueRequest(
            CredentialReissueRequest request // 재발급 요청
    ) {
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRevokeRequest(
            CredentialRevokeRequest request // 폐기 요청
    ) {
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
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

    private KyvcEnums.CredentialRequestType parseRequestType(
            String type // 요청 유형 문자열
    ) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        try {
            return KyvcEnums.CredentialRequestType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private KyvcEnums.CredentialRequestStatus parseRequestStatus(
            String status // 요청 상태 문자열
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return KyvcEnums.CredentialRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private KyvcEnums.CredentialStatus resolveCredentialStatus(
            String status // Core 응답 상태
    ) {
        if (!StringUtils.hasText(status)) {
            return KyvcEnums.CredentialStatus.FAILED;
        }
        try {
            KyvcEnums.CredentialStatus resolvedStatus = KyvcEnums.CredentialStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            return KyvcEnums.CredentialStatus.ISSUING == resolvedStatus
                    ? KyvcEnums.CredentialStatus.FAILED
                    : resolvedStatus;
        } catch (IllegalArgumentException exception) {
            return KyvcEnums.CredentialStatus.FAILED;
        }
    }

    private IssuanceSeed resolveIssuanceSeed(
            Credential credential // 발급 대상 Credential
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

    private Map<String, Object> resolveClaims() {
        if (!coreProperties.isDevSeedEnabled()) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "VC 발급 claims 데이터 부족");
        }
        return CoreMockSeedData.legalEntityClaims();
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

    private CredentialOperationResponse toOperationResponse(
            CredentialRequest credentialRequest, // Credential 요청
            Long credentialId, // Credential ID
            KyvcEnums.CredentialStatus credentialStatus, // Credential 상태
            String failureReason // 실패 사유
    ) {
        return new CredentialOperationResponse(
                credentialRequest.getCredentialRequestId(),
                credentialId,
                enumName(credentialRequest.getRequestStatus()),
                enumName(credentialStatus),
                enumName(resolveTxStatus(credentialRequest)),
                failureReason
        );
    }

    private CredentialRequestHistoryResponse toHistoryResponse(
            CredentialRequest credentialRequest // Credential 요청
    ) {
        return new CredentialRequestHistoryResponse(
                credentialRequest.getCredentialRequestId(),
                credentialRequest.getCredentialId(),
                enumName(credentialRequest.getRequestType()),
                enumName(credentialRequest.getRequestStatus()),
                credentialRequest.getReason(),
                credentialRequest.getRequestedAt(),
                credentialRequest.getCompletedAt()
        );
    }

    private KyvcEnums.XrplTransactionStatus resolveTxStatus(
            CredentialRequest credentialRequest // Credential 요청
    ) {
        if (KyvcEnums.CredentialRequestStatus.COMPLETED == credentialRequest.getRequestStatus()) {
            return KyvcEnums.XrplTransactionStatus.CONFIRMED;
        }
        if (KyvcEnums.CredentialRequestStatus.FAILED == credentialRequest.getRequestStatus()) {
            return KyvcEnums.XrplTransactionStatus.FAILED;
        }
        return KyvcEnums.XrplTransactionStatus.PENDING;
    }

    private String failureReason(
            CredentialRequest request // Credential 요청
    ) {
        return KyvcEnums.CredentialRequestStatus.FAILED == request.getRequestStatus()
                ? request.getReasonCode()
                : null;
    }

    private String normalizeReason(
            String reason, // 요청 사유
            String requestMessage // 요청 메시지
    ) {
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : "사용자 요청";
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

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    private Map<String, Object> createBaseLogFields(
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
