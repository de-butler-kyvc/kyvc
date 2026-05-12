package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 권한 그룹 생성 응답 DTO입니다.
 */
@Schema(description = "관리자 권한 그룹 생성 응답")
public record AdminRoleCreateResponse(
        /** 권한 그룹 ID */
        @Schema(description = "권한 그룹 ID", example = "7")
        Long roleId,
        /** 생성 여부 */
        @Schema(description = "생성 여부", example = "true")
        Boolean created
) {
}
