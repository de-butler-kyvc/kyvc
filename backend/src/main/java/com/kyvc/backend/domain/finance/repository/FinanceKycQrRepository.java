package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.credential.domain.Credential;

import java.util.Optional;

/**
 * 금융사 방문 KYC QR Repository
 */
public interface FinanceKycQrRepository {

    /**
     * KYC 신청 ID 기준 최신 Credential 조회
     *
     * @param kycId KYC 신청 ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findLatestByKycId(
            Long kycId // KYC 신청 ID
    );

    /**
     * Credential 저장
     *
     * @param credential 저장 대상 Credential
     * @return 저장된 Credential
     */
    Credential save(
            Credential credential // 저장 대상 Credential
    );
}
