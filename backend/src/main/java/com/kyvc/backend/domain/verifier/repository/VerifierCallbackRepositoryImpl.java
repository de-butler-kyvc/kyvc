package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierCallback;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Verifier callback Repository 구현체
@Repository
@RequiredArgsConstructor
public class VerifierCallbackRepositoryImpl implements VerifierCallbackRepository {

    private static final String ENABLED_YN = "Y"; // 사용 여부 Y

    private final VerifierCallbackJpaRepository verifierCallbackJpaRepository;

    @Override
    public Optional<VerifierCallback> findActiveByVerifierId(
            Long verifierId // Verifier ID
    ) {
        return verifierCallbackJpaRepository
                .findFirstByVerifierIdAndCallbackStatusCodeAndEnabledYnOrderByCreatedAtDesc(
                        verifierId,
                        KyvcEnums.VerifierCallbackStatus.ACTIVE,
                        ENABLED_YN
                );
    }
}
