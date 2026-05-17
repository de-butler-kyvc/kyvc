package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 금융사 VP 요청 취소 응답
 *
 * @param requestId VP 요청 ID
 * @param status VP 요청 상태
 */
@Schema(description = "금융사 VP 요청 취소 응답")
public record FinanceVpRequestCancelResponse(
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "VP 요청 상태", example = "CANCELLED")
        String status // VP 요청 상태
) {
}
