package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 선택 가능 역할 목록 응답
 *
 * @param roles 역할 목록
 */
@Schema(description = "선택 가능 역할 목록 응답")
public record AuthRoleResponse(
        @Schema(description = "역할 목록")
        List<RoleItem> roles // 역할 목록
) {

    public AuthRoleResponse {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    /**
     * 역할 목록 항목
     *
     * @param roleId 역할 ID
     * @param roleCode 역할 코드
     * @param roleName 역할명
     * @param roleTypeCode 역할 유형 코드
     * @param selected 선택 여부
     */
    @Schema(description = "역할 목록 항목")
    public record RoleItem(
            @Schema(description = "역할 ID", example = "1")
            Long roleId, // 역할 ID
            @Schema(description = "역할 코드", example = "CORPORATE_USER")
            String roleCode, // 역할 코드
            @Schema(description = "역할명", example = "법인 사용자")
            String roleName, // 역할명
            @Schema(description = "역할 유형 코드", example = "USER")
            String roleTypeCode, // 역할 유형 코드
            @Schema(description = "선택 여부", example = "true")
            boolean selected // 선택 여부
    ) {
    }
}
