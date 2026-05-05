package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Credential JPA Repository
 */
public interface CredentialJpaRepository extends JpaRepository<Credential, Long> {

    /**
     * KYC 요청 ID 기준 최신 Credential 조회
     *
     * @param kycId KYC 요청 ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findFirstByKycIdOrderByCreatedAtDesc(
            Long kycId // KYC 요청 ID
    );

    /**
     * 법인 ID 기준 최신 Credential 조회
     *
     * @param corporateId 법인 ID
     * @return Credential 조회 결과
     */
    Optional<Credential> findFirstByCorporateIdOrderByCreatedAtDesc(
            Long corporateId // 법인 ID
    );

    /**
     * 법인 ID 기준 Credential 목록 조회
     *
     * @param corporateId 법인 ID
     * @return Credential 목록
     */
    List<Credential> findByCorporateIdOrderByCreatedAtDesc(
            Long corporateId // 법인 ID
    );
}
