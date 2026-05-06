package com.kyvc.backend.domain.review.repository;

import com.kyvc.backend.domain.review.domain.KycSupplement;

import java.util.List;
import java.util.Optional;

/**
 * 보완요청 Repository
 */
public interface SupplementRepository {

    /**
     * 보완요청 ID 기준 조회
     *
     * @param supplementId 보완요청 ID
     * @return 보완요청 조회 결과
     */
    Optional<KycSupplement> findById(
            Long supplementId // 보완요청 ID
    );

    /**
     * 보완요청 ID와 KYC 요청 ID 기준 조회
     *
     * @param supplementId 보완요청 ID
     * @param kycId KYC 요청 ID
     * @return 보완요청 조회 결과
     */
    Optional<KycSupplement> findByIdAndKycId(
            Long supplementId, // 보완요청 ID
            Long kycId // KYC 요청 ID
    );

    /**
     * KYC 요청 ID 기준 보완요청 목록 조회
     *
     * @param kycId KYC 요청 ID
     * @return 보완요청 목록
     */
    List<KycSupplement> findByKycId(
            Long kycId // KYC 요청 ID
    );

    /**
     * 보완요청 저장
     *
     * @param supplement 저장 대상 보완요청
     * @return 저장된 보완요청
     */
    KycSupplement save(
            KycSupplement supplement // 저장 대상 보완요청
    );
}
