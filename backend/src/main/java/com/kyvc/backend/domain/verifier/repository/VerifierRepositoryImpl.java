package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.Verifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Verifier Repository 구현체
@Repository
@RequiredArgsConstructor
public class VerifierRepositoryImpl implements VerifierRepository {

    private final VerifierJpaRepository verifierJpaRepository;

    // Verifier ID 기준 조회
    @Override
    public Optional<Verifier> findById(
            Long verifierId // Verifier ID
    ) {
        return verifierJpaRepository.findByVerifierId(verifierId);
    }

    // 연락 이메일 기준 최신 Verifier 조회
    @Override
    public Optional<Verifier> findLatestByContactEmail(
            String contactEmail // 연락 이메일
    ) {
        return verifierJpaRepository.findFirstByContactEmailOrderByCreatedAtDesc(contactEmail);
    }

    // Verifier 저장
    @Override
    public Verifier save(
            Verifier verifier // 저장 대상 Verifier
    ) {
        return verifierJpaRepository.save(verifier);
    }
}
