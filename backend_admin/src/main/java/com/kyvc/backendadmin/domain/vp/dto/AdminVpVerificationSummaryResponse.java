package com.kyvc.backendadmin.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * VP 검증 목록 항목 DTO입니다.
 */
@Schema(description = "VP 검증 목록 항목")
public record AdminVpVerificationSummaryResponse(

        /** VP 검증 ID */
        @Schema(description = "VP 검증 ID", example = "1")
        Long vpVerificationId,

        /** VP 요청 ID */
        @Schema(description = "VP 요청 ID", example = "vp-request-001")
        String vpRequestId,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "900003")
        Long corporateId,

        /** 법인명 */
        @Schema(description = "법인명", example = "VC 발급 테스트 법인")
        String corporateName,

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** 요청자명 */
        @Schema(description = "요청자명", example = "검증기관")
        String requesterName,

        /** 제출 목적 */
        @Schema(description = "제출 목적", example = "대출 심사")
        String purpose,

        /** VP 검증 상태 코드 */
        @Schema(description = "VP 검증 상태 코드", example = "VALID")
        String vpVerificationStatusCode,

        /** Replay 의심 여부 */
        @Schema(description = "Replay 의심 여부", example = "N")
        String replaySuspectedYn,

        /** 요청 시각 */
        @Schema(description = "요청 시각")
        LocalDateTime requestedAt,

        /** 제출 시각 */
        @Schema(description = "제출 시각")
        LocalDateTime presentedAt,

        /** 검증 시각 */
        @Schema(description = "검증 시각")
        LocalDateTime verifiedAt,

        /** 만료 시각 */
        @Schema(description = "만료 시각")
        LocalDateTime expiresAt,

        /** Callback 상태 코드 */
        @Schema(description = "Callback 상태 코드", example = "SENT")
        String callbackStatusCode
) {
}
