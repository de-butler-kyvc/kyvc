package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * QR 해석 요청
 *
 * @param qrPayload QR Payload JSON 문자열
 */
@Schema(description = "QR 해석 요청")
public record QrResolveRequest(
        @Schema(description = "QR Payload JSON 문자열", example = "{\"type\":\"VP_REQUEST\",\"requestId\":\"vp-req-001\",\"nonce\":\"nonce\",\"challenge\":\"challenge\"}")
        String qrPayload // QR Payload JSON 문자열
) {
}
