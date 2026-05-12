package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 권한 그룹 생성 요청 DTO입니다.
 */
@Schema(description = "관리자 권한 그룹 생성 요청")
public record AdminRoleCreateRequest(
        /** 권한 그룹 코드 */
        @Schema(description = "권한 그룹 코드", example = "POLICY_MANAGER")
        String roleCode,
        /** 권한 그룹명 */
        @Schema(description = "권한 그룹명", example = "정책 관리자")
        String roleName,
        /** 권한 설명 */
        @Schema(description = "권한 설명", example = "정책과 심사 기준을 관리하는 권한 그룹")
        String description,
        /** 권한 상태 */
        @Schema(description = "권한 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        String status
) {
}
