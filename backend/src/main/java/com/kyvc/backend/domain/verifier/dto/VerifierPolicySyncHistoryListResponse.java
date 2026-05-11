package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifier 정책 동기화 이력 목록 응답
 *
 * @param items 정책 동기화 이력 목록
 * @param page 페이징 정보
 */
@Schema(description = "Verifier 정책 동기화 이력 목록 응답")
public record VerifierPolicySyncHistoryListResponse(
        @Schema(description = "정책 동기화 이력 목록")
        List<Item> items, // 정책 동기화 이력 목록
        @Schema(description = "페이징 정보")
        PageInfo page // 페이징 정보
) {

    /**
     * 정책 동기화 이력 항목
     *
     * @param historyId 이력 ID
     * @param syncStatus 동기화 상태
     * @param syncedAt 동기화 일시
     * @param summary 요약
     */
    public record Item(
            @Schema(description = "이력 ID", example = "1")
            Long historyId, // 이력 ID
            @Schema(description = "동기화 상태", example = "SUCCESS")
            String syncStatus, // 동기화 상태
            @Schema(description = "동기화 일시", example = "2026-05-11T10:00:00")
            LocalDateTime syncedAt, // 동기화 일시
            @Schema(description = "요약", example = "Issuer policy fetched")
            String summary // 요약
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
