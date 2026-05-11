package com.kyvc.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 내부 대량 알림 발송 응답
 *
 * @param requestedCount 요청 건수
 * @param successCount 성공 건수
 * @param failedCount 실패 건수
 * @param failedUserIds 실패 사용자 ID 목록
 */
@Schema(description = "내부 대량 알림 발송 응답")
public record InternalNotificationBulkSendResponse(
        @Schema(description = "요청 건수", example = "3")
        int requestedCount, // 요청 건수
        @Schema(description = "성공 건수", example = "3")
        int successCount, // 성공 건수
        @Schema(description = "실패 건수", example = "0")
        int failedCount, // 실패 건수
        @Schema(description = "실패 사용자 ID 목록")
        List<Long> failedUserIds // 실패 사용자 ID 목록
) {
}
