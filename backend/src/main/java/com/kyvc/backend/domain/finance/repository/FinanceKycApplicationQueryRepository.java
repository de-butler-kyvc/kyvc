package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationDetailResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationListResponse;

import java.util.List;
import java.util.Optional;

/**
 * 금융사 방문 KYC 조회 Repository
 */
public interface FinanceKycApplicationQueryRepository {

    /**
     * 금융사 방문 KYC 목록 조회
     *
     * @param staffUserId 금융사 직원 사용자 ID
     * @param status KYC 상태 코드
     * @param keyword 검색어
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return KYC 목록
     */
    List<FinanceKycApplicationListResponse.Item> searchApplications(
            Long staffUserId, // 금융사 직원 사용자 ID
            String status, // KYC 상태 코드
            String keyword, // 검색어
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * 금융사 방문 KYC 목록 건수 조회
     *
     * @param staffUserId 금융사 직원 사용자 ID
     * @param status KYC 상태 코드
     * @param keyword 검색어
     * @return 전체 건수
     */
    long countApplications(
            Long staffUserId, // 금융사 직원 사용자 ID
            String status, // KYC 상태 코드
            String keyword // 검색어
    );

    /**
     * 금융사 방문 KYC 상세 조회
     *
     * @param staffUserId 금융사 직원 사용자 ID
     * @param kycId KYC 신청 ID
     * @return KYC 상세 조회 결과
     */
    Optional<FinanceKycApplicationDetailResponse> findDetail(
            Long staffUserId, // 금융사 직원 사용자 ID
            Long kycId // KYC 신청 ID
    );
}
