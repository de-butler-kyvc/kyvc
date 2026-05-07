package com.kyvc.backendadmin.domain.review.application;

import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySearchRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySummaryResponse;
import com.kyvc.backendadmin.domain.review.repository.AiReviewPolicyQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * AI 심사 업무 정책 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AiReviewPolicyQueryService {

    private final AiReviewPolicyQueryRepository aiReviewPolicyQueryRepository;

    /**
     * AI 심사 업무 정책 목록을 조회합니다.
     *
     * @param request 검색 조건
     * @return AI 심사 업무 정책 목록 응답
     */
    @Transactional(readOnly = true)
    public AiReviewPolicySummaryResponse search(AiReviewPolicySearchRequest request) {
        validateSearchRequest(request);
        List<AiReviewPolicySummaryResponse.Item> items = aiReviewPolicyQueryRepository.search(request);
        long totalElements = aiReviewPolicyQueryRepository.count(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new AiReviewPolicySummaryResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    /**
     * AI 심사 업무 정책 상세 정보를 조회합니다.
     *
     * @param aiPolicyId AI 심사 정책 ID
     * @return AI 심사 업무 정책 상세 응답
     */
    @Transactional(readOnly = true)
    public AiReviewPolicyResponse getDetail(Long aiPolicyId) {
        return aiReviewPolicyQueryRepository.findDetailById(aiPolicyId)
                .orElseThrow(() -> new ApiException(ErrorCode.AI_REVIEW_POLICY_NOT_FOUND));
    }

    private void validateSearchRequest(AiReviewPolicySearchRequest request) {
        if (StringUtils.hasText(request.enabledYn()) && !"Y".equals(request.enabledYn()) && !"N".equals(request.enabledYn())) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "enabledYn은 Y 또는 N이어야 합니다.");
        }
        if (StringUtils.hasText(request.status()) && !"ACTIVE".equals(request.status()) && !"INACTIVE".equals(request.status())) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "status는 ACTIVE 또는 INACTIVE이어야 합니다.");
        }
    }
}
