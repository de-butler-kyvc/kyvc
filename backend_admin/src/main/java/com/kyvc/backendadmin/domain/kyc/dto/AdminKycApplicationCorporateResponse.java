package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 신청 법인정보 조회 응답 DTO입니다.
 */
@Schema(description = "KYC 신청 법인정보 응답")
public record AdminKycApplicationCorporateResponse(
        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** KYC 신청 상태 */
        @Schema(description = "KYC 신청 상태", example = "SUBMITTED")
        String kycStatus,

        /** 법인 유형 코드 */
        @Schema(description = "법인 유형 코드", example = "CORPORATION")
        String corporateType,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,

        /** 법인명 */
        @Schema(description = "법인명", example = "케이와이브이씨")
        String corporateName,

        /** 법인 연락처 */
        @Schema(description = "법인 연락처", example = "02-1234-5678")
        String corporatePhone,

        /** 사업자등록번호 */
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNo,

        /** 법인등록번호 */
        @Schema(description = "법인등록번호", example = "110111-1234567")
        String corporateRegistrationNo,

        /** 대표자명 */
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName,

        /** 대표자 연락처 */
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String representativePhone,

        /** 대표자 이메일 */
        @Schema(description = "대표자 이메일", example = "ceo@kyvc.local")
        String representativeEmail,

        /** 대리인명 */
        @Schema(description = "대리인명", example = "김대리")
        String agentName,

        /** 대리인 연락처 */
        @Schema(description = "대리인 연락처", example = "010-9876-5432")
        String agentPhone,

        /** 대리인 이메일 */
        @Schema(description = "대리인 이메일", example = "agent@kyvc.local")
        String agentEmail,

        /** 대리인 권한 범위 */
        @Schema(description = "대리인 권한 범위", example = "KYC 신청 및 보완 제출")
        String agentAuthorityScope,

        /** 주소 */
        @Schema(description = "주소", example = "서울특별시 강남구")
        String address,

        /** 업종 */
        @Schema(description = "업종", example = "핀테크")
        String businessType,

        /** 법인 상태 */
        @Schema(description = "법인 상태", example = "ACTIVE")
        String corporateStatus,

        /** 신청 사용자 ID */
        @Schema(description = "신청 사용자 ID", example = "1")
        Long applicantUserId,

        /** 신청 사용자 이메일 */
        @Schema(description = "신청 사용자 이메일", example = "corp@kyvc.local")
        String applicantEmail,

        /** 제출 시각 */
        @Schema(description = "제출 시각")
        LocalDateTime submittedAt
) {
}
