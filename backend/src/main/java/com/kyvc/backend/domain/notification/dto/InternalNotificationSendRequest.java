package com.kyvc.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * 내부 알림 발송 요청
 *
 * @param recipientUserId 수신 사용자 ID
 * @param channel 발송 채널
 * @param type 알림 유형 코드
 * @param title 알림 제목
 * @param message 알림 메시지
 * @param templateCode 템플릿 코드
 * @param data 템플릿 데이터
 */
@Schema(description = "내부 알림 발송 요청")
public record InternalNotificationSendRequest(
        @Schema(description = "수신 사용자 ID", example = "1")
        @NotNull(message = "수신 사용자 ID는 필수입니다.")
        @Positive(message = "수신 사용자 ID는 1 이상이어야 합니다.")
        Long recipientUserId, // 수신 사용자 ID
        @Schema(description = "발송 채널", example = "IN_APP", nullable = true)
        String channel, // 발송 채널
        @Schema(description = "알림 유형 코드", example = "KYC_SUBMITTED")
        @NotBlank(message = "알림 유형 코드는 필수입니다.")
        String type, // 알림 유형 코드
        @Schema(description = "알림 제목", example = "KYC 접수 안내")
        @NotBlank(message = "알림 제목은 필수입니다.")
        String title, // 알림 제목
        @Schema(description = "알림 메시지", example = "KYC 신청이 접수되었습니다.")
        @NotBlank(message = "알림 메시지는 필수입니다.")
        String message, // 알림 메시지
        @Schema(description = "템플릿 코드", example = "KYC_SUBMITTED", nullable = true)
        String templateCode, // 템플릿 코드
        @Schema(description = "템플릿 데이터", nullable = true)
        Map<String, Object> data // 템플릿 데이터
) {
}
