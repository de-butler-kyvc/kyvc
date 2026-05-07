package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.review.domain.AiReviewPolicy;

import java.util.Optional;

/**
 * AI 심사 업무 정책 저장 및 단건 조회 Repository입니다.
 */
public interface AiReviewPolicyRepository {

    /**
     * AI 심사 업무 정책을 저장합니다.
     *
     * @param policy 저장할 AI 심사 업무 정책
     * @return 저장된 AI 심사 업무 정책
     */
    AiReviewPolicy save(AiReviewPolicy policy);

    /**
     * AI 심사 업무 정책 ID로 정책을 조회합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @return AI 심사 업무 정책 Optional
     */
    Optional<AiReviewPolicy> findById(Long aiPolicyId);
}
