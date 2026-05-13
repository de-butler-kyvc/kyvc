package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Verifier API Key Repository 구현체
@Repository
@RequiredArgsConstructor
public class VerifierApiKeyRepositoryImpl implements VerifierApiKeyRepository {

    private final VerifierApiKeyJpaRepository verifierApiKeyJpaRepository;

    @Override
    public Optional<VerifierApiKey> findByApiKeyHash(
            String apiKeyHash // API Key hash
    ) {
        return verifierApiKeyJpaRepository.findByApiKeyHash(apiKeyHash);
    }

    @Override
    public VerifierApiKey save(
            VerifierApiKey verifierApiKey // 저장 대상 API Key
    ) {
        return verifierApiKeyJpaRepository.save(verifierApiKey);
    }
}
