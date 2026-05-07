package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * QR 해석 응답
 *
 * @param type QR 유형
 * @param targetId 이동 대상 ID
 * @param offerId Credential Offer ID
 * @param requestId VP 요청 ID
 * @param nextAction 다음 화면 이동 동작
 * @param message QR 해석 메시지
 */
@Schema(description = "QR 해석 응답")
public record QrResolveResponse(
        @Schema(description = "QR 유형", example = "VP_REQUEST")
        String type, // QR 유형
        @Schema(description = "이동 대상 ID", example = "vp-req-001")
        String targetId, // 이동 대상 ID
        @Schema(description = "Credential Offer ID", example = "1")
        Long offerId, // Credential Offer ID
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "다음 화면 이동 동작", example = "OPEN_VP_REQUEST")
        String nextAction, // 다음 화면 이동 동작
        @Schema(description = "QR 해석 메시지", example = "VP 요청 QR입니다.")
        String message // QR 해석 메시지
) {
}
