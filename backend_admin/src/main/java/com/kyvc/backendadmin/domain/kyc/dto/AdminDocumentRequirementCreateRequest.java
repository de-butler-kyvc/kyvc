package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 법인 유형별 필수서류 정책 등록 요청 DTO입니다.
 *
 * <p>새 필수서류 정책 생성을 위해 법인 유형, 문서 유형, 필수 여부,
 * 사용 여부, 정렬 순서, 안내 문구를 전달합니다.</p>
 */
@Schema(description = "필수서류 정책 등록 요청")
public record AdminDocumentRequirementCreateRequest(
        @Schema(description = "법인 유형 공통코드", example = "CORPORATION")
        @NotBlank
        String corporateType,
        @Schema(description = "문서 유형 공통코드", example = "BUSINESS_REGISTRATION")
        @NotBlank
        String documentType,
        @Schema(description = "필수 여부", example = "Y", allowableValues = {"Y", "N"})
        @NotBlank
        String requiredYn,
        @Schema(description = "사용 여부", example = "Y", allowableValues = {"Y", "N"})
        @NotBlank
        String enabledYn,
        @Schema(description = "정렬 순서", example = "1")
        Integer sortOrder,
        @Schema(description = "제출 안내 문구", example = "최근 3개월 이내 발급본을 제출하세요.")
        String guideMessage
) {
}
