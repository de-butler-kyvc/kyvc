package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Core /ai-assessment/assessments/llm-primary 응답 DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmPrimaryAssessmentApiResponse(
        String strategy, // 심사 전략
        String extractionProvider, // 추출 Provider
        KycAssessmentApiResponse assessment, // KYC 심사 결과
        List<Map<String, Object>> documents // 문서 메타데이터 목록
) {
    public LlmPrimaryAssessmentApiResponse {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    // Core KYC 심사 결과 DTO
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KycAssessmentApiResponse(
            String assessmentId, // 심사 결과 ID
            String kycApplicationId, // KYC 신청 ID
            String legalEntityType, // Core 법인 유형
            String applicantRole, // 신청자 역할
            String status, // 심사 상태
            BigDecimal overallConfidence, // 전체 신뢰도
            String summary, // 심사 요약
            List<Map<String, Object>> documentResults, // 문서별 결과 목록
            Map<String, Object> extractedFields, // 추출 필드
            List<Map<String, Object>> crossDocumentChecks, // 문서 간 검증 결과 목록
            Map<String, Object> beneficialOwnership, // 실소유자 판정 결과
            Map<String, Object> delegation, // 위임 판정 결과
            List<EngineIssueApiResponse> supplementRequests, // 보완 요청 목록
            List<EngineIssueApiResponse> manualReviewReasons, // 수동 심사 사유 목록
            List<Object> evidence, // 근거 목록
            List<Map<String, Object>> providerUsageLogs, // Provider 사용 이력
            Map<String, Object> modelMetadata, // 모델 메타데이터
            String createdAt // 생성 일시
    ) {
        public KycAssessmentApiResponse {
            documentResults = documentResults == null ? List.of() : List.copyOf(documentResults);
            extractedFields = extractedFields == null ? Map.of() : new LinkedHashMap<>(extractedFields);
            crossDocumentChecks = crossDocumentChecks == null ? List.of() : List.copyOf(crossDocumentChecks);
            beneficialOwnership = beneficialOwnership == null ? Map.of() : new LinkedHashMap<>(beneficialOwnership);
            delegation = delegation == null ? Map.of() : new LinkedHashMap<>(delegation);
            supplementRequests = supplementRequests == null ? List.of() : List.copyOf(supplementRequests);
            manualReviewReasons = manualReviewReasons == null ? List.of() : List.copyOf(manualReviewReasons);
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
            providerUsageLogs = providerUsageLogs == null ? List.of() : List.copyOf(providerUsageLogs);
            modelMetadata = modelMetadata == null ? Map.of() : new LinkedHashMap<>(modelMetadata);
        }
    }

    // Core 심사 이슈 DTO
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EngineIssueApiResponse(
            String code, // 이슈 코드
            String message, // 이슈 메시지
            List<String> evidenceRefs // 근거 참조 목록
    ) {
        public EngineIssueApiResponse {
            evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        }
    }
}
