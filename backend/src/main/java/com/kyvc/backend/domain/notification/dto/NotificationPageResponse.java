package com.kyvc.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 알림 페이지 응답
 *
 * @param content 알림 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 알림 수
 * @param totalPages 전체 페이지 수
 */
@Schema(description = "알림 페이지 응답")
public record NotificationPageResponse(
        @Schema(description = "알림 목록")
        List<NotificationResponse> content, // 알림 목록
        @Schema(description = "현재 페이지 번호", example = "0")
        int page, // 현재 페이지 번호
        @Schema(description = "페이지 크기", example = "20")
        int size, // 페이지 크기
        @Schema(description = "전체 알림 수", example = "35")
        long totalElements, // 전체 알림 수
        @Schema(description = "전체 페이지 수", example = "2")
        int totalPages // 전체 페이지 수
) {

    public NotificationPageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
