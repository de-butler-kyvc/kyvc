package com.kyvc.backend.domain.core.exception;

import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class CoreAiReviewException extends ApiException {

    public enum FailureType {
        TIMEOUT,
        INVALID_RESPONSE,
        CORE_ERROR
    }

    private final String endpoint;
    private final String coreRequestId;
    private final Long kycId;
    private final Long corporateId;
    private final FailureType failureType;
    private final Integer statusCode;
    private final String contentType;
    private final String responseBodySummary;
    private final long durationMs;
    private final int configuredAiReviewTimeoutSeconds;

    public CoreAiReviewException(
            ErrorCode errorCode, // 실패 오류 코드
            String message, // 실패 요약 메시지
            Throwable cause, // 원인 예외
            String endpoint, // Core endpoint
            String coreRequestId, // Core 요청 ID
            Long kycId, // KYC 신청 ID
            Long corporateId, // 법인 ID
            FailureType failureType, // Core AI 실패 유형
            Integer statusCode, // Core 응답 상태 코드
            String contentType, // Core 응답 Content-Type
            String responseBodySummary, // Core 응답 요약
            long durationMs, // 요청 소요 시간
            int configuredAiReviewTimeoutSeconds // 설정된 AI 심사 타임아웃
    ) {
        super(errorCode, message, cause);
        this.endpoint = endpoint;
        this.coreRequestId = coreRequestId;
        this.kycId = kycId;
        this.corporateId = corporateId;
        this.failureType = failureType;
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.responseBodySummary = responseBodySummary;
        this.durationMs = durationMs;
        this.configuredAiReviewTimeoutSeconds = configuredAiReviewTimeoutSeconds;
    }

    public String failureReasonCode() {
        return switch (failureType) {
            case TIMEOUT -> "CORE_AI_REVIEW_TIMEOUT";
            case INVALID_RESPONSE -> "CORE_AI_REVIEW_INVALID_RESPONSE";
            case CORE_ERROR -> "CORE_AI_REVIEW_FAILED";
        };
    }
}
