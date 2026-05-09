package com.kyvc.backendadmin.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * KYC 신청 상세 조회 응답 DTO입니다.
 */
@Schema(description = "KYC 신청 상세 조회 응답")
public record AdminKycApplicationDetailResponse(

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,

        /** 법인명 */
        @Schema(description = "법인명", example = "케이와이브이씨")
        String corporateName,

        /** 사업자등록번호 */
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNumber,

        /** 대표자명 */
        @Schema(description = "대표자명", example = "홍길동")
        String representativeName,

        /** 신청 채널 코드 */
        @Schema(description = "신청 채널 코드", example = "WEB")
        String applicationChannelCode,

        /** 금융기관 코드 */
        @Schema(description = "금융기관 코드", example = "BANK001")
        String financeInstitutionCode,

        /** 금융기관 지점 코드 */
        @Schema(description = "금융기관 지점 코드", example = "BR001")
        String financeBranchCode,

        /** 금융기관 담당자 사용자 ID */
        @Schema(description = "금융기관 담당자 사용자 ID", example = "1")
        Long financeStaffUserId,

        /** 금융기관 고객 번호 */
        @Schema(description = "금융기관 고객 번호", example = "CUST-001")
        String financeCustomerNo,

        /** 방문 일시 */
        @Schema(description = "방문 일시")
        LocalDateTime visitedAt,

        /** KYC 신청 상태 */
        @Schema(description = "KYC 신청 상태", example = "SUBMITTED")
        String status,

        /** AI 심사 상태 */
        @Schema(description = "AI 심사 상태", example = "SUCCESS")
        String aiReviewStatus,

        /** AI 심사 결과 */
        @Schema(description = "AI 심사 결과", example = "PASS")
        String aiReviewResult,

        /** 제출 문서 수 */
        @Schema(description = "제출 문서 수", example = "3")
        Long documentCount,

        /** Credential 발급 상태 */
        @Schema(description = "Credential 발급 상태", example = "VALID")
        String credentialStatus,

        /** KYC 제출 시각 */
        @Schema(description = "KYC 제출 시각")
        LocalDateTime submittedAt,

        /** KYC 수정 시각 */
        @Schema(description = "KYC 수정 시각")
        LocalDateTime updatedAt,

        /** 수동 심사 상태 */
        @Schema(description = "수동 심사 상태", example = "MANUAL_REVIEW")
        String manualReviewStatus,

        /** 최근 Core 요청 상태 */
        @Schema(description = "최근 Core 요청 상태", example = "SUCCESS")
        String latestCoreRequestStatus,

        /** 최근 심사 이력 액션 유형 */
        @Schema(description = "최근 심사 이력 액션 유형", example = "APPROVE")
        String latestReviewActionType,

        /** 최근 심사 이력 의견 */
        @Schema(description = "최근 심사 이력 의견", example = "서류 검토 완료")
        String latestReviewComment,

        /** 최근 심사 이력 생성 시각 */
        @Schema(description = "최근 심사 이력 생성 시각")
        LocalDateTime latestReviewCreatedAt
) {
}
