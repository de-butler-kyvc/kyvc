package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.Verifier;

import java.util.Optional;

/**
 * Verifier Repository
 */
public interface VerifierRepository {

    /**
     * Verifier ID 기준 조회
     *
     * @param verifierId Verifier ID
     * @return Verifier 조회 결과
     */
    Optional<Verifier> findById(
            Long verifierId // Verifier ID
    );

    /**
     * 연락 이메일 기준 최신 Verifier 조회
     *
     * @param contactEmail 연락 이메일
     * @return Verifier 조회 결과
     */
    Optional<Verifier> findLatestByContactEmail(
            String contactEmail // 연락 이메일
    );

    /**
     * Verifier 저장
     *
     * @param verifier 저장 대상 Verifier
     * @return 저장된 Verifier
     */
    Verifier save(
            Verifier verifier // 저장 대상 Verifier
    );
}
