package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Verifier log JPA Repository
 */
public interface VerifierLogJpaRepository extends JpaRepository<VerifierLog, Long> {
}
