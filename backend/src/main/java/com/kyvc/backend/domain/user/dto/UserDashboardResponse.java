package com.kyvc.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 법인 사용자 대시보드 응답
 *
 * @param userId 사용자 ID
 * @param corporateRegistered 법인정보 등록 여부
 * @param corporateId 법인 ID
 * @param corporateName 법인명
 * @param activeKycId 진행 중 KYC ID
 * @param activeKycStatus 진행 중 KYC 상태
 * @param needSupplementCount 보완요청 수
 * @param notificationUnreadCount 미확인 알림 수
 * @param credentialIssued VC 발급 여부
 */
@Schema(description = "법인 사용자 대시보드 응답")
public record UserDashboardResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId, // 사용자 ID
        @Schema(description = "법인정보 등록 여부", example = "true")
        boolean corporateRegistered, // 법인정보 등록 여부
        @Schema(description = "법인 ID", example = "1")
        Long corporateId, // 법인 ID
        @Schema(description = "법인명", example = "주식회사 케이와이브이씨")
        String corporateName, // 법인명
        @Schema(description = "진행 중 KYC ID", example = "1")
        Long activeKycId, // 진행 중 KYC ID
        @Schema(description = "진행 중 KYC 상태", example = "DRAFT")
        String activeKycStatus, // 진행 중 KYC 상태
        @Schema(description = "보완요청 수", example = "0")
        int needSupplementCount, // 보완요청 수
        @Schema(description = "미확인 알림 수", example = "0")
        int notificationUnreadCount, // 미확인 알림 수
        @Schema(description = "VC 발급 여부", example = "false")
        boolean credentialIssued // VC 발급 여부
) {
}
