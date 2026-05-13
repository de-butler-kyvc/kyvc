package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Map;

// Credential claims 조립기
@Component
@RequiredArgsConstructor
public class CredentialClaimsAssembler {

    private final CoreProperties coreProperties;
    private final LogEventLogger logEventLogger;
    private final ObjectMapper objectMapper;

    // KYC 신청 기준 claims 조립
    public Map<String, Object> assemble(
            KycApplication kycApplication // KYC 신청
    ) {
        if (kycApplication == null || kycApplication.getKycId() == null) {
            throw new ApiException(ErrorCode.KYC_NOT_FOUND);
        }

        Map<String, Object> aiReview = aiReviewClaims(kycApplication);
        if (aiReview.isEmpty()) {
            return fallbackClaimsIfEnabled(kycApplication.getKycId());
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("aiReview", aiReview);
        return claims;
    }

    private Map<String, Object> aiReviewClaims(
            KycApplication kycApplication // KYC 신청
    ) {
        Map<String, Object> detail = parseAiReviewDetailJson(kycApplication.getAiReviewDetailJson());
        Map<String, Object> aiReview = new LinkedHashMap<>();
        putIfPresent(aiReview, "coreRequestId", detail.get("coreRequestId"));
        putIfPresent(aiReview, "aiReviewStatus", firstPresent(kycApplication.getAiReviewStatus(), detail.get("status")));
        putIfPresent(aiReview, "aiReviewResult", kycApplication.getAiReviewResult());
        putIfPresent(aiReview, "assessmentStatus", detail.get("assessmentStatus"));
        putIfPresent(aiReview, "assessmentId", detail.get("assessmentId"));
        putIfPresent(aiReview, "confidenceScore", firstPresent(kycApplication.getAiConfidenceScore(), detail.get("confidenceScore")));
        putIfPresent(aiReview, "summary", firstPresent(kycApplication.getAiReviewSummary(), detail.get("message")));
        putIfPresent(aiReview, "requestedAt", detail.get("requestedAt"));
        return aiReview;
    }

    private Map<String, Object> parseAiReviewDetailJson(
            String detailJson // AI 심사 상세 JSON
    ) {
        if (!StringUtils.hasText(detailJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(detailJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CREDENTIAL_CLAIMS_REQUIRED_DATA_MISSING, exception);
        }
    }

    private void putIfPresent(
            Map<String, Object> target, // claims Map
            String key, // claims key
            Object value // claims 값
    ) {
        Object normalized = normalizeValue(value);
        if (normalized != null) {
            target.put(key, normalized);
        }
    }

    private Object firstPresent(
            Object first, // 1순위 값
            Object second // 2순위 값
    ) {
        if (normalizeValue(first) != null) {
            return first;
        }
        return second;
    }

    private Object normalizeValue(
            Object value // claims 값
    ) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return StringUtils.hasText(stringValue) ? stringValue : null;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof TemporalAccessor) {
            return value.toString();
        }
        return value;
    }

    private Map<String, Object> fallbackClaimsIfEnabled(
            Long kycId // KYC 신청 ID
    ) {
        if (coreProperties.isDevSeedEnabled()) {
            logEventLogger.warn(
                    "credential.claims.ai_review_missing",
                    "Credential claims AI 심사 결과 누락",
                    Map.of("kycId", kycId)
            );
        }
        throw new ApiException(ErrorCode.CREDENTIAL_CLAIMS_REQUIRED_DATA_MISSING);
    }
}
