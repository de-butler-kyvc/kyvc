package com.kyvc.backend.domain.core.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreCallbackResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationCallbackRequest;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionCallbackRequest;
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

    private static final String CALLBACK_ALREADY_PROCESSED_MESSAGE = "Callback already processed";
    private static final String CALLBACK_PROCESSED_MESSAGE = "Callback processed successfully";

    private static final String AI_REVIEW_SUCCESS_MANUAL_REASON = "AI 심사 완료 후 수동심사 전환";
    private static final String AI_REVIEW_FAILED_MANUAL_REASON = "AI 심사 실패로 수동심사 전환";

    private final CoreRequestService coreRequestService;
    private final KycApplicationRepository kycApplicationRepository;
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
            return duplicatedResponse(callbackContext.coreRequest());
        }

        CoreRequest updatedCoreRequest = processCoreRequestStatusOnly(
                callbackContext,
                request,
                request.errorMessage()
        );
        logProcessed(updatedCoreRequest, callbackContext.status());
        return processedResponse(updatedCoreRequest);
    }

    public CoreCallbackResponse processVpVerificationCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreVpVerificationCallbackRequest request // VP 검증 Callback 요청
    ) {
        validateRequestBody(request);
        CallbackContext callbackContext = prepareCallback(
                pathCoreRequestId,
                request.coreRequestId(),
                request.status(),
                KyvcEnums.CoreRequestType.VP_VERIFY
        );
        if (callbackContext.duplicated()) {
            return duplicatedResponse(callbackContext.coreRequest());
        }

        CoreRequest updatedCoreRequest = processCoreRequestStatusOnly(
                callbackContext,
                request,
                request.errorMessage()
        );
        logProcessed(updatedCoreRequest, callbackContext.status());
        return processedResponse(updatedCoreRequest);
    }

    public CoreCallbackResponse processXrplTransactionCallback(
            String pathCoreRequestId, // 경로 Core 요청 ID
            CoreXrplTransactionCallbackRequest request // XRPL Callback 요청
    ) {
        validateRequestBody(request);
        CallbackContext callbackContext = prepareCallback(
                pathCoreRequestId,
                request.coreRequestId(),
                request.status(),
                KyvcEnums.CoreRequestType.XRPL_TX
        );
        if (callbackContext.duplicated()) {
            return duplicatedResponse(callbackContext.coreRequest());
        }

        CoreRequest updatedCoreRequest = processCoreRequestStatusOnly(
                callbackContext,
                request,
                request.errorMessage()
        );
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

    // AI 심사 성공 후 수동심사 전환
    private void applyAiReviewSuccess(
            CoreRequest coreRequest, // Core 요청 엔티티
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

    // AI 심사 실패 후 수동심사 전환
    private void applyAiReviewFailed(
            CoreRequest coreRequest // Core 요청 엔티티
    ) {
        KycApplication kycApplication = findAiReviewTarget(coreRequest);
        kycApplication.failAiReviewAsManualReview(AI_REVIEW_FAILED_MANUAL_REASON);
        kycApplicationRepository.save(kycApplication);
    }

    // AI 심사 대상 KYC 조회
    private KycApplication findAiReviewTarget(
            CoreRequest coreRequest // Core 요청 엔티티
    ) {
        if (KyvcEnums.CoreTargetType.KYC_APPLICATION != coreRequest.getCoreTargetType()
                || coreRequest.getTargetId() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return kycApplicationRepository.findById(coreRequest.getTargetId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    // Core 요청 유형 검증
    private void validateCoreRequestType(
            CoreRequest coreRequest, // Core 요청 엔티티
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

        String normalizedPathCoreRequestId = pathCoreRequestId.trim(); // 경로 Core 요청 ID 정규화
        if (!StringUtils.hasText(bodyCoreRequestId)) {
            return normalizedPathCoreRequestId;
        }

        String normalizedBodyCoreRequestId = bodyCoreRequestId.trim(); // 본문 Core 요청 ID 정규화
        if (!normalizedPathCoreRequestId.equals(normalizedBodyCoreRequestId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return normalizedBodyCoreRequestId;
    }

    // Callback 상태값 검증
    private String resolveStatus(
            String rawStatus, // 원본 상태값
            String coreRequestId, // Core 요청 ID
            CoreRequest coreRequest // Core 요청 엔티티
    ) {
        if (!StringUtils.hasText(rawStatus)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String normalizedStatus = rawStatus.trim().toUpperCase(Locale.ROOT); // 상태값 정규화
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
            CoreRequest coreRequest // Core 요청 엔티티
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
            CoreRequest coreRequest // Core 요청 엔티티
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
            CoreRequest coreRequest, // Core 요청 엔티티
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
            CoreRequest coreRequest // Core 요청 엔티티
    ) {
        logEventLogger.info(
                "core.callback.duplicated",
                "Core callback already processed",
                createLogFields(coreRequest.getCoreRequestId(), coreRequest, coreRequest.getCoreRequestStatus().name())
        );
    }

    // Callback 처리 완료 로그 기록
    private void logProcessed(
            CoreRequest coreRequest, // Core 요청 엔티티
            String status // Callback 상태값
    ) {
        logEventLogger.info(
                "core.callback.processed",
                "Core callback processed",
                createLogFields(coreRequest.getCoreRequestId(), coreRequest, status)
        );
    }

    // Callback 공통 로그 필드 생성
    private Map<String, Object> createLogFields(
            String coreRequestId, // Core 요청 ID
            CoreRequest coreRequest, // Core 요청 엔티티
            String status // Callback 상태값
    ) {
        Map<String, Object> fields = new LinkedHashMap<>(); // 로그 필드 맵
        fields.put("coreRequestId", coreRequestId);
        fields.put("targetType", coreRequest.getCoreTargetType().name());
        fields.put("targetId", coreRequest.getTargetId());
        fields.put("callbackType", coreRequest.getCoreRequestType().name());
        fields.put("status", status);
        return fields;
    }

    private record CallbackContext(
            CoreRequest coreRequest,
            String coreRequestId,
            String status,
            boolean duplicated
    ) {
    }
}
