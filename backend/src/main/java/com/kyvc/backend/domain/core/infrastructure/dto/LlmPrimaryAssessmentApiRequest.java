package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

// Core /ai-assessment/assessments/llm-primary 요청 DTO
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LlmPrimaryAssessmentApiRequest(
        String kycApplicationId, // KYC 신청 ID
        String legalEntityType, // Core 법인 유형
        String applicantRole, // 신청자 역할
        String applicantName, // 신청자명
        Boolean isNonprofit, // 비영리 여부
        String businessRegistrationNumber, // 사업자등록번호
        String corporateRegistrationNumber, // 법인등록번호
        DeclaredPersonApiRequest declaredRepresentative, // 신고 대표자
        List<DeclaredBeneficialOwnerApiRequest> declaredBeneficialOwners, // 신고 실소유자 목록
        List<LlmPrimaryDocumentInputApiRequest> documents // 심사 문서 목록
) {
    public LlmPrimaryAssessmentApiRequest {
        declaredBeneficialOwners = declaredBeneficialOwners == null ? List.of() : List.copyOf(declaredBeneficialOwners);
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    // Core 신고 인물 요청 DTO
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DeclaredPersonApiRequest(
            String name, // 성명
            String birthDate, // 생년월일
            String nationality, // 국적
            String englishName // 영문명
    ) {
    }

    // Core 신고 실소유자 요청 DTO
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DeclaredBeneficialOwnerApiRequest(
            String name, // 성명
            String birthDate, // 생년월일
            String nationality, // 국적
            String englishName, // 영문명
            Number ownershipPercent // 지분율
    ) {
    }

    // Core LLM 심사 문서 입력 DTO
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LlmPrimaryDocumentInputApiRequest(
            String documentId, // 문서 ID
            String originalFileName, // 원본 파일명
            String mimeType, // MIME 유형
            String declaredDocumentType, // 신고 문서 유형
            String storagePath, // 저장 경로
            String contentBase64, // 문서 원문 Base64
            String textContent, // 문서 텍스트
            Long sizeBytes, // 파일 크기 byte
            String sha256, // SHA-256 해시
            Map<String, Object> extracted // 사전 추출값
    ) {
    }
}
