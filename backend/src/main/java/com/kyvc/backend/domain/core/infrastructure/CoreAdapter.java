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

/**
 * Core 연동 어댑터 계약
 */
public interface CoreAdapter {

    /**
     * Core 헬스 체크
     *
     * @return Core 헬스 체크 응답
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
     * VP 검증 요청
     *
     * @param request VP 검증 요청
     * @return VP 검증 요청 응답
     */
    CoreVpVerificationResponse requestVpVerification(
            CoreVpVerificationRequest request // VP 검증 요청
    );

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
