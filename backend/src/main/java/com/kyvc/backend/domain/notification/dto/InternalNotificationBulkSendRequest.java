package com.kyvc.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 내부 대량 알림 발송 요청
 *
 * @param targetType 대상 유형
 * @param templateCode 템플릿 코드
 * @param payload 템플릿 payload
 * @param userIds 사용자 ID 목록
 */
@Schema(description = "내부 대량 알림 발송 요청")
public record InternalNotificationBulkSendRequest(
        @Schema(description = "대상 유형", example = "USER_IDS")
        String targetType, // 대상 유형
        @Schema(description = "템플릿 코드", example = "KYC_SUBMITTED")
        String templateCode, // 템플릿 코드
        @Schema(description = "템플릿 payload")
        Map<String, Object> payload, // 템플릿 payload
        @Schema(description = "사용자 ID 목록")
        List<Long> userIds // 사용자 ID 목록
) {
}
