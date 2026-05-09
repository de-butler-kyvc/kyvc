package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.Verifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Verifier JPA Repository
 */
public interface VerifierJpaRepository extends JpaRepository<Verifier, Long> {

    /**
     * Verifier ID 기준 조회
     *
     * @param verifierId Verifier ID
     * @return Verifier 조회 결과
     */
    Optional<Verifier> findByVerifierId(
            Long verifierId // Verifier ID
    );

    /**
     * 연락 이메일 기준 최신 Verifier 조회
     *
     * @param contactEmail 연락 이메일
     * @return Verifier 조회 결과
     */
    Optional<Verifier> findFirstByContactEmailOrderByCreatedAtDesc(
            String contactEmail // 연락 이메일
    );
}
