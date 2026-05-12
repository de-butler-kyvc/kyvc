package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관리자 권한 그룹 수정 요청 DTO입니다.
 */
@Schema(description = "관리자 권한 그룹 수정 요청")
public record AdminRoleUpdateRequest(
        /** 권한 그룹명 */
        @Schema(description = "권한 그룹명", example = "정책 운영 관리자")
        String roleName,
        /** 권한 설명 */
        @Schema(description = "권한 설명", example = "정책 운영과 심사 기준을 관리하는 권한 그룹")
        String description,
        /** 권한 상태 */
        @Schema(description = "권한 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        String status
) {
}
