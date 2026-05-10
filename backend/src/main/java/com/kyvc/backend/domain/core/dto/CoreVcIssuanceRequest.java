package com.kyvc.backend.domain.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Core VC 발급 요청
 *
 * @param coreRequestId Core 요청 ID
 * @param credentialId Credential ID
 * @param kycId KYC 신청 ID
 * @param corporateId 법인 ID
 * @param issuerAccount Issuer XRPL Account
 * @param issuerSeed Issuer seed
 * @param issuerPrivateKeyPem Issuer private key PEM
 * @param issuerDid Issuer DID
 * @param issuerVerificationMethodId Issuer Verification Method ID
 * @param keyId Issuer key ID
 * @param holderAccount Holder XRPL Account
 * @param holderDid Holder DID
 * @param credentialType Credential 유형
 * @param kycLevel KYC 레벨 코드
 * @param jurisdiction 관할 코드
 * @param claims VC Claim 데이터
 * @param validFrom VC 유효 시작 시각
 * @param validUntil VC 유효 종료 시각
 * @param persist Core 저장 여부
 * @param persistStatus 상태 저장 여부
 * @param markStatusAccepted 상태 수락 마킹 여부
 * @param storeIssuerDidDocument Issuer DID Document 저장 여부
 * @param statusUri 상태 URI
 * @param xrplJsonRpcUrl XRPL JSON RPC URL
 * @param allowMainnet mainnet 허용 여부
 * @param statusMode 상태 저장 모드
 * @param credentialFormat Credential 형식
 * @param format VC format
 * @param holderKeyId Holder key ID
 * @param vct Verifiable Credential Type
 * @param requestedAt 요청 시각
 */
@Schema(description = "Core VC 발급 요청")
public record CoreVcIssuanceRequest(
        @Schema(description = "Core 요청 ID", example = "fcf03404-f0eb-43f9-b56a-a5f3b8fdd7de")
        String coreRequestId, // Core 요청 ID
        @Schema(description = "Credential ID", example = "11")
        Long credentialId, // Credential ID
        @Schema(description = "KYC 신청 ID", example = "1")
        Long kycId, // KYC 신청 ID
        @Schema(description = "법인 ID", example = "10")
        Long corporateId, // 법인 ID
        @Schema(description = "Issuer XRPL Account", example = "rIssuer")
        @JsonProperty("issuer_account")
        String issuerAccount, // Issuer XRPL Account
        @Schema(description = "Issuer seed")
        @JsonProperty("issuer_seed")
        String issuerSeed, // Issuer seed
        @Schema(description = "Issuer private key PEM")
        @JsonProperty("issuer_private_key_pem")
        String issuerPrivateKeyPem, // Issuer private key PEM
        @Schema(description = "Issuer DID", example = "did:xrpl:1:rIssuer")
        @JsonProperty("issuer_did")
        String issuerDid, // Issuer DID
        @Schema(description = "Issuer Verification Method ID", example = "did:xrpl:1:rIssuer#issuer-key-1")
        String issuerVerificationMethodId, // Issuer Verification Method ID
        @Schema(description = "Issuer key ID", example = "issuer-key-1")
        @JsonProperty("key_id")
        String keyId, // Issuer key ID
        @Schema(description = "Holder XRPL Account", example = "rHolder")
        @JsonProperty("holder_account")
        String holderAccount, // Holder XRPL Account
        @Schema(description = "Holder DID", example = "did:xrpl:1:rHolder")
        @JsonProperty("holder_did")
        String holderDid, // Holder DID
        @Schema(description = "Credential 유형", example = "KYC_CREDENTIAL")
        String credentialType, // Credential 유형
        @Schema(description = "KYC 레벨 코드", example = "BASIC")
        String kycLevel, // KYC 레벨 코드
        @Schema(description = "관할 코드", example = "KR")
        String jurisdiction, // 관할 코드
        @Schema(description = "VC Claim 데이터")
        @JsonProperty("claims")
        Map<String, Object> claims, // VC Claim 데이터
        @Schema(description = "VC 유효 시작 시각", example = "2026-05-07T12:00:00Z")
        @JsonProperty("valid_from")
        OffsetDateTime validFrom, // VC 유효 시작 시각
        @Schema(description = "VC 유효 종료 시각", example = "2027-05-07T12:00:00Z")
        @JsonProperty("valid_until")
        OffsetDateTime validUntil, // VC 유효 종료 시각
        @Schema(description = "Core 저장 여부", example = "true")
        @JsonProperty("persist")
        Boolean persist, // Core 저장 여부
        @Schema(description = "상태 저장 여부", example = "true")
        @JsonProperty("persist_status")
        Boolean persistStatus, // 상태 저장 여부
        @Schema(description = "상태 수락 마킹 여부", example = "false")
        @JsonProperty("mark_status_accepted")
        Boolean markStatusAccepted, // 상태 수락 마킹 여부
        @Schema(description = "Issuer DID Document 저장 여부", example = "true")
        @JsonProperty("store_issuer_did_document")
        Boolean storeIssuerDidDocument, // Issuer DID Document 저장 여부
        @Schema(description = "상태 URI")
        @JsonProperty("status_uri")
        String statusUri, // 상태 URI
        @Schema(description = "XRPL JSON RPC URL")
        @JsonProperty("xrpl_json_rpc_url")
        String xrplJsonRpcUrl, // XRPL JSON RPC URL
        @Schema(description = "mainnet 허용 여부", example = "false")
        @JsonProperty("allow_mainnet")
        Boolean allowMainnet, // mainnet 허용 여부
        @Schema(description = "상태 저장 모드", example = "xrpl")
        @JsonProperty("status_mode")
        String statusMode, // 상태 저장 모드
        @Schema(description = "Credential 형식", example = "jwt")
        @JsonProperty("credential_format")
        String credentialFormat, // Credential 형식
        @Schema(description = "VC format", example = "vc+jwt")
        @JsonProperty("format")
        String format, // VC format
        @Schema(description = "Holder key ID")
        @JsonProperty("holder_key_id")
        String holderKeyId, // Holder key ID
        @Schema(description = "Verifiable Credential Type", example = "KYC_CREDENTIAL")
        @JsonProperty("vct")
        String vct, // Verifiable Credential Type
        @Schema(description = "요청 시각", example = "2026-05-06T10:10:00Z")
        OffsetDateTime requestedAt // 요청 시각
) {
}
