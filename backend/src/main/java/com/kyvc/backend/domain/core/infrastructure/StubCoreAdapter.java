package com.kyvc.backend.domain.core.infrastructure;

import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.dto.CoreAiReviewStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialSchemaResponse;
import com.kyvc.backend.domain.core.dto.CoreHealthResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionResponse;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// Core Stub 어댑터
@Component
public class StubCoreAdapter implements CoreAdapter {

    private static final String STUB_MODE = "STUB";

    // Core 헬스 체크
    @Override
    public CoreHealthResponse checkHealth() {
        return new CoreHealthResponse(
                STUB_MODE,
                true,
                "Core is not implemented. StubCoreAdapter is active."
        );
    }

    // AI 심사 요청
    @Override
    public CoreAiReviewResponse requestAiReview(
            CoreAiReviewRequest request // AI 심사 요청
    ) {
        return new CoreAiReviewResponse(
                request.coreRequestId(),
                KyvcEnums.AiReviewStatus.QUEUED.name(),
                "AI review request accepted by stub core.",
                LocalDateTime.now()
        );
    }

    // AI 심사 상태 조회
    @Override
    public CoreAiReviewStatusResponse getAiReviewStatus(
            String coreRequestId // Core 요청 ID
    ) {
        return new CoreAiReviewStatusResponse(
                coreRequestId,
                KyvcEnums.AiReviewStatus.QUEUED.name(),
                "AI review is queued in stub core.",
                LocalDateTime.now()
        );
    }

    // VC 발급 요청
    @Override
    public CoreVcIssuanceResponse requestVcIssuance(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        return new CoreVcIssuanceResponse(
                request.coreRequestId(),
                KyvcEnums.CredentialStatus.ISSUING.name(),
                "VC issuance request accepted by stub core.",
                LocalDateTime.now()
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
                "VC issuance is in progress in stub core.",
                LocalDateTime.now()
        );
    }

    // VP 검증 요청
    @Override
    public CoreVpVerificationResponse requestVpVerification(
            CoreVpVerificationRequest request // VP 검증 요청
    ) {
        return new CoreVpVerificationResponse(
                request.coreRequestId(),
                KyvcEnums.VpVerificationStatus.REQUESTED.name(),
                "VP verification request accepted by stub core.",
                LocalDateTime.now()
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
                "VP verification is requested in stub core.",
                LocalDateTime.now()
        );
    }

    // XRPL 트랜잭션 조회
    @Override
    public CoreXrplTransactionResponse getXrplTransaction(
            String txHash // 트랜잭션 해시
    ) {
        return new CoreXrplTransactionResponse(
                txHash,
                "PENDING",
                "XRPL transaction is pending in stub core.",
                LocalDateTime.now()
        );
    }

    // Credential 스키마 조회
    @Override
    public CoreCredentialSchemaResponse getCredentialSchema(
            String schemaId // 스키마 ID
    ) {
        return new CoreCredentialSchemaResponse(
                schemaId,
                "Stub KYVC Credential Schema",
                "1.0",
                true,
                "Credential schema metadata returned by stub core."
        );
    }
}
