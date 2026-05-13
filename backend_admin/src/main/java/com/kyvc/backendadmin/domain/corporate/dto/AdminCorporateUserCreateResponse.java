package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 법인 사용자 계정 등록 응답 DTO입니다.
 */
@Schema(description = "법인 사용자 계정 등록 응답")
public record AdminCorporateUserCreateResponse(
        /** 사용자 ID */
        @Schema(description = "사용자 ID", example = "10")
        Long userId,
        /** 법인 ID */
        @Schema(description = "법인 ID", example = "20")
        Long corporateId,
        /** 생성 여부 */
        @Schema(description = "생성 여부", example = "true")
        Boolean created
) {
}
