package com.kyvc.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 역할 선택 요청
 *
 * @param roleCode 선택 역할 코드
 */
@Schema(description = "역할 선택 요청")
public record AuthRoleSelectRequest(
        @Schema(description = "선택 역할 코드", example = "CORPORATE_USER")
        @NotBlank(message = "역할 코드는 필수입니다.")
        String roleCode // 선택 역할 코드
) {
}
