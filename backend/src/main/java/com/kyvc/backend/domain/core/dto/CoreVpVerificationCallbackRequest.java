package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Core VP 검증 Callback 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param status Callback 처리 상태
 * @param resultSummary 검증 결과 요약
 * @param replaySuspected 재사용 의심 여부
 * @param errorMessage 실패 메시지
 * @param verifiedAt 검증 완료 시각
 */
@Schema(description = "Core VP 검증 Callback 요청")
public record CoreVpVerificationCallbackRequest(
        @Schema(description = "Core 요청 ID", example = "36ad38f0-d3bd-41a1-8c4e-079dcaec3075")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Callback 처리 상태", example = "SUCCESS")
        String status, // Callback 처리 상태
        @Schema(description = "검증 결과 요약", example = "VP verification completed")
        String resultSummary, // 검증 결과 요약
        @Schema(description = "재사용 의심 여부", example = "false")
        Boolean replaySuspected, // 재사용 의심 여부
        @Schema(description = "실패 메시지", example = "VP verification failed")
        String errorMessage, // 실패 메시지
        @Schema(description = "검증 완료 시각", example = "2026-05-06T16:00:00")
        LocalDateTime verifiedAt // 검증 완료 시각
) {
}
