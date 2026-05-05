package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 권한 부여 요청 DTO입니다.
 *
 * <p>관리자에게 부여할 admin_roles.role_id 값을 전달합니다.</p>
 */
@Schema(description = "관리자 권한 부여 요청")
public record AdminRoleAssignRequest(
        @Schema(description = "부여할 권한 ID", example = "1")
        @NotNull Long roleId // 부여할 권한 ID
) {
}
