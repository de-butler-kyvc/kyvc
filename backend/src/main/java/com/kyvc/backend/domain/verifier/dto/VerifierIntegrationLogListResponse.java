package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifier 연동 로그 목록 응답
 *
 * @param items 연동 로그 목록
 * @param page 페이징 정보
 */
@Schema(description = "Verifier 연동 로그 목록 응답")
public record VerifierIntegrationLogListResponse(
        @Schema(description = "연동 로그 목록")
        List<Item> items, // 연동 로그 목록
        @Schema(description = "페이징 정보")
        PageInfo page // 페이징 정보
) {

    /**
     * Verifier 연동 로그 항목
     *
     * @param logId 로그 ID
     * @param actionTypeCode 작업 유형 코드
     * @param status 처리 상태
     * @param requestedAt 요청 일시
     * @param completedAt 완료 일시
     */
    public record Item(
            @Schema(description = "로그 ID", example = "1")
            Long logId, // 로그 ID
            @Schema(description = "작업 유형 코드", example = "VP_VERIFY")
            String actionTypeCode, // 작업 유형 코드
            @Schema(description = "처리 상태", example = "SUCCESS")
            String status, // 처리 상태
            @Schema(description = "요청 일시", example = "2026-05-11T10:00:00")
            LocalDateTime requestedAt, // 요청 일시
            @Schema(description = "완료 일시", example = "2026-05-11T10:00:01")
            LocalDateTime completedAt // 완료 일시
    ) {
    }

    /**
     * 페이징 정보
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param totalElements 전체 건수
     * @param totalPages 전체 페이지 수
     */
    public record PageInfo(
            int page, // 페이지 번호
            int size, // 페이지 크기
            long totalElements, // 전체 건수
            int totalPages // 전체 페이지 수
    ) {
    }
}
