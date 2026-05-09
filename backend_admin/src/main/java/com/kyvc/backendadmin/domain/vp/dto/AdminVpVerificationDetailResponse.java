package com.kyvc.backendadmin.domain.vp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * VP 검증 상세 응답 DTO입니다.
 */
@Schema(description = "VP 검증 상세 응답")
public record AdminVpVerificationDetailResponse(

        /** VP 검증 ID */
        @Schema(description = "VP 검증 ID", example = "1")
        Long vpVerificationId,

        /** Credential ID */
        @Schema(description = "Credential ID", example = "1")
        Long credentialId,

        /** 법인 ID */
        @Schema(description = "법인 ID", example = "900003")
        Long corporateId,

        /** 법인명 */
        @Schema(description = "법인명", example = "VC 발급 테스트 법인")
        String corporateName,

        /** 요청 nonce */
        @Schema(description = "요청 nonce")
        String requestNonce,

        /** VP 요청 ID */
        @Schema(description = "VP 요청 ID")
        String vpRequestId,

        /** 제출 목적 */
        @Schema(description = "제출 목적", example = "대출 심사")
        String purpose,

        /** 요청자명 */
        @Schema(description = "요청자명", example = "검증기관")
        String requesterName,

        /** 요구 Claim JSON */
        @Schema(description = "요구 Claim JSON")
        String requiredClaimsJson,

        /** VP 검증 상태 코드 */
        @Schema(description = "VP 검증 상태 코드", example = "VALID")
        String vpVerificationStatusCode,

        /** Replay 의심 여부 */
        @Schema(description = "Replay 의심 여부", example = "N")
        String replaySuspectedYn,

        /** 검증 결과 요약 */
        @Schema(description = "검증 결과 요약")
        String resultSummary,

        /** 요청 시각 */
        @Schema(description = "요청 시각")
        LocalDateTime requestedAt,

        /** 제출 시각 */
        @Schema(description = "제출 시각")
        LocalDateTime presentedAt,

        /** 검증 시각 */
        @Schema(description = "검증 시각")
        LocalDateTime verifiedAt,

        /** 만료 시각 */
        @Schema(description = "만료 시각")
        LocalDateTime expiresAt,

        /** Core 요청 ID */
        @Schema(hidden = true)
        @JsonIgnore
        String coreRequestId,

        /** Core 요청 상태 코드 */
        @Schema(description = "Core 요청 상태 코드", example = "SUCCESS")
        String coreRequestStatusCode,

        /** Callback 상태 코드 */
        @Schema(description = "Callback 상태 코드", example = "SENT")
        String callbackStatusCode,

        /** Callback 전송 시각 */
        @Schema(description = "Callback 전송 시각")
        LocalDateTime callbackSentAt,

        /** 권한 검증 결과 JSON */
        @Schema(description = "권한 검증 결과 JSON")
        String permissionResultJson,

        /** Credential 정보 */
        @Schema(description = "Credential 정보")
        CredentialInfo credential,

        /** Verifier 정보 */
        @Schema(description = "Verifier 정보")
        VerifierInfo verifier
) {

    /**
     * VP 검증 상세의 Credential 정보입니다.
     */
    @Schema(description = "VP 검증 상세 Credential 정보")
    public record CredentialInfo(

            /** Credential ID */
            @Schema(description = "Credential ID", example = "1")
            Long credentialId,

            /** 외부 Credential ID */
            @Schema(description = "외부 Credential ID")
            String credentialExternalId,

            /** Credential 유형 코드 */
            @Schema(description = "Credential 유형 코드", example = "KYC_CREDENTIAL")
            String credentialTypeCode,

            /** Credential 상태 코드 */
            @Schema(description = "Credential 상태 코드", example = "VALID")
            String credentialStatusCode,

            /** Issuer DID */
            @Schema(description = "Issuer DID")
            String issuerDid,

            /** Holder DID */
            @Schema(description = "Holder DID")
            String holderDid,

            /** XRPL 트랜잭션 해시 */
            @Schema(description = "XRPL 트랜잭션 해시")
            String xrplTxHash,

            /** Wallet 저장 여부 */
            @Schema(description = "Wallet 저장 여부", example = "Y")
            String walletSavedYn
    ) {
    }

    /**
     * VP 검증 상세의 Verifier 정보입니다.
     */
    @Schema(description = "VP 검증 상세 Verifier 정보")
    public record VerifierInfo(

            /** Verifier ID */
            @Schema(description = "Verifier ID", example = "1")
            Long verifierId,

            /** Verifier 이름 */
            @Schema(description = "Verifier 이름", example = "검증기관")
            String verifierName,

            /** Verifier 상태 코드 */
            @Schema(description = "Verifier 상태 코드", example = "ACTIVE")
            String verifierStatusCode,

            /** 담당자 이메일 */
            @Schema(description = "담당자 이메일", example = "contact@example.com")
            String contactEmail
    ) {
    }
}
