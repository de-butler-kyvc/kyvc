package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 법인 사용자 계정 삭제 응답 DTO입니다.
 */
@Schema(description = "법인 사용자 계정 삭제 응답")
public record AdminCorporateUserDeleteResponse(
        /** 사용자 ID */
        @Schema(description = "사용자 ID", example = "10")
        Long userId,
        /** 삭제 처리 여부 */
        @Schema(description = "삭제 처리 여부", example = "true")
        Boolean deleted
) {
}
