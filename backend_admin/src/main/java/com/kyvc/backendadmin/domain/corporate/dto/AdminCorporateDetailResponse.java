package com.kyvc.backendadmin.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 법인 상세 응답 DTO입니다.
 *
 * <p>corporateId 기준으로 조회한 법인 기본정보, 대표자 정보, 대리인 정보와
 * 최근 KYC 요약을 전달합니다.</p>
 */
@Schema(description = "법인 상세 응답")
public record AdminCorporateDetailResponse(
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "사용자 이메일", example = "corp@kyvc.local")
        String userEmail,
        @Schema(description = "사용자 이름", example = "홍길동")
        String userName,
        @Schema(description = "사용자 연락처", example = "010-1234-5678")
        String phone,
        @Schema(description = "온보딩 법인명", example = "케이와이브이씨")
        String onboardingCorporateName,
        @Schema(description = "사용자 상태", example = "ACTIVE")
        String userStatus,
        @Schema(description = "법인명", example = "케이와이브이씨")
        String corporateName,
        @Schema(description = "법인 연락처", example = "02-1234-5678")
        String corporatePhone,
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo,
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo,
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName,
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String representativePhone,
        @Schema(description = "대표자 이메일", example = "ceo@kyvc.local")
        String representativeEmail,
        @Schema(description = "대리인명", example = "김대리")
        String agentName,
        @Schema(description = "대리인 연락처", example = "010-9876-5432")
        String agentPhone,
        @Schema(description = "대리인 이메일", example = "agent@kyvc.local")
        String agentEmail,
        @Schema(description = "대리인 권한 범위", example = "KYC 신청 및 보완 제출")
        String agentAuthorityScope,
        @Schema(description = "주소", example = "서울특별시 강남구")
        String address,
        @Schema(description = "업종", example = "핀테크")
        String businessType,
        @Schema(description = "법인 상태", example = "ACTIVE")
        String corporateStatus,
        @Schema(description = "최근 KYC 정보")
        AdminCorporateUserDetailResponse.KycInfo latestKyc,
        @Schema(description = "법인 생성 시각")
        LocalDateTime createdAt,
        @Schema(description = "법인 수정 시각")
        LocalDateTime updatedAt
) {
}
