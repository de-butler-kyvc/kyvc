package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.review.dto.AdminAiReviewDetailResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewAgentAuthorityResponse;
import com.kyvc.backendadmin.domain.review.dto.KycReviewHistoryResponse;

import java.util.List;
import java.util.Optional;

/**
 * Backend Admin AI 심사 결과 조회용 QueryRepository입니다.
 */
public interface AiReviewQueryRepository {

    /**
     * KYC 신청 ID 기준으로 AI 심사 상세 정보를 조회합니다.
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return AI 심사 상세 정보 Optional
     */
    Optional<AdminAiReviewDetailResponse> findAiReviewDetail(Long kycId);

    /**
     * KYC 신청 ID 기준으로 대리인 권한 응답에 필요한 법인 대리인 기본 정보를 조회합니다.
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return 대리인 권한 기본 정보 Optional
     */
    Optional<AiReviewAgentAuthorityResponse.AgentAuthority> findAgentAuthorityBase(Long kycId);

    /**
     * KYC 신청 ID 기준으로 심사 이력을 최신순으로 조회합니다.
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return KYC 심사 이력 목록
     */
    List<KycReviewHistoryResponse> findReviewHistories(Long kycId);
}
