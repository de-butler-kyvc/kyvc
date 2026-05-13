package com.kyvc.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사용자 설정 변경 응답
 *
 * @param updated 변경 완료 여부
 */
@Schema(description = "사용자 설정 변경 응답")
public record UserProfileUpdateResponse(
        @Schema(description = "변경 완료 여부", example = "true")
        boolean updated // 변경 완료 여부
) {
}
