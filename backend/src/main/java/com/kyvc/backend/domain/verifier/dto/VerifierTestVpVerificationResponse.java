package com.kyvc.backend.domain.verifier.dto;

import com.kyvc.backend.domain.vp.dto.VpVerificationResultResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Verifier 테스트 VP 검증 응답
 *
 * @param testId 테스트 검증 ID
 * @param status VP 검증 상태
 * @param result 검증 결과 요약
 * @param failureReason 실패 사유
 * @param verifiedAt 검증 일시
 */
@Schema(description = "Verifier 테스트 VP 검증 응답")
public record VerifierTestVpVerificationResponse(
        @Schema(description = "테스트 검증 ID", example = "1")
        Long testId, // 테스트 검증 ID
        @Schema(description = "VP 검증 상태", example = "VALID")
        String status, // VP 검증 상태
        @Schema(description = "검증 결과 요약")
        VpVerificationResultResponse result, // 검증 결과 요약
        @Schema(description = "실패 사유", example = "INVALID_SIGNATURE")
        String failureReason, // 실패 사유
        @Schema(description = "검증 일시", example = "2026-05-10T10:00:00")
        LocalDateTime verifiedAt // 검증 일시
) {
}
