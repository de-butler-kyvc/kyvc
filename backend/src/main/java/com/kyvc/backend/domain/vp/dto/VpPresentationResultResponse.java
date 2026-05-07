package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * VP 제출 결과 응답
 *
 * @param presentationId VP 제출 ID
 * @param requestId VP 요청 ID
 * @param credentialId 제출 Credential ID
 * @param status VP 검증 상태
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
        @Schema(description = "제출 Credential ID", example = "1")
        Long credentialId, // 제출 Credential ID
        @Schema(description = "VP 검증 상태", example = "PRESENTED")
        String status, // VP 검증 상태
        @Schema(description = "Replay 의심 여부", example = "false")
        boolean replaySuspected, // Replay 의심 여부
        @Schema(description = "검증 결과 요약", example = "VP 검증이 접수되었습니다.")
        String resultSummary, // 검증 결과 요약
        @Schema(description = "제출 일시", example = "2026-05-07T16:00:00")
        LocalDateTime presentedAt, // 제출 일시
        @Schema(description = "검증 일시", example = "2026-05-07T16:05:00")
        LocalDateTime verifiedAt // 검증 일시
) {
}
