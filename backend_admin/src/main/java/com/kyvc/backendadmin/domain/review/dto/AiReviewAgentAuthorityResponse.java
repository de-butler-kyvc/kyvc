package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * AI 대리인 권한 판단 결과 조회 응답 DTO입니다.
 */
@Schema(description = "AI 대리인 권한 판단 결과 조회 응답")
public record AiReviewAgentAuthorityResponse(

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** AI가 판단한 대리인 권한 정보 */
        @Schema(description = "AI가 판단한 대리인 권한 정보")
        AgentAuthority agentAuthority
) {

    /**
     * AI 대리인 권한 판단 항목 DTO입니다.
     */
    @Schema(description = "AI 대리인 권한 판단 항목")
    public record AgentAuthority(

            /** 대리인명 */
            @Schema(description = "대리인명", example = "김대리")
            String agentName,

            /** 권한 범위 */
            @Schema(description = "권한 범위", example = "KYC 신청 및 보완 서류 제출")
            String authorityScope,

            /** 서명 검증 여부 */
            @Schema(description = "서명 검증 여부", example = "Y")
            String signatureVerifiedYn,

            /** 직인 검증 여부 */
            @Schema(description = "직인 검증 여부", example = "Y")
            String sealVerifiedYn,

            /** 권한 유효 여부 */
            @Schema(description = "권한 유효 여부", example = "Y")
            String authorityValidYn,

            /** 신뢰도 점수 */
            @Schema(description = "신뢰도 점수", example = "90.00")
            BigDecimal confidenceScore,

            /** 판단 사유 */
            @Schema(description = "판단 사유", example = "위임장에 신청 권한과 서명이 확인되었습니다.")
            String judgementReason
    ) {
    }
}
