package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * KYC 신청 법인정보 조회 응답 DTO입니다.
 *
 * <p>KYC 신청 ID 기준으로 신청 존재 여부를 검증한 뒤 연결된 법인과 신청자 정보를 전달합니다.</p>
 */
@Schema(description = "KYC 신청 법인정보 응답")
public record AdminKycApplicationCorporateResponse(
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,
        @Schema(description = "KYC 신청 상태", example = "SUBMITTED")
        String kycStatus,
        @Schema(description = "신청 채널 코드", example = "WEB")
        String applicationChannelCode,
        @Schema(description = "금융기관 코드", example = "BANK001")
        String financeInstitutionCode,
        @Schema(description = "금융기관 지점 코드", example = "BR001")
        String financeBranchCode,
        @Schema(description = "금융기관 담당자 사용자 ID", example = "1")
        Long financeStaffUserId,
        @Schema(description = "금융기관 고객 번호", example = "CUST-001")
        String financeCustomerNo,
        @Schema(description = "방문 일시")
        LocalDateTime visitedAt,
        @Schema(description = "법인 유형", example = "SME")
        String corporateType,
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,
        @Schema(description = "법인명", example = "케이와이브이씨")
        String corporateName,
        @Schema(description = "법인 대표 연락처", example = "02-1234-5678")
        String corporatePhone,
        @Schema(description = "법인 유형 코드", example = "SME")
        String corporateTypeCode,
        @Schema(description = "설립일", example = "2020-01-01")
        LocalDate establishedDate,
        @Schema(description = "웹사이트", example = "https://kyvc.example")
        String website,
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
        @Schema(description = "신청 사용자 ID", example = "1")
        Long applicantUserId,
        @Schema(description = "신청 사용자 이메일", example = "corp@kyvc.local")
        String applicantEmail,
        @Schema(description = "제출 시각")
        LocalDateTime submittedAt
) {
}
