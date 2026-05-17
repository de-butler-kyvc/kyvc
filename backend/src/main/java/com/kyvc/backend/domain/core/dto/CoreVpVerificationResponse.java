package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Core VP 검증 요청 응답
 *
 * @param coreRequestId Core 요청 ID
 * @param status 처리 상태
 * @param message 처리 메시지
 * @param requestedAt 요청 시각
 * @param completed 검증 완료 여부
 * @param valid VP 유효 여부
 * @param replaySuspected Replay 의심 여부
 * @param resultSummary 결과 요약
 * @param errors Core 검증 오류 목록
 * @param details Core 검증 상세 데이터
 */
@Schema(description = "Core VP 검증 요청 응답")
public record CoreVpVerificationResponse(
        @Schema(description = "Core 요청 ID", example = "36ad38f0-d3bd-41a1-8c4e-079dcaec3075")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "처리 상태", example = "VALID")
        String status, // 처리 상태
        @Schema(description = "처리 메시지", example = "VP verification request accepted by core.")
        String message, // 처리 메시지
        @Schema(description = "요청 시각", example = "2026-05-06T10:20:00")
        LocalDateTime requestedAt, // 요청 시각
        @Schema(description = "검증 완료 여부", example = "true")
        Boolean completed, // 검증 완료 여부
        @Schema(description = "VP 유효 여부", example = "true")
        Boolean valid, // VP 유효 여부
        @Schema(description = "Replay 의심 여부", example = "false")
        Boolean replaySuspected, // Replay 의심 여부
        @Schema(description = "결과 요약", example = "VP 검증 성공")
        String resultSummary, // 결과 요약
        @Schema(description = "Core 검증 오류 목록")
        List<String> errors, // Core 검증 오류 목록
        @Schema(description = "Core 검증 상세 데이터")
        Map<String, Object> details // Core 검증 상세 데이터
) {
    public CoreVpVerificationResponse(
            String coreRequestId, // Core 요청 ID
            String status, // 처리 상태
            String message, // 처리 메시지
            LocalDateTime requestedAt, // 요청 시각
            Boolean completed, // 검증 완료 여부
            Boolean valid, // VP 유효 여부
            Boolean replaySuspected, // Replay 의심 여부
            String resultSummary // 결과 요약
    ) {
        this(coreRequestId, status, message, requestedAt, completed, valid, replaySuspected, resultSummary, List.of(), Map.of());
    }
}
