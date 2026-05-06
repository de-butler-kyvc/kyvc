package com.kyvc.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 알림 응답
 *
 * @param notificationId 알림 ID
 * @param notificationType 알림 유형 코드
 * @param title 알림 제목
 * @param message 알림 메시지
 * @param read 읽음 여부
 * @param createdAt 생성 일시
 */
@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "1")
        Long notificationId, // 알림 ID
        @Schema(description = "알림 유형 코드", example = "NEED_SUPPLEMENT")
        String notificationType, // 알림 유형 코드
        @Schema(description = "알림 제목", example = "보완서류 제출이 필요합니다.")
        String title, // 알림 제목
        @Schema(description = "알림 메시지", example = "보완요청 내용을 확인해 주세요.")
        String message, // 알림 메시지
        @Schema(description = "읽음 여부", example = "false")
        Boolean read, // 읽음 여부
        @Schema(description = "생성 일시", example = "2026-05-05T10:30:00")
        LocalDateTime createdAt // 생성 일시
) {
}
