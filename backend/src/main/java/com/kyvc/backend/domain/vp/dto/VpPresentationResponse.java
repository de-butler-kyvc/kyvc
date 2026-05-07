package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * VP 제출 응답
 *
 * @param presentationId VP 제출 ID
 * @param requestId VP 요청 ID
 * @param credentialId 제출 Credential ID
 * @param status VP 제출 상태
 * @param submittedAt 제출 일시
 * @param message 제출 안내 메시지
 */
@Schema(description = "VP 제출 응답")
public record VpPresentationResponse(
        @Schema(description = "VP 제출 ID", example = "1")
        Long presentationId, // VP 제출 ID
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "제출 Credential ID", example = "1")
        Long credentialId, // 제출 Credential ID
        @Schema(description = "VP 제출 상태", example = "PRESENTED")
        String status, // VP 제출 상태
        @Schema(description = "제출 일시", example = "2026-05-07T16:00:00")
        LocalDateTime submittedAt, // 제출 일시
        @Schema(description = "제출 안내 메시지", example = "VP 제출이 접수되었습니다. 검증 결과는 추후 확인할 수 있습니다.")
        String message // 제출 안내 메시지
) {
}
