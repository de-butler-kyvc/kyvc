package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySearchRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySummaryResponse;

import java.util.List;
import java.util.Optional;

/**
 * AI 심사 업무 정책 목록/상세 조회 QueryRepository입니다.
 */
public interface AiReviewPolicyQueryRepository {

    /**
     * AI 심사 업무 정책 목록을 검색합니다.
     *
     * @param request 검색 조건
     * @return AI 심사 업무 정책 목록
     */
    List<AiReviewPolicySummaryResponse.Item> search(AiReviewPolicySearchRequest request);

    /**
     * AI 심사 업무 정책 목록 검색 조건에 맞는 전체 건수를 조회합니다.
     *
     * @param request 검색 조건
     * @return 전체 건수
     */
    long count(AiReviewPolicySearchRequest request);

    /**
     * AI 심사 업무 정책 ID 기준으로 상세 정보를 조회합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @return AI 심사 업무 정책 상세 Optional
     */
    Optional<AiReviewPolicyResponse> findDetailById(Long aiPolicyId);
}
