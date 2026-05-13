package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 법인 변경 이력 페이지 응답
 *
 * @param items 변경 이력 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "법인 변경 이력 페이지 응답")
public record CorporateChangeHistoryResponse(
        @Schema(description = "변경 이력 목록")
        List<Item> items, // 변경 이력 목록
        @Schema(description = "현재 페이지 번호", example = "0")
        int page, // 현재 페이지 번호
        @Schema(description = "페이지 크기", example = "20")
        int size, // 페이지 크기
        @Schema(description = "전체 건수", example = "35")
        long totalElements, // 전체 건수
        @Schema(description = "전체 페이지 수", example = "2")
        int totalPages // 전체 페이지 수
) {

    public CorporateChangeHistoryResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * 법인 변경 이력 항목
     *
     * @param auditLogId 감사로그 ID
     * @param actorType 행위자 유형 코드
     * @param actorId 행위자 ID
     * @param actionType 작업 유형
     * @param targetType 감사 대상 유형 코드
     * @param targetId 대상 ID
     * @param requestSummary 요청 요약
     * @param createdAt 생성 일시
     */
    @Schema(description = "법인 변경 이력 항목")
    public record Item(
            @Schema(description = "감사로그 ID", example = "1")
            Long auditLogId, // 감사로그 ID
            @Schema(description = "행위자 유형 코드", example = "USER")
            String actorType, // 행위자 유형 코드
            @Schema(description = "행위자 ID", example = "1")
            Long actorId, // 행위자 ID
            @Schema(description = "작업 유형", example = "CORPORATE_AGENT_AUTHORITY_UPDATE")
            String actionType, // 작업 유형
            @Schema(description = "감사 대상 유형 코드", example = "CORPORATE")
            String targetType, // 감사 대상 유형 코드
            @Schema(description = "대상 ID", example = "10")
            Long targetId, // 대상 ID
            @Schema(description = "요청 요약", example = "대리인 권한 수정")
            String requestSummary, // 요청 요약
            @Schema(description = "생성 일시", example = "2026-05-11T10:30:00")
            LocalDateTime createdAt // 생성 일시
    ) {
    }
}
