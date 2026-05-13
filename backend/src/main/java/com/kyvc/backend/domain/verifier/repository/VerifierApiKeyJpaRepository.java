package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Verifier API Key JPA Repository
 */
public interface VerifierApiKeyJpaRepository extends JpaRepository<VerifierApiKey, Long> {

    /**
     * API Key hash 기준 조회
     *
     * @param apiKeyHash API Key hash
     * @return API Key 조회 결과
     */
    Optional<VerifierApiKey> findByApiKeyHash(
            String apiKeyHash // API Key hash
    );
}
