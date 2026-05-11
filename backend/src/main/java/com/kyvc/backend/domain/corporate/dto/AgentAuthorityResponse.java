package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 대리인 권한 응답
 *
 * @param agentId 대리인 ID
 * @param corporateId 법인 ID
 * @param authorityScope 권한 범위
 * @param authorityStatusCode 권한 상태 코드
 * @param validFrom 권한 시작일
 * @param validTo 권한 종료일
 */
@Schema(description = "대리인 권한 응답")
public record AgentAuthorityResponse(
        @Schema(description = "대리인 ID", example = "1")
        Long agentId, // 대리인 ID
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "권한 범위", example = "KYC 신청 및 문서 제출")
        String authorityScope, // 권한 범위
        @Schema(description = "권한 상태 코드", example = "ACTIVE")
        String authorityStatusCode, // 권한 상태 코드
        @Schema(description = "권한 시작일", example = "2026-05-11", nullable = true)
        LocalDate validFrom, // 권한 시작일
        @Schema(description = "권한 종료일", example = "2027-05-10", nullable = true)
        LocalDate validTo // 권한 종료일
) {
}
