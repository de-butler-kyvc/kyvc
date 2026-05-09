package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 법인 대리인 조회 응답 DTO입니다.
 */
@Schema(description = "법인 대리인 조회 응답")
public record AdminCorporateAgentResponse(
        @Schema(description = "대리인 ID", example = "1")
        Long agentId,
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,
        @Schema(description = "대리인명", example = "김대리")
        String agentName,
        @Schema(description = "대리인 생년월일", example = "1990-01-01")
        LocalDate agentBirthDate,
        @Schema(description = "대리인 전화번호", example = "010-9876-5432")
        String agentPhone,
        @Schema(description = "대리인 이메일", example = "agent@kyvc.local")
        String agentEmail,
        @Schema(description = "권한 범위", example = "KYC 신청 및 보완 제출")
        String authorityScope,
        @Schema(description = "권한 상태 코드", example = "ACTIVE")
        String authorityStatusCode,
        @Schema(description = "신분증 문서 ID", example = "100")
        Long identityDocumentId,
        @Schema(description = "신분증 문서명", example = "agent-id.pdf")
        String identityDocumentName,
        @Schema(description = "위임장 문서 ID", example = "101")
        Long delegationDocumentId,
        @Schema(description = "위임장 문서명", example = "power-of-attorney.pdf")
        String delegationDocumentName,
        @Schema(description = "유효 시작일", example = "2026-01-01")
        LocalDate validFrom,
        @Schema(description = "유효 종료일", example = "2026-12-31")
        LocalDate validTo,
        @Schema(description = "생성 일시")
        LocalDateTime createdAt,
        @Schema(description = "수정 일시")
        LocalDateTime updatedAt
) {
}
