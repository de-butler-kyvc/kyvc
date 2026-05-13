package com.kyvc.backendadmin.domain.credential.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Credential 상세 조회 응답 DTO입니다.
 */
@Schema(description = "Credential 상세 조회 응답")
public record AdminCredentialDetailResponse(

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** 외부 Credential ID */
        @Schema(description = "외부 Credential ID", example = "kyvc-credential-550e8400-e29b-41d4-a716-446655440000")
        String credentialExternalId,

        /** Credential 유형 코드 */
        @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
        String credentialTypeCode,

        /** Credential 상태 코드 */
        @Schema(description = "Credential 상태 코드", example = "ISSUING")
        String credentialStatusCode,

        /** Issuer DID */
        @Schema(description = "Issuer DID", example = "did:kyvc:backend-admin")
        String issuerDid,

        /** Holder DID */
        @Schema(description = "Holder DID")
        String holderDid,

        /** Holder XRPL 주소 */
        @Schema(description = "Holder XRPL 주소")
        String holderXrplAddress,

        /** XRPL 트랜잭션 해시 */
        @Schema(description = "XRPL 트랜잭션 해시")
        String xrplTxHash,

        /** Credential Status ID */
        @Schema(description = "Credential Status ID")
        String credentialStatusId,

        /** Wallet 저장 여부 */
        @Schema(description = "Wallet 저장 여부", example = "N")
        String walletSavedYn,

        /** Wallet 저장 시각 */
        @Schema(description = "Wallet 저장 시각")
        LocalDateTime walletSavedAt,

        /** 발급 시각 */
        @Schema(description = "발급 시각")
        LocalDateTime issuedAt,

        /** 만료 시각 */
        @Schema(description = "만료 시각")
        LocalDateTime expiresAt,

        /** 폐기 시각 */
        @Schema(description = "폐기 시각")
        LocalDateTime revokedAt,

        /** 생성 시각 */
        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        /** 수정 시각 */
        @Schema(description = "수정 시각")
        LocalDateTime updatedAt,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "10")
        Long corporateId,

        /** 법인명 */
        @Schema(description = "법인명", example = "모의 재심사 법인")
        String corporateName,

        /** 사업자등록번호 */
        @Schema(description = "사업자등록번호", example = "999-88-77777")
        String businessRegistrationNo,

        /** 법인 전화번호 */
        @Schema(description = "법인 전화번호", example = "02-9999-8888")
        String corporatePhone,

        /** KYC 신청 ID */
        @Schema(description = "KYC 신청 ID", example = "100")
        Long kycId,

        /** KYC 상태 코드 */
        @Schema(description = "KYC 상태 코드", example = "APPROVED")
        String kycStatusCode,

        /** AI 심사 상태 코드 */
        @Schema(description = "AI 심사 상태 코드", example = "SUCCESS")
        String aiReviewStatusCode,

        /** 최신 Core 요청 ID */
        @Schema(hidden = true)
        @JsonIgnore
        String coreRequestId,

        /** 최신 Core 요청 상태 코드 */
        @Schema(description = "최신 Core 요청 상태 코드", example = "QUEUED")
        String coreRequestStatusCode,

        /** 최신 Core 요청 오류 메시지 */
        @Schema(description = "최신 Core 요청 오류 메시지")
        String coreErrorMessage,

        /** 최근 상태 변경 이력 */
        @Schema(description = "최근 상태 변경 이력")
        List<AdminCredentialStatusHistoryResponse> recentStatusHistories
) {
}
