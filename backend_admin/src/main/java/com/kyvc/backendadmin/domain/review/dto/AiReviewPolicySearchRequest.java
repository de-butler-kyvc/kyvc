package com.kyvc.backendadmin.domain.review.dto;

/**
 * AI 심사 정책 목록 검색 조건 DTO입니다.
 */
public record AiReviewPolicySearchRequest(
        int page,
        int size,
        String keyword,
        String status,
        String corporateType,
        String enabledYn
) {

    /**
     * AI 심사 정책 목록 검색 조건을 생성합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param keyword 검색어
     * @param status 정책 상태
     * @param corporateType 법인 유형
     * @param enabledYn 사용 여부
     * @return 검색 조건
     */
    public static AiReviewPolicySearchRequest of(
            Integer page,
            Integer size,
            String keyword,
            String status,
            String corporateType,
            String enabledYn
    ) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return new AiReviewPolicySearchRequest(
                normalizedPage,
                normalizedSize,
                keyword,
                status,
                corporateType,
                enabledYn
        );
    }
}
