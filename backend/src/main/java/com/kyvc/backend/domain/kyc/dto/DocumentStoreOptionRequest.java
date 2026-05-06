package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 원본서류 저장 옵션 변경 요청
 *
 * @param storeOption 원본서류 저장 옵션
 */
@Schema(description = "원본서류 저장 옵션 변경 요청")
public record DocumentStoreOptionRequest(
        @Schema(description = "원본서류 저장 옵션", example = "STORE")
        @NotBlank(message = "원본서류 저장 옵션은 필수입니다.")
        String storeOption // 원본서류 저장 옵션
) {
}
