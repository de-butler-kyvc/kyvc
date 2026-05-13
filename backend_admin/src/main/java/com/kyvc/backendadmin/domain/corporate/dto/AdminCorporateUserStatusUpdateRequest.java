package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 법인 사용자 상태 변경 요청 DTO입니다.
 *
 * <p>관리자가 법인 사용자 계정 상태를 변경할 때 변경할 상태와 사유를 전달합니다.</p>
 */
@Schema(description = "법인 사용자 상태 변경 요청")
public record AdminCorporateUserStatusUpdateRequest(
        @Schema(description = "변경할 사용자 상태", example = "LOCKED", allowableValues = {
                "PENDING", "ACTIVE", "LOCKED", "INACTIVE", "WITHDRAWN"
        })
        @NotBlank
        String status,
        @Schema(description = "상태 변경 사유", example = "관리자에 의한 계정 잠금")
        String reason
) {
}
