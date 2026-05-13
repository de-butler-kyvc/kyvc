package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 법인 사용자 상세 응답 DTO입니다.
 *
 * <p>사용자 정보, 연결된 법인 정보, 최근 KYC 정보를 함께 전달합니다.</p>
 */
@Schema(description = "법인 사용자 상세 응답")
public record AdminCorporateUserDetailResponse(
        @Schema(description = "사용자 정보")
        UserInfo user,
        @Schema(description = "법인 정보")
        CorporateInfo corporate,
        @Schema(description = "최근 KYC 정보")
        KycInfo latestKyc
) {

    /**
     * 법인 사용자 계정 요약 DTO입니다.
     */
    @Schema(description = "법인 사용자 계정 정보")
    public record UserInfo(
            @Schema(description = "사용자 ID", example = "1")
            Long userId,
            @Schema(description = "사용자 이메일", example = "corp@kyvc.local")
            String email,
            @Schema(description = "사용자 이름", example = "홍길동")
            String userName,
            @Schema(description = "사용자 연락처", example = "010-1234-5678")
            String phone,
            @Schema(description = "온보딩 법인명", example = "케이와이브이씨")
            String onboardingCorporateName,
            @Schema(description = "사용자 유형", example = "CORPORATE_USER")
            String userType,
            @Schema(description = "사용자 상태", example = "ACTIVE")
            String status,
            @Schema(description = "사용자 생성 시각")
            LocalDateTime createdAt,
            @Schema(description = "사용자 수정 시각")
            LocalDateTime updatedAt
    ) {
    }

    /**
     * 법인 사용자 상세 화면에 표시할 법인 요약 DTO입니다.
     */
    @Schema(description = "법인 요약 정보")
    public record CorporateInfo(
            @Schema(description = "법인 ID", example = "10")
            Long corporateId,
            @Schema(description = "법인명", example = "케이와이브이씨")
            String corporateName,
            @Schema(description = "법인 연락처", example = "02-1234-5678")
            String corporatePhone,
            @Schema(description = "사업자등록번호", example = "123-45-67890")
            String businessRegistrationNo,
            @Schema(description = "대표자명", example = "홍길동")
            String representativeName,
            @Schema(description = "법인 상태", example = "ACTIVE")
            String status
    ) {
    }

    /**
     * 법인 사용자 상세 화면에 표시할 최근 KYC 요약 DTO입니다.
     */
    @Schema(description = "최근 KYC 요약 정보")
    public record KycInfo(
            @Schema(description = "KYC ID", example = "100")
            Long kycId,
            @Schema(description = "KYC 상태", example = "APPROVED")
            String status,
            @Schema(description = "법인 유형", example = "SME")
            String corporateType,
            @Schema(description = "제출 시각")
            LocalDateTime submittedAt,
            @Schema(description = "승인 시각")
            LocalDateTime approvedAt,
            @Schema(description = "반려 시각")
            LocalDateTime rejectedAt
    ) {
    }
}
