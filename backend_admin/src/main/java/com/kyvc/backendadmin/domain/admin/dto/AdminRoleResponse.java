package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 권한 목록 조회 응답 DTO입니다.
 *
 * <p>admin_roles 테이블의 권한 ID, 권한 코드, 권한 이름, 상태를 클라이언트에 전달합니다.</p>
 */
@Schema(description = "관리자 권한 응답")
public record AdminRoleResponse(
        @Schema(description = "권한 ID", example = "1")
        Long roleId, // 권한 ID
        @Schema(description = "권한 코드", example = "SYSTEM_ADMIN")
        String roleCode, // 권한 코드
        @Schema(description = "권한 이름", example = "시스템 관리자")
        String roleName, // 권한 이름
        @Schema(description = "권한 상태", example = "ACTIVE")
        String status // 권한 상태
) {
}
