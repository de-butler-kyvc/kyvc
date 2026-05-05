package com.kyvc.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 읽지 않은 알림 수 응답
 *
 * @param unreadCount 읽지 않은 알림 수
 */
@Schema(description = "읽지 않은 알림 수 응답")
public record NotificationUnreadCountResponse(
        @Schema(description = "읽지 않은 알림 수", example = "3")
        long unreadCount // 읽지 않은 알림 수
) {
}
