package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 권한 그룹 수정 응답 DTO입니다.
 */
@Schema(description = "관리자 권한 그룹 수정 응답")
public record AdminRoleUpdateResponse(
        /** 권한 그룹 ID */
        @Schema(description = "권한 그룹 ID", example = "7")
        Long roleId,
        /** 수정 여부 */
        @Schema(description = "수정 여부", example = "true")
        Boolean updated
) {
}
