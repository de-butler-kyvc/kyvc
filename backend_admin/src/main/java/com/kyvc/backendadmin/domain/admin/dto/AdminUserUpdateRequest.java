package com.kyvc.backendadmin.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

// 관리자 계정 수정 요청 DTO
/**
 * 관리자 계정 수정 요청 DTO입니다.
 *
 * <p>기존 관리자 계정의 이름과 상태 변경에 사용합니다.</p>
 */
@Schema(description = "관리자 계정 수정 요청")
public record AdminUserUpdateRequest(
        @Schema(description = "관리자 이름", example = "Backend Admin")
        @NotBlank String name, // 관리자 이름
        @Schema(description = "관리자 상태", example = "ACTIVE")
        @NotBlank String status // 관리자 상태
) {
}
