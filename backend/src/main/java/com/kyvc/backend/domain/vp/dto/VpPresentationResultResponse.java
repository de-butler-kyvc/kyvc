package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * VP 제출 결과 응답
 *
 * @param presentationId VP 제출 ID
 * @param requestId VP 요청 ID
 * @param verifierName Verifier명
 * @param purpose 제출 목적
 * @param verificationStatus 검증 상태 코드
 * @param credentialId 제출 Credential ID
 * @param status VP 검증 상태
 * @param result VP 검증 결과
 * @param replaySuspected Replay 의심 여부
 * @param resultSummary 검증 결과 요약
 * @param presentedAt 제출 일시
 * @param verifiedAt 검증 일시
 */
@Schema(description = "VP 제출 결과 응답")
public record VpPresentationResultResponse(
        @Schema(description = "VP 제출 ID", example = "1")
        Long presentationId, // VP 제출 ID
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "Verifier명", example = "Verifier")
        String verifierName, // Verifier명
        @Schema(description = "제출 목적", example = "ACCOUNT_OPENING")
        String purpose, // 제출 목적
        @Schema(description = "검증 상태 코드", example = "VALID")
        String verificationStatus, // 검증 상태 코드
        @Schema(description = "제출 Credential ID", example = "1")
        Long credentialId, // 제출 Credential ID
        @Schema(description = "VP 검증 상태", example = "PRESENTED")
        String status, // VP 검증 상태
        @Schema(description = "VP 검증 결과")
        VpVerificationResultResponse result, // VP 검증 결과
        @Schema(description = "Replay 의심 여부", example = "false")
        boolean replaySuspected, // Replay 의심 여부
        @Schema(description = "검증 결과 요약", example = "VP 검증이 접수되었습니다.")
        String resultSummary, // 검증 결과 요약
        @Schema(description = "제출 일시", example = "2026-05-07T16:00:00")
        LocalDateTime presentedAt, // 제출 일시
        @Schema(description = "검증 일시", example = "2026-05-07T16:05:00")
        LocalDateTime verifiedAt // 검증 일시
) {
    public VpPresentationResultResponse(
            Long presentationId, // VP 제출 ID
            String requestId, // VP 요청 ID
            Long credentialId, // 제출 Credential ID
            String status, // VP 검증 상태
            VpVerificationResultResponse result, // VP 검증 결과
            boolean replaySuspected, // Replay 의심 여부
            String resultSummary, // 검증 결과 요약
            LocalDateTime presentedAt, // 제출 일시
            LocalDateTime verifiedAt // 검증 일시
    ) {
        this(presentationId, requestId, null, null, status, credentialId, status, result, replaySuspected, resultSummary, presentedAt, verifiedAt);
    }
}
