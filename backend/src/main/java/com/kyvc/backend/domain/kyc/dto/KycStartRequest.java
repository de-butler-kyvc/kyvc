package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * KYC 신청 시작 요청
 *
 * @param corporateTypeCode 법인 유형 코드
 */
@Schema(description = "KYC 신청 시작 요청")
public record KycStartRequest(
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        @NotBlank(message = "법인 유형 코드는 필수입니다.")
        String corporateTypeCode // 법인 유형 코드
) {
}
