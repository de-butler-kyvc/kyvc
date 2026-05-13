package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;

/**
 * Verifier log Repository
 */
public interface VerifierLogRepository {

    /**
     * Verifier log 저장
     *
     * @param verifierLog 저장 대상 로그
     * @return 저장된 로그
     */
    VerifierLog save(
            VerifierLog verifierLog // 저장 대상 로그
    );
}
