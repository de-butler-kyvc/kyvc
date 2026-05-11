package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.finance.dto.FinanceKycDocumentListResponse;

import java.util.List;

/**
 * 금융사 방문 KYC 문서 조회 Repository
 */
public interface FinanceKycDocumentQueryRepository {

    /**
     * KYC 신청 ID 기준 문서 목록 조회
     *
     * @param kycId KYC 신청 ID
     * @return 문서 목록
     */
    List<FinanceKycDocumentListResponse.Item> findDocuments(
            Long kycId // KYC 신청 ID
    );
}
