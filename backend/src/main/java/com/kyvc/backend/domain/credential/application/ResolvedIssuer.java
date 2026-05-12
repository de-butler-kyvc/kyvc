package com.kyvc.backend.domain.credential.application;

/**
 * Credential 발급 Issuer 해석 결과
 *
 * @param issuerAccount Issuer XRPL Account
 * @param issuerDid Issuer DID
 * @param issuerVerificationMethodId Issuer Verification Method ID
 * @param signingKeyRef 서명키 참조
 * @param credentialType Credential 유형
 */
public record ResolvedIssuer(
        String issuerAccount, // Issuer XRPL Account
        String issuerDid, // Issuer DID
        String issuerVerificationMethodId, // Issuer Verification Method ID
        String signingKeyRef, // 서명키 참조
        String credentialType // Credential 유형
) {
}
