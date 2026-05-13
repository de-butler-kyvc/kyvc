package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Verifier 기업 재인증 요청 생성 응답
 *
 * @param requestId VP 요청 ID
 * @param status VP 요청 상태
 * @param qrPayload QR Payload
 * @param expiresAt 만료 일시
 */
@Schema(description = "Verifier 기업 재인증 요청 생성 응답")
public record VerifierReAuthRequestCreateResponse(
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "VP 요청 상태", example = "REQUESTED")
        String status, // VP 요청 상태
        @Schema(description = "QR Payload")
        String qrPayload, // QR Payload
        @Schema(description = "만료 일시", example = "2026-05-10T10:10:00")
        LocalDateTime expiresAt // 만료 일시
) {
}
