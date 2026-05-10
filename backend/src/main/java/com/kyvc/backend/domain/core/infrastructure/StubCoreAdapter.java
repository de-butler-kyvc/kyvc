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
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionResponse;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Core Mock Adapter
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${kyvc.core.mode:http}'.toLowerCase() == 'mock'")
public class StubCoreAdapter implements CoreAdapter {

    private static final String MOCK_MODE = "MOCK";

    private final CoreMockResponseFactory coreMockResponseFactory;

    // Core 상태 체크
    @Override
    public CoreHealthResponse checkHealth() {
        return new CoreHealthResponse(
                MOCK_MODE,
                true,
                "Core mock adapter is active."
        );
    }

    // AI 심사 요청
    @Override
    public CoreAiReviewResponse requestAiReview(
            CoreAiReviewRequest request // AI 심사 요청
    ) {
        return coreMockResponseFactory.mockAiReviewAccepted(request);
    }

    // AI 심사 상태 조회
    @Override
    public CoreAiReviewStatusResponse getAiReviewStatus(
            String coreRequestId // Core 요청 ID
    ) {
        return coreMockResponseFactory.mockAiReviewStatus(coreRequestId);
    }

    // VC 발급 요청
    @Override
    public CoreVcIssuanceResponse requestVcIssuance(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        return new CoreVcIssuanceResponse(
                request.coreRequestId(),
                KyvcEnums.CredentialStatus.VALID.name(),
                "VC issuance completed by mock core.",
                LocalDateTime.now(),
                "mock-" + request.coreRequestId(),
                request.issuerDid() == null ? CoreMockSeedData.DEV_ISSUER_DID : request.issuerDid(),
                "vc+jwt",
                null,
                "dev.vc.jwt." + request.credentialId(),
                "mock-vc-hash-" + request.coreRequestId(),
                "mock-tx-" + request.coreRequestId(),
                "mock-status-" + request.coreRequestId(),
                LocalDateTime.now(),
                request.validUntil()
        );
    }

    // VC 발급 상태 조회
    @Override
    public CoreVcIssuanceStatusResponse getVcIssuanceStatus(
            String coreRequestId // Core 요청 ID
    ) {
        return new CoreVcIssuanceStatusResponse(
                coreRequestId,
                KyvcEnums.CredentialStatus.ISSUING.name(),
                "VC issuance is in progress in mock core.",
                LocalDateTime.now()
        );
    }

    // Credential 폐기 요청
    @Override
    public CoreRevokeCredentialResponse revokeCredential(
            CoreRevokeCredentialRequest request // Credential 폐기 요청
    ) {
        return new CoreRevokeCredentialResponse(
                true,
                "local",
                "Credential revoked by mock core."
        );
    }

    // Credential 상태 조회
    @Override
    public CoreCredentialStatusResponse getCredentialStatus(
            String issuerAccount, // Issuer XRPL Account
            String holderAccount, // Holder XRPL Account
            String credentialType // Credential 유형
    ) {
        return new CoreCredentialStatusResponse(
                issuerAccount,
                holderAccount,
                credentialType,
                true,
                true,
                KyvcEnums.CredentialStatus.VALID.name(),
                LocalDateTime.now(),
                "Credential status returned by mock core."
        );
    }

    // VP 검증 요청
    @Override
    public CoreVpVerificationResponse requestVpVerification(
            CoreVpVerificationRequest request, // VP 검증 요청
            String format, // Presentation format
            Object presentation // Presentation 원문 또는 객체
    ) {
        return new CoreVpVerificationResponse(
                request.coreRequestId(),
                KyvcEnums.VpVerificationStatus.VALID.name(),
                "VP verification handled by mock core.",
                LocalDateTime.now(),
                true,
                true,
                false,
                "VP 검증 성공"
        );
    }

    // VP 검증 상태 조회
    @Override
    public CoreVpVerificationStatusResponse getVpVerificationStatus(
            String coreRequestId // Core 요청 ID
    ) {
        return new CoreVpVerificationStatusResponse(
                coreRequestId,
                KyvcEnums.VpVerificationStatus.REQUESTED.name(),
                "VP verification is requested in mock core.",
                LocalDateTime.now()
        );
    }

    // VP Challenge 발급
    @Override
    public CorePresentationChallengeResponse issuePresentationChallenge(
            CorePresentationChallengeRequest request // VP Challenge 발급 요청
    ) {
        return new CorePresentationChallengeResponse(
                CoreMockSeedData.DEV_VP_CHALLENGE,
                CoreMockSeedData.DEV_VP_NONCE,
                request.domain(),
                request.aud(),
                CoreMockSeedData.vpExpiresAt(),
                request.presentationDefinition()
        );
    }

    // Credential 검증
    @Override
    public CoreCredentialVerificationResponse verifyCredential(
            CoreCredentialVerificationRequest request // Credential 검증 요청
    ) {
        return new CoreCredentialVerificationResponse(
                true,
                List.of(),
                Map.of("message", "Credential verified by mock core.")
        );
    }

    // DID Document 조회
    @Override
    public CoreDidDocumentResponse getDidDocument(
            String account // XRPL Account
    ) {
        return new CoreDidDocumentResponse(
                account,
                Map.of(
                        "id", "did:xrpl:1:" + account,
                        "verificationMethod", List.of(
                                Map.of(
                                        "id", "did:xrpl:1:" + account + "#issuer-key-1",
                                        "type", "JsonWebKey2020"
                                )
                        )
                )
        );
    }

    // XRPL 트랜잭션 조회
    @Override
    public CoreXrplTransactionResponse getXrplTransaction(
            String txHash // 트랜잭션 해시
    ) {
        return coreMockResponseFactory.mockXrplTransactionStatus(txHash);
    }

    // Credential 스키마 조회
    @Override
    public CoreCredentialSchemaResponse getCredentialSchema(
            String schemaId // 스키마 ID
    ) {
        return coreMockResponseFactory.mockCredentialSchema(schemaId);
    }
}
