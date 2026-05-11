package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierApiKey;

import java.util.Optional;

/**
 * Verifier API Key Repository
 */
public interface VerifierApiKeyRepository {

    /**
     * API Key hash 기준 조회
     *
     * @param apiKeyHash API Key hash
     * @return API Key 조회 결과
     */
    Optional<VerifierApiKey> findByApiKeyHash(
            String apiKeyHash // API Key hash
    );

    /**
     * API Key 저장
     *
     * @param verifierApiKey 저장 대상 API Key
     * @return 저장된 API Key
     */
    VerifierApiKey save(
            VerifierApiKey verifierApiKey // 저장 대상 API Key
    );
}
