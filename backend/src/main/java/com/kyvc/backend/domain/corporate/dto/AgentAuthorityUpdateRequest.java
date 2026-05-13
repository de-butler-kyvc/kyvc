package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * 대리인 권한 수정 요청
 *
 * @param authorityScope 권한 범위
 * @param authorityStatusCode 권한 상태 코드
 * @param validFrom 권한 시작일
 * @param validTo 권한 종료일
 */
@Schema(description = "대리인 권한 수정 요청")
public record AgentAuthorityUpdateRequest(
        @Schema(description = "권한 범위", example = "KYC 신청 및 문서 제출")
        String authorityScope, // 권한 범위
        @Schema(description = "권한 상태 코드", example = "ACTIVE")
        @NotBlank(message = "권한 상태 코드는 필수입니다.")
        String authorityStatusCode, // 권한 상태 코드
        @Schema(description = "권한 시작일", example = "2026-05-11", nullable = true)
        LocalDate validFrom, // 권한 시작일
        @Schema(description = "권한 종료일", example = "2027-05-10", nullable = true)
        LocalDate validTo // 권한 종료일
) {
}
