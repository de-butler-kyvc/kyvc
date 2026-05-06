package com.kyvc.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내부 알림 발송 응답
 *
 * @param notificationId 알림 ID
 * @param sendStatus 발송 상태
 */
@Schema(description = "내부 알림 발송 응답")
public record InternalNotificationSendResponse(
        @Schema(description = "알림 ID", example = "10")
        Long notificationId, // 알림 ID
        @Schema(description = "발송 상태", example = "SAVED")
        String sendStatus // 발송 상태
) {
}
