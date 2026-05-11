package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.kyc.domain.KycApplication;

import java.util.Optional;

/**
 * 금융사 방문 KYC Repository
 */
public interface FinanceKycApplicationRepository {

    /**
     * KYC 신청 ID 기준 조회
     *
     * @param kycId KYC 신청 ID
     * @return KYC 신청 조회 결과
     */
    Optional<KycApplication> findById(
            Long kycId // KYC 신청 ID
    );

    /**
     * KYC 신청 저장
     *
     * @param kycApplication 저장 대상 KYC 신청
     * @return 저장된 KYC 신청
     */
    KycApplication save(
            KycApplication kycApplication // 저장 대상 KYC 신청
    );
}
