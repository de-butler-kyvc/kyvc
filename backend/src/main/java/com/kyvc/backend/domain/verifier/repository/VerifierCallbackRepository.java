package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierCallback;

import java.util.Optional;

/**
 * Verifier callback Repository
 */
public interface VerifierCallbackRepository {

    /**
     * 활성 callback 조회
     *
     * @param verifierId Verifier ID
     * @return Callback 조회 결과
     */
    Optional<VerifierCallback> findActiveByVerifierId(
            Long verifierId // Verifier ID
    );
}
