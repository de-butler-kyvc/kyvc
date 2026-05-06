package com.kyvc.backend.domain.review.repository;

import com.kyvc.backend.domain.review.domain.KycSupplement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 보완요청 JPA Repository
 */
public interface SupplementJpaRepository extends JpaRepository<KycSupplement, Long> {

    /**
     * KYC 요청 ID 기준 보완요청 목록 조회
     *
     * @param kycId KYC 요청 ID
     * @return 보완요청 목록
     */
    List<KycSupplement> findByKycIdOrderByRequestedAtDesc(
            Long kycId // KYC 요청 ID
    );

    /**
     * 보완요청 ID와 KYC 요청 ID 기준 조회
     *
     * @param supplementId 보완요청 ID
     * @param kycId KYC 요청 ID
     * @return 보완요청 조회 결과
     */
    Optional<KycSupplement> findBySupplementIdAndKycId(
            Long supplementId, // 보완요청 ID
            Long kycId // KYC 요청 ID
    );
}
