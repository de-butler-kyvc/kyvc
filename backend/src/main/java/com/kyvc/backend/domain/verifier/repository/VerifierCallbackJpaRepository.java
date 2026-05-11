package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierCallback;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Verifier callback JPA Repository
 */
public interface VerifierCallbackJpaRepository extends JpaRepository<VerifierCallback, Long> {

    /**
     * 활성 callback 조회
     *
     * @param verifierId Verifier ID
     * @param callbackStatusCode Callback 상태
     * @param enabledYn 사용 여부
     * @return Callback 조회 결과
     */
    Optional<VerifierCallback> findFirstByVerifierIdAndCallbackStatusCodeAndEnabledYnOrderByCreatedAtDesc(
            Long verifierId, // Verifier ID
            KyvcEnums.VerifierCallbackStatus callbackStatusCode, // Callback 상태
            String enabledYn // 사용 여부
    );
}
