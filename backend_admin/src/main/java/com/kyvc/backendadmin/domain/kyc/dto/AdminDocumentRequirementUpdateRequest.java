package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 필수서류 정책 수정 요청 DTO입니다.
 *
 * <p>전달된 값만 기존 정책에 반영하며, 전달하지 않은 값은 기존 값을 유지합니다.</p>
 */
@Schema(description = "필수서류 정책 수정 요청")
public record AdminDocumentRequirementUpdateRequest(
        /** 법인 유형 코드 */
        @Schema(description = "법인 유형 공통코드", example = "CORPORATION")
        String corporateTypeCode,

        /** 제출서류 유형 코드 */
        @Schema(description = "제출서류 유형 공통코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode,

        /** 필수 여부 */
        @Schema(description = "필수 여부", example = "Y", allowableValues = {"Y", "N"})
        String requiredYn,

        /** 사용 여부 */
        @Schema(description = "사용 여부", example = "Y", allowableValues = {"Y", "N"})
        String enabledYn,

        /** 정렬 순서 */
        @Schema(description = "정렬 순서", example = "1")
        Integer sortOrder,

        /** 설명 */
        @Schema(description = "제출 안내 설명", example = "최근 3개월 이내 발급본을 제출하세요.")
        String description
) {
}
