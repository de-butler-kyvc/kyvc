package com.kyvc.backend.domain.kyc.repository;

import com.kyvc.backend.domain.kyc.dto.KycApplicationHistoryResponse;

import java.util.List;

/**
 * 사용자 KYC 신청 이력 조회 Repository
 */
public interface KycApplicationHistoryQueryRepository {

    /**
     * 사용자 소유 법인 KYC 신청 이력 조회
     *
     * @param userId 사용자 ID
     * @param status KYC 상태 코드
     * @param keyword 검색어
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return KYC 신청 이력 목록
     */
    List<KycApplicationHistoryResponse.Item> search(
            Long userId, // 사용자 ID
            String status, // KYC 상태 코드
            String keyword, // 검색어
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * 사용자 소유 법인 KYC 신청 이력 건수 조회
     *
     * @param userId 사용자 ID
     * @param status KYC 상태 코드
     * @param keyword 검색어
     * @return KYC 신청 이력 건수
     */
    long count(
            Long userId, // 사용자 ID
            String status, // KYC 상태 코드
            String keyword // 검색어
    );
}
