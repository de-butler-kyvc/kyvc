package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// Verifier log Repository 구현체
@Repository
@RequiredArgsConstructor
public class VerifierLogRepositoryImpl implements VerifierLogRepository {

    private final VerifierLogJpaRepository verifierLogJpaRepository;

    @Override
    public VerifierLog save(
            VerifierLog verifierLog // 저장 대상 로그
    ) {
        return verifierLogJpaRepository.save(verifierLog);
    }
}
