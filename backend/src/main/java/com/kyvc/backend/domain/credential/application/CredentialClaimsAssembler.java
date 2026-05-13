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

        Map<String, Object> detail = parseAiReviewDetailJson(kycApplication.getAiReviewDetailJson());
        Map<String, Object> claims = asObjectMap(detail.get("claims"));
        if (claims.isEmpty()) {
            return fallbackClaimsIfEnabled(kycApplication.getKycId());
        }
        return claims;
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

    private Map<String, Object> asObjectMap(
            Object value // Map 변환 대상
    ) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        mapValue.forEach((key, mapEntryValue) -> {
            if (key != null) {
                result.put(String.valueOf(key), mapEntryValue);
            }
        });
        return result;
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
