package com.kyvc.backend.domain.core.infrastructure;

import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.dto.CoreAiReviewStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialSchemaResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionResponse;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Core 미구현 API Mock 응답 팩토리
@Component
public class CoreMockResponseFactory {

    // AI 심사 요청 Mock 응답
    public CoreAiReviewResponse mockAiReviewAccepted(
            CoreAiReviewRequest request // AI 심사 요청
    ) {
        return new CoreAiReviewResponse(
                request.coreRequestId(),
                KyvcEnums.AiReviewStatus.LOW_CONFIDENCE.name(),
                "MANUAL_REVIEW_REQUIRED",
                "mock-" + request.coreRequestId(),
                BigDecimal.ZERO,
                "Core AI API가 없어 개발용 Mock AI 심사 요청을 처리했습니다.",
                LocalDateTime.now()
        );
    }

    // AI 심사 상태 Mock 응답
    public CoreAiReviewStatusResponse mockAiReviewStatus(
            String coreRequestId // Core 요청 ID
    ) {
        return new CoreAiReviewStatusResponse(
                coreRequestId,
                KyvcEnums.AiReviewStatus.LOW_CONFIDENCE.name(),
                "Core AI API가 없어 개발용 Mock AI 심사 상태를 반환했습니다.",
                LocalDateTime.now()
        );
    }

    // AI 심사 결과 Mock 응답
    public CoreAiReviewStatusResponse mockAiReviewResult(
            String coreRequestId // Core 요청 ID
    ) {
        return new CoreAiReviewStatusResponse(
                coreRequestId,
                KyvcEnums.AiReviewStatus.LOW_CONFIDENCE.name(),
                "decision=MANUAL_REVIEW, overallScore=86, confidence=0.86",
                LocalDateTime.now()
        );
    }

    // Credential Schema Mock 응답
    public CoreCredentialSchemaResponse mockCredentialSchema(
            String schemaId // Schema ID
    ) {
        return new CoreCredentialSchemaResponse(
                schemaId,
                "KYVC KYC Credential Schema",
                "dev-mock-1.0",
                true,
                "Core Credential Schema API가 없어 개발용 Mock Schema를 반환했습니다."
        );
    }

    // XRPL 트랜잭션 상태 Mock 응답
    public CoreXrplTransactionResponse mockXrplTransactionStatus(
            String txHash // 트랜잭션 해시
    ) {
        return new CoreXrplTransactionResponse(
                txHash,
                KyvcEnums.XrplTransactionStatus.PENDING.name(),
                "Core XRPL 트랜잭션 API가 없어 개발용 Mock 상태를 반환했습니다.",
                LocalDateTime.now()
        );
    }

    // VC 발급 장애 fallback 응답
    public CoreVcIssuanceResponse fallbackIssueKycCredentialOnFailure(
            CoreVcIssuanceRequest request, // VC 발급 요청
            String reason // fallback 사유
    ) {
        return new CoreVcIssuanceResponse(
                request.coreRequestId(),
                KyvcEnums.CredentialStatus.FAILED.name(),
                "Core 장애 fallback VC 발급 응답: " + reason,
                LocalDateTime.now(),
                "fallback-" + request.coreRequestId(),
                request.credentialType() == null ? CoreMockSeedData.DEV_CREDENTIAL_TYPE : request.credentialType(),
                request.issuerDid() == null ? CoreMockSeedData.DEV_ISSUER_DID : request.issuerDid(),
                "vc+jwt",
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                request.validUntil() == null ? null : request.validUntil().toLocalDateTime()
        );
    }

    // VP 검증 장애 fallback 응답
    public CoreVpVerificationResponse fallbackVerifyPresentationOnFailure(
            CoreVpVerificationRequest request, // VP 검증 요청
            String reason // fallback 사유
    ) {
        return new CoreVpVerificationResponse(
                request.coreRequestId(),
                KyvcEnums.VpVerificationStatus.INVALID.name(),
                "Core 장애 fallback VP 검증 응답: " + reason,
                LocalDateTime.now(),
                true,
                false,
                false,
                "VP 검증 실패"
        );
    }
}
