package com.kyvc.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 정보 수정 요청
 *
 * @param userName 사용자명
 * @param phone 사용자 연락처
 * @param notificationEnabledYn 알림 수신 여부
 */
@Schema(description = "내 정보 수정 요청")
public record UserMeUpdateRequest(
        @Schema(description = "사용자명", example = "홍길동")
        String userName, // 사용자명
        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        String phone, // 사용자 연락처
        @Schema(description = "알림 수신 여부", example = "Y")
        String notificationEnabledYn // 알림 수신 여부
) {
}
