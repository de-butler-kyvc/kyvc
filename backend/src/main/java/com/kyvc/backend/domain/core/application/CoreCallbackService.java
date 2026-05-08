package com.kyvc.backend.domain.core.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreCallbackResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionCallbackRequest;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

// Core Callback 처리 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CoreCallbackService {

    private static final String SUCCESS_STATUS = KyvcEnums.CoreRequestStatus.SUCCESS.name();
    private static final String FAILED_STATUS = KyvcEnums.CoreRequestStatus.FAILED.name();
    private static final String ERROR_STATUS = "ERROR";
    private static final String XRPL_CONFIRMED_STATUS = "CONFIRMED";
    private static final String VALID_STATUS = KyvcEnums.VpVerificationStatus.VALID.name();
    private static final String INVALID_VP_STATUS = KyvcEnums.VpVerificationStatus.INVALID.name();
    private static final String REPLAY_SUSPECTED_STATUS = KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED.name();

    private static final String CALLBACK_ALREADY_PROCESSED_MESSAGE = "Callback already processed";
    private static final String CALLBACK_PROCESSED_MESSAGE = "Callback processed successfully";

    private static final String AI_REVIEW_SUCCESS_MANUAL_REASON = "AI 심사 완료 후 수동 심사 전환";
    private static final String AI_REVIEW_FAILED_MANUAL_REASON = "AI 심사 실패로 수동 심사 전환";

    private final CoreRequestService coreRequestService;
    private final CredentialRepository credentialRepository;
    private final KycApplicationRepository kycApplicationRepository;
    private final VpVerificationRepository vpVerificationRepository;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    public CoreCallbackResponse processAiReviewCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreAiReviewCallbackRequest request // AI 심사 Callback 요청
    ) {
        validateRequestBody(request);
        CallbackContext callbackContext = prepareCallback(
                pathCoreRequestId,
                request.coreRequestId(),
                request.status(),
                KyvcEnums.CoreRequestType.AI_REVIEW
        );
        if (callbackContext.duplicated()) {
            return duplicatedResponse(callbackContext.coreRequest());
        }

        if (SUCCESS_STATUS.equals(callbackContext.status())) {
            String payloadJson = toJson(request, callbackContext.coreRequestId());
            CoreRequest updatedCoreRequest = coreRequestService.markCallbackSuccess(
                    callbackContext.coreRequestId(),
                    payloadJson
            );
            applyAiReviewSuccess(updatedCoreRequest, request);
            logProcessed(updatedCoreRequest, callbackContext.status());
            return processedResponse(updatedCoreRequest);
        }

        CoreRequest updatedCoreRequest = coreRequestService.markCallbackFailed(
                callbackContext.coreRequestId(),
                request.errorMessage()
        );
        applyAiReviewFailed(updatedCoreRequest);
        logProcessed(updatedCoreRequest, callbackContext.status());
        return processedResponse(updatedCoreRequest);
    }

    public CoreCallbackResponse processVcIssuanceCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreVcIssuanceCallbackRequest request // VC 발급 Callback 요청
    ) {
        validateRequestBody(request);
        CallbackContext callbackContext = prepareCallback(
                pathCoreRequestId,
                request.coreRequestId(),
                request.status(),
                KyvcEnums.CoreRequestType.VC_ISSUE
        );
        if (callbackContext.duplicated()) {
            if (KyvcEnums.CoreRequestStatus.SUCCESS == callbackContext.coreRequest().getCoreRequestStatus()) {
                applyVcIssuanceSuccess(callbackContext.coreRequest(), request);
            }
            return duplicatedResponse(callbackContext.coreRequest());
        }

        CoreRequest updatedCoreRequest;
        if (SUCCESS_STATUS.equals(callbackContext.status())) {
            String payloadJson = toJson(request, callbackContext.coreRequestId());
            updatedCoreRequest = coreRequestService.markCallbackSuccess(callbackContext.coreRequestId(), payloadJson);
            applyVcIssuanceSuccess(updatedCoreRequest, request);
        } else {
            updatedCoreRequest = coreRequestService.markCallbackFailed(callbackContext.coreRequestId(), request.errorMessage());
            applyVcIssuanceFailed(updatedCoreRequest);
        }
        logProcessed(updatedCoreRequest, callbackContext.status());
        return processedResponse(updatedCoreRequest);
    }

    public CoreCallbackResponse processVpVerificationCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreVpVerificationCallbackRequest request // VP 검증 Callback 요청
    ) {
        validateRequestBody(request);
        CallbackContext callbackContext = prepareVpVerificationCallback(pathCoreRequestId, request);
        if (callbackContext.duplicated()) {
            return duplicatedResponse(callbackContext.coreRequest());
        }

        CoreRequest updatedCoreRequest = processVpVerificationCoreRequest(callbackContext, request);
        applyVpVerificationCallback(updatedCoreRequest, request, callbackContext.status());
        logVpCallbackApplied(updatedCoreRequest, callbackContext.status());
        logProcessed(updatedCoreRequest, callbackContext.status());
        return processedResponse(updatedCoreRequest);
    }

    public CoreCallbackResponse processXrplTransactionCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreXrplTransactionCallbackRequest request // XRPL Callback 요청
    ) {
        validateRequestBody(request);
        CallbackContext callbackContext = prepareXrplTransactionCallback(pathCoreRequestId, request);
        if (callbackContext.duplicated()) {
            return duplicatedResponse(callbackContext.coreRequest());
        }

        CoreRequest updatedCoreRequest = processCoreRequestStatusOnly(
                callbackContext,
                request,
                request.errorMessage()
        );
        applyXrplTransactionCallback(updatedCoreRequest, request, callbackContext.status());
        logProcessed(updatedCoreRequest, callbackContext.status());
        return processedResponse(updatedCoreRequest);
    }

    // 공통 Callback 준비 처리
    private CallbackContext prepareCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            String bodyCoreRequestId, // 본문 Core 요청 ID
            String rawStatus, // 본문 상태값
            KyvcEnums.CoreRequestType expectedRequestType // 기대 Core 요청 유형
    ) {
        String resolvedCoreRequestId = resolveCoreRequestId(pathCoreRequestId, bodyCoreRequestId);
        CoreRequest coreRequest = coreRequestService.getCoreRequest(resolvedCoreRequestId);
        validateCoreRequestType(coreRequest, expectedRequestType);
        logReceived(coreRequest, rawStatus);

        if (coreRequest.isCompleted()) {
            logDuplicated(coreRequest);
            return new CallbackContext(coreRequest, resolvedCoreRequestId, coreRequest.getCoreRequestStatus().name(), true);
        }

        String resolvedStatus = resolveStatus(rawStatus, resolvedCoreRequestId, coreRequest);
        return new CallbackContext(coreRequest, resolvedCoreRequestId, resolvedStatus, false);
    }

    // VP Callback 준비 처리
    private CallbackContext prepareVpVerificationCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreVpVerificationCallbackRequest request // VP 검증 Callback 요청
    ) {
        String resolvedCoreRequestId = resolveCoreRequestId(pathCoreRequestId, request.coreRequestId());
        CoreRequest coreRequest = coreRequestService.getCoreRequest(resolvedCoreRequestId);
        validateCoreRequestType(coreRequest, KyvcEnums.CoreRequestType.VP_VERIFY);
        logReceived(coreRequest, request.status());
        logVpCallbackReceived(coreRequest, request.status());

        if (coreRequest.isCompleted()) {
            logDuplicated(coreRequest);
            return new CallbackContext(coreRequest, resolvedCoreRequestId, coreRequest.getCoreRequestStatus().name(), true);
        }

        String resolvedStatus = resolveVpStatus(request.status(), resolvedCoreRequestId, coreRequest);
        return new CallbackContext(coreRequest, resolvedCoreRequestId, resolvedStatus, false);
    }

    // XRPL Callback 준비 처리
    private CallbackContext prepareXrplTransactionCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreXrplTransactionCallbackRequest request // XRPL Callback 요청
    ) {
        String resolvedCoreRequestId = resolveCoreRequestId(pathCoreRequestId, request.coreRequestId());
        CoreRequest coreRequest = coreRequestService.getCoreRequest(resolvedCoreRequestId);
        validateCoreRequestType(coreRequest, KyvcEnums.CoreRequestType.XRPL_TX);
        logReceived(coreRequest, request.status());

        if (coreRequest.isCompleted()) {
            logDuplicated(coreRequest);
            return new CallbackContext(coreRequest, resolvedCoreRequestId, coreRequest.getCoreRequestStatus().name(), true);
        }

        String resolvedStatus = resolveXrplStatus(request.status(), resolvedCoreRequestId, coreRequest);
        return new CallbackContext(coreRequest, resolvedCoreRequestId, resolvedStatus, false);
    }

    // CoreRequest 상태만 반영
    private CoreRequest processCoreRequestStatusOnly(
            CallbackContext callbackContext, // Callback 공통 문맥
            Object callbackRequest, // Callback 요청 본문
            String errorMessage // 실패 메시지
    ) {
        if (SUCCESS_STATUS.equals(callbackContext.status())) {
            String payloadJson = toJson(callbackRequest, callbackContext.coreRequestId());
            return coreRequestService.markCallbackSuccess(callbackContext.coreRequestId(), payloadJson);
        }

        return coreRequestService.markCallbackFailed(callbackContext.coreRequestId(), errorMessage);
    }

    // VP CoreRequest 상태 반영
    private CoreRequest processVpVerificationCoreRequest(
            CallbackContext callbackContext, // Callback 공통 문맥
            CoreVpVerificationCallbackRequest request // VP 검증 Callback 요청
    ) {
        if (isVpSuccessStatus(callbackContext.status()) && !isReplayCallback(callbackContext.status(), request.replaySuspected())) {
            String payloadJson = toJson(request, callbackContext.coreRequestId());
            return coreRequestService.markCallbackSuccess(callbackContext.coreRequestId(), payloadJson);
        }
        return coreRequestService.markCallbackFailed(
                callbackContext.coreRequestId(),
                resolveVpFailureMessage(request, callbackContext.status())
        );
    }

    // AI 심사 성공 후 수동 심사 전환
    private void applyAiReviewSuccess(
            CoreRequest coreRequest, // Core 요청 Entity
            CoreAiReviewCallbackRequest request // AI 심사 Callback 요청
    ) {
        KycApplication kycApplication = findAiReviewTarget(coreRequest);
        kycApplication.completeAiReviewAsManualReview(
                request.confidenceScore(),
                request.summary(),
                request.detailJson(),
                AI_REVIEW_SUCCESS_MANUAL_REASON
        );
        kycApplicationRepository.save(kycApplication);
    }

    // AI 심사 실패 후 수동 심사 전환
    private void applyAiReviewFailed(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        KycApplication kycApplication = findAiReviewTarget(coreRequest);
        kycApplication.failAiReviewAsManualReview(AI_REVIEW_FAILED_MANUAL_REASON);
        kycApplicationRepository.save(kycApplication);
    }

    // VC 발급 성공 결과 반영
    private void applyVcIssuanceSuccess(
            CoreRequest coreRequest, // Core 요청 Entity
            CoreVcIssuanceCallbackRequest request // VC 발급 Callback 요청
    ) {
        Credential credential = findCredentialTarget(coreRequest);
        LocalDateTime issuedAt = resolveIssuedAt(credential, request.issuedAt());
        credential.applyIssuanceMetadata(
                resolveCredentialExternalId(request),
                request.issuerDid(),
                KyvcEnums.CredentialStatus.VALID,
                resolveVcHash(request),
                request.xrplTxHash(),
                request.credentialStatusId(),
                issuedAt,
                resolveExpiresAt(credential, request.expiresAt())
        );
        Credential savedCredential = credentialRepository.save(credential);

        KycApplication kycApplication = kycApplicationRepository.findById(savedCredential.getKycId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        kycApplication.markVcIssued(issuedAt);
        kycApplicationRepository.save(kycApplication);

        ensureXrplTransactionRequest(savedCredential);
    }

    // VC 발급 실패 결과 반영
    private void applyVcIssuanceFailed(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        Credential credential = findCredentialTarget(coreRequest);
        if (!credential.isIssued()) {
            credential.refreshStatus(KyvcEnums.CredentialStatus.FAILED);
            credentialRepository.save(credential);
        }
    }

    // XRPL 기록 결과 반영
    private void applyXrplTransactionCallback(
            CoreRequest coreRequest, // Core 요청 Entity
            CoreXrplTransactionCallbackRequest request, // XRPL Callback 요청
            String status // Callback 상태값
    ) {
        if (!SUCCESS_STATUS.equals(status)) {
            return;
        }

        Credential credential = findCredentialTarget(coreRequest);
        credential.applyXrplTransactionMetadata(request.txHash());
        credentialRepository.save(credential);
    }

    // AI 심사 대상 KYC 조회
    private KycApplication findAiReviewTarget(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        if (KyvcEnums.CoreTargetType.KYC_APPLICATION != coreRequest.getCoreTargetType()
                || coreRequest.getTargetId() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return kycApplicationRepository.findById(coreRequest.getTargetId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    // Credential 대상 조회
    private Credential findCredentialTarget(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        if (KyvcEnums.CoreTargetType.CREDENTIAL != coreRequest.getCoreTargetType()
                || coreRequest.getTargetId() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return credentialRepository.getById(coreRequest.getTargetId());
    }

    // XRPL 기록 요청 생성
    private void ensureXrplTransactionRequest(
            Credential credential // Credential Entity
    ) {
        if (credential == null || credential.getCredentialId() == null) {
            return;
        }
        if (coreRequestService.findLatestXrplTransactionRequest(credential.getCredentialId()).isPresent()) {
            return;
        }

        coreRequestService.createXrplTransactionRequest(
                credential.getCredentialId(),
                toJson(createXrplRequestPayload(credential), String.valueOf(credential.getCredentialId()))
        );
    }

    // XRPL 요청 Payload 생성
    private Map<String, Object> createXrplRequestPayload(
            Credential credential // Credential Entity
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("credentialId", credential.getCredentialId());
        payload.put("kycId", credential.getKycId());
        payload.put("issuerDid", credential.getIssuerDid());
        payload.put("vcHash", credential.getVcHash());
        payload.put("credentialStatus", enumName(credential.getCredentialStatus()));
        payload.put("issuedAt", credential.getIssuedAt());
        payload.put("expiresAt", credential.getExpiresAt());
        return payload;
    }

    // 발급 외부 ID 결정
    private String resolveCredentialExternalId(
            CoreVcIssuanceCallbackRequest request // VC 발급 Callback 요청
    ) {
        if (StringUtils.hasText(request.credentialExternalId())) {
            return request.credentialExternalId().trim();
        }
        if (StringUtils.hasText(request.credentialId())) {
            return request.credentialId().trim();
        }
        return null;
    }

    // VC 해시 결정
    private String resolveVcHash(
            CoreVcIssuanceCallbackRequest request // VC 발급 Callback 요청
    ) {
        if (StringUtils.hasText(request.vcHash())) {
            return request.vcHash().trim();
        }
        if (StringUtils.hasText(request.credentialHash())) {
            return request.credentialHash().trim();
        }
        return null;
    }

    // 발급 시각 결정
    private LocalDateTime resolveIssuedAt(
            Credential credential, // Credential Entity
            LocalDateTime issuedAt // Callback 발급 시각
    ) {
        if (issuedAt != null) {
            return issuedAt;
        }
        if (credential.getIssuedAt() != null) {
            return credential.getIssuedAt();
        }
        return LocalDateTime.now();
    }

    // 만료 시각 결정
    private LocalDateTime resolveExpiresAt(
            Credential credential, // Credential Entity
            LocalDateTime expiresAt // Callback 만료 시각
    ) {
        if (expiresAt != null) {
            return expiresAt;
        }
        return credential.getExpiresAt();
    }

    // VP 검증 결과 반영
    private void applyVpVerificationCallback(
            CoreRequest coreRequest, // Core 요청 Entity
            CoreVpVerificationCallbackRequest request, // VP 검증 Callback 요청
            String status // Callback 상태값
    ) {
        VpVerification vpVerification = findVpVerificationTarget(coreRequest);
        LocalDateTime verifiedAt = request.verifiedAt() == null ? LocalDateTime.now() : request.verifiedAt();
        String resultSummary = resolveVpResultSummary(request, status);

        if (isReplayCallback(status, request.replaySuspected())) {
            vpVerification.markReplaySuspected(resultSummary, verifiedAt);
        } else if (isVpSuccessStatus(status)) {
            vpVerification.markValid(resultSummary, verifiedAt);
        } else if (INVALID_VP_STATUS.equals(status)) {
            vpVerification.markInvalid(resultSummary, verifiedAt);
        } else {
            vpVerification.markFailed(resultSummary, verifiedAt);
        }
        vpVerificationRepository.save(vpVerification);
    }

    // VP 검증 대상 조회
    private VpVerification findVpVerificationTarget(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        if (KyvcEnums.CoreTargetType.VP_VERIFICATION != coreRequest.getCoreTargetType()
                || coreRequest.getTargetId() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return vpVerificationRepository.getById(coreRequest.getTargetId());
    }

    // Core 요청 유형 검증
    private void validateCoreRequestType(
            CoreRequest coreRequest, // Core 요청 Entity
            KyvcEnums.CoreRequestType expectedRequestType // 기대 Core 요청 유형
    ) {
        if (coreRequest.getCoreRequestType() != expectedRequestType) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // Callback 요청 본문 검증
    private void validateRequestBody(
            Object request // Callback 요청 본문
    ) {
        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // Core 요청 ID 정합성 검증
    private String resolveCoreRequestId(
            String pathCoreRequestId, // 경로 Core 요청 ID
            String bodyCoreRequestId // 본문 Core 요청 ID
    ) {
        if (!StringUtils.hasText(pathCoreRequestId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedPathCoreRequestId = pathCoreRequestId.trim();
        if (!StringUtils.hasText(bodyCoreRequestId)) {
            return normalizedPathCoreRequestId;
        }

        String normalizedBodyCoreRequestId = bodyCoreRequestId.trim();
        if (!normalizedPathCoreRequestId.equals(normalizedBodyCoreRequestId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return normalizedBodyCoreRequestId;
    }

    // 공통 Callback 상태값 검증
    private String resolveStatus(
            String rawStatus, // 원본 상태값
            String coreRequestId, // Core 요청 ID
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        if (!StringUtils.hasText(rawStatus)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedStatus = rawStatus.trim().toUpperCase(Locale.ROOT);
        if (ERROR_STATUS.equals(normalizedStatus)) {
            return FAILED_STATUS;
        }
        if (!SUCCESS_STATUS.equals(normalizedStatus) && !FAILED_STATUS.equals(normalizedStatus)) {
            logEventLogger.warn(
                    "core.callback.failed",
                    "Unsupported callback status",
                    createLogFields(coreRequestId, coreRequest, normalizedStatus)
            );
            throw new ApiException(ErrorCode.INVALID_STATUS);
        }
        return normalizedStatus;
    }

    // XRPL Callback 상태값 검증
    private String resolveXrplStatus(
            String rawStatus, // 원본 상태값
            String coreRequestId, // Core 요청 ID
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        if (!StringUtils.hasText(rawStatus)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedStatus = rawStatus.trim().toUpperCase(Locale.ROOT);
        if (XRPL_CONFIRMED_STATUS.equals(normalizedStatus)) {
            return SUCCESS_STATUS;
        }
        if (ERROR_STATUS.equals(normalizedStatus)) {
            return FAILED_STATUS;
        }
        if (!SUCCESS_STATUS.equals(normalizedStatus) && !FAILED_STATUS.equals(normalizedStatus)) {
            logEventLogger.warn(
                    "core.callback.failed",
                    "Unsupported XRPL callback status",
                    createLogFields(coreRequestId, coreRequest, normalizedStatus)
            );
            throw new ApiException(ErrorCode.INVALID_STATUS);
        }
        return normalizedStatus;
    }

    // VP Callback 상태값 검증
    private String resolveVpStatus(
            String rawStatus, // 원본 상태값
            String coreRequestId, // Core 요청 ID
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        if (!StringUtils.hasText(rawStatus)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedStatus = rawStatus.trim().toUpperCase(Locale.ROOT);
        if (!SUCCESS_STATUS.equals(normalizedStatus)
                && !FAILED_STATUS.equals(normalizedStatus)
                && !VALID_STATUS.equals(normalizedStatus)
                && !INVALID_VP_STATUS.equals(normalizedStatus)
                && !REPLAY_SUSPECTED_STATUS.equals(normalizedStatus)) {
            logEventLogger.warn(
                    "core.callback.failed",
                    "Unsupported VP callback status",
                    createLogFields(coreRequestId, coreRequest, normalizedStatus)
            );
            throw new ApiException(ErrorCode.INVALID_STATUS);
        }
        return normalizedStatus;
    }

    // VP 성공 상태 여부
    private boolean isVpSuccessStatus(
            String status // Callback 상태값
    ) {
        return SUCCESS_STATUS.equals(status) || VALID_STATUS.equals(status);
    }

    // Replay 상태 여부
    private boolean isReplayCallback(
            String status, // Callback 상태값
            Boolean replaySuspected // Replay 의심 여부
    ) {
        return REPLAY_SUSPECTED_STATUS.equals(status) || Boolean.TRUE.equals(replaySuspected);
    }

    // VP 실패 메시지 조회
    private String resolveVpFailureMessage(
            CoreVpVerificationCallbackRequest request, // VP 검증 Callback 요청
            String status // Callback 상태값
    ) {
        if (StringUtils.hasText(request.errorMessage())) {
            return request.errorMessage();
        }
        if (StringUtils.hasText(request.resultSummary())) {
            return request.resultSummary();
        }
        return status;
    }

    // VP 결과 요약 조회
    private String resolveVpResultSummary(
            CoreVpVerificationCallbackRequest request, // VP 검증 Callback 요청
            String status // Callback 상태값
    ) {
        if (StringUtils.hasText(request.resultSummary())) {
            return request.resultSummary();
        }
        if (StringUtils.hasText(request.errorMessage())) {
            return request.errorMessage();
        }
        return status;
    }

    // Callback 요청 JSON 변환
    private String toJson(
            Object value, // JSON 변환 대상
            String coreRequestId // Core 요청 ID
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            logEventLogger.error(
                    "core.callback.failed",
                    "Callback payload serialization failed",
                    Map.of("coreRequestId", coreRequestId),
                    exception
            );
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 중복 Callback 응답 생성
    private CoreCallbackResponse duplicatedResponse(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        return new CoreCallbackResponse(
                coreRequest.getCoreRequestId(),
                true,
                false,
                coreRequest.getCoreRequestStatus().name(),
                CALLBACK_ALREADY_PROCESSED_MESSAGE
        );
    }

    // 처리 완료 Callback 응답 생성
    private CoreCallbackResponse processedResponse(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        return new CoreCallbackResponse(
                coreRequest.getCoreRequestId(),
                true,
                true,
                coreRequest.getCoreRequestStatus().name(),
                CALLBACK_PROCESSED_MESSAGE
        );
    }

    // Callback 수신 로그 기록
    private void logReceived(
            CoreRequest coreRequest, // Core 요청 Entity
            String rawStatus // 원본 상태값
    ) {
        logEventLogger.info(
                "core.callback.received",
                "Core callback received",
                createLogFields(coreRequest.getCoreRequestId(), coreRequest, rawStatus)
        );
    }

    // Callback 중복 로그 기록
    private void logDuplicated(
            CoreRequest coreRequest // Core 요청 Entity
    ) {
        logEventLogger.info(
                "core.callback.duplicated",
                "Core callback already processed",
                createLogFields(coreRequest.getCoreRequestId(), coreRequest, coreRequest.getCoreRequestStatus().name())
        );
    }

    // Callback 처리 완료 로그 기록
    private void logProcessed(
            CoreRequest coreRequest, // Core 요청 Entity
            String status // Callback 상태값
    ) {
        logEventLogger.info(
                "core.callback.processed",
                "Core callback processed",
                createLogFields(coreRequest.getCoreRequestId(), coreRequest, status)
        );
    }

    // VP Callback 수신 로그 기록
    private void logVpCallbackReceived(
            CoreRequest coreRequest, // Core 요청 Entity
            String rawStatus // 원본 상태값
    ) {
        logEventLogger.info(
                "vp.callback.received",
                "VP callback received",
                createLogFields(coreRequest.getCoreRequestId(), coreRequest, rawStatus)
        );
    }

    // VP Callback 반영 로그 기록
    private void logVpCallbackApplied(
            CoreRequest coreRequest, // Core 요청 Entity
            String status // Callback 상태값
    ) {
        logEventLogger.info(
                "vp.callback.applied",
                "VP callback applied",
                createLogFields(coreRequest.getCoreRequestId(), coreRequest, status)
        );
    }

    // Callback 공통 로그 필드 생성
    private Map<String, Object> createLogFields(
            String coreRequestId, // Core 요청 ID
            CoreRequest coreRequest, // Core 요청 Entity
            String status // Callback 상태값
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("coreRequestId", coreRequestId);
        fields.put("targetType", coreRequest.getCoreTargetType().name());
        fields.put("targetId", coreRequest.getTargetId());
        fields.put("callbackType", coreRequest.getCoreRequestType().name());
        fields.put("status", status);
        return fields;
    }

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    private record CallbackContext(
            CoreRequest coreRequest,
            String coreRequestId,
            String status,
            boolean duplicated
    ) {
    }
}
