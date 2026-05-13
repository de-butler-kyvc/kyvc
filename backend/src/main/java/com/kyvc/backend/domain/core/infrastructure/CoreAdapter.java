package com.kyvc.backend.domain.core.infrastructure;

import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.dto.CoreAiReviewStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialSchemaResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreCredentialVerificationResponse;
import com.kyvc.backend.domain.core.dto.CoreDidDocumentResponse;
import com.kyvc.backend.domain.core.dto.CoreHealthResponse;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeResponse;
import com.kyvc.backend.domain.core.dto.CorePresentationVerifyResponse;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionResponse;

import java.util.Map;

/**
 * Core 연동 어댑터 계약
 */
public interface CoreAdapter {

    /**
     * Core 상태 체크
     *
     * @return Core 상태 체크 응답
     */
    CoreHealthResponse checkHealth();

    /**
     * AI 심사 요청
     *
     * @param request AI 심사 요청
     * @return AI 심사 요청 응답
     */
    CoreAiReviewResponse requestAiReview(
            CoreAiReviewRequest request // AI 심사 요청
    );

    /**
     * AI 심사 상태 조회
     *
     * @param coreRequestId Core 요청 ID
     * @return AI 심사 상태 응답
     */
    CoreAiReviewStatusResponse getAiReviewStatus(
            String coreRequestId // Core 요청 ID
    );

    /**
     * VC 발급 요청
     *
     * @param request VC 발급 요청
     * @return VC 발급 요청 응답
     */
    CoreVcIssuanceResponse requestVcIssuance(
            CoreVcIssuanceRequest request // VC 발급 요청
    );

    /**
     * VC 발급 상태 조회
     *
     * @param coreRequestId Core 요청 ID
     * @return VC 발급 상태 응답
     */
    CoreVcIssuanceStatusResponse getVcIssuanceStatus(
            String coreRequestId // Core 요청 ID
    );

    /**
     * Credential 폐기 요청
     *
     * @param request Credential 폐기 요청
     * @return Credential 폐기 응답
     */
    CoreRevokeCredentialResponse revokeCredential(
            CoreRevokeCredentialRequest request // Credential 폐기 요청
    );

    /**
     * Credential 상태 조회
     *
     * @param issuerAccount Issuer XRPL Account
     * @param holderAccount Holder XRPL Account
     * @param credentialType Credential 유형
     * @return Credential 상태 조회 응답
     */
    CoreCredentialStatusResponse getCredentialStatus(
            String issuerAccount, // Issuer XRPL Account
            String holderAccount, // Holder XRPL Account
            String credentialType // Credential 유형
    );

    /**
     * VP 검증 요청
     *
     * @param request VP 검증 요청
     * @param format Presentation format
     * @param presentation Presentation 원문 또는 객체
     * @param didDocuments DID document 목록
     * @return VP 검증 요청 응답
     */
    CoreVpVerificationResponse requestVpVerification(
            CoreVpVerificationRequest request, // VP 검증 요청
            String format, // Presentation format
            Object presentation, // Presentation 원문 또는 객체
            Map<String, Object> didDocuments // DID document 목록
    );

    /**
     * VP 검증 요청
     *
     * @param request VP 검증 요청
     * @param format Presentation format
     * @param presentation Presentation 원문 또는 객체
     * @return VP 검증 요청 응답
     */
    default CoreVpVerificationResponse requestVpVerification(
            CoreVpVerificationRequest request, // VP 검증 요청
            String format, // Presentation format
            Object presentation // Presentation 원문 또는 객체
    ) {
        return requestVpVerification(request, format, presentation, null);
    }

    /**
     * VP 검증 상태 조회
     *
     * @param coreRequestId Core 요청 ID
     * @return VP 검증 상태 응답
     */
    CoreVpVerificationStatusResponse getVpVerificationStatus(
            String coreRequestId // Core 요청 ID
    );

    /**
     * VP Challenge 발급 요청
     *
     * @param request VP Challenge 발급 요청
     * @return VP Challenge 발급 응답
     */
    CorePresentationChallengeResponse issuePresentationChallenge(
            CorePresentationChallengeRequest request // VP Challenge 발급 요청
    );

    /**
     * 웹 VP 로그인 Presentation 검증 요청
     *
     * @param vp Wallet 생성 VP 객체
     * @return Presentation 검증 응답
     */
    CorePresentationVerifyResponse verifyWebVpLoginPresentation(
            Object vp // Wallet 생성 VP 객체
    );

    /**
     * 웹 VP 로그인 Presentation 검증 요청
     *
     * @param vp Wallet 생성 VP 객체
     * @param didDocuments DID document 목록
     * @return Presentation 검증 응답
     */
    default CorePresentationVerifyResponse verifyWebVpLoginPresentation(
            Object vp, // Wallet 생성 VP 객체
            Map<String, Map<String, Object>> didDocuments // DID document 목록
    ) {
        return verifyWebVpLoginPresentation(vp);
    }

    /**
     * Credential 검증 요청
     *
     * @param request Credential 검증 요청
     * @return Credential 검증 응답
     */
    CoreCredentialVerificationResponse verifyCredential(
            CoreCredentialVerificationRequest request // Credential 검증 요청
    );

    /**
     * DID Document 조회
     *
     * @param account XRPL Account
     * @return DID Document 조회 응답
     */
    CoreDidDocumentResponse getDidDocument(
            String account // XRPL Account
    );

    /**
     * XRPL 트랜잭션 조회
     *
     * @param txHash 트랜잭션 해시
     * @return XRPL 트랜잭션 응답
     */
    CoreXrplTransactionResponse getXrplTransaction(
            String txHash // 트랜잭션 해시
    );

    /**
     * Credential 스키마 조회
     *
     * @param schemaId 스키마 ID
     * @return Credential 스키마 응답
     */
    CoreCredentialSchemaResponse getCredentialSchema(
            String schemaId // 스키마 ID
    );
}
