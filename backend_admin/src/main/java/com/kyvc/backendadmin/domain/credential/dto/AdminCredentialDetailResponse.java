package com.kyvc.backendadmin.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * VC 발급 상세 조회 응답 DTO입니다.
 */
@Schema(description = "VC 발급 상세 조회 응답")
public record AdminCredentialDetailResponse(

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** KYC 상태 */
        @Schema(description = "KYC 상태", example = "APPROVED")
        String kycStatus,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,

        /** 법인명 */
        @Schema(description = "법인명", example = "케이와이브이씨")
        String corporateName,

        /** 사업자등록번호 */
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String businessRegistrationNumber,

        /** Credential 유형 */
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType,

        /** Credential 상태 */
        @Schema(description = "Credential 상태", example = "ISSUING")
        String credentialStatus,

        /** Core 요청 ID */
        @Schema(description = "Core 요청 ID")
        String coreRequestId,

        /** Core 요청 상태 */
        @Schema(description = "Core 요청 상태", example = "QUEUED")
        String coreRequestStatus,

        /** Core 오류 메시지 */
        @Schema(description = "Core 오류 메시지")
        String coreErrorMessage,

        /** XRPL 트랜잭션 해시 */
        @Schema(description = "XRPL 트랜잭션 해시")
        String xrplTxHash,

        /** 발급 시각 */
        @Schema(description = "발급 시각")
        LocalDateTime issuedAt,

        /** 만료 시각 */
        @Schema(description = "만료 시각")
        LocalDateTime expiresAt,

        /** 생성 시각 */
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        /** 수정 시각 */
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
