package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Credential 상태 이력 JPA Repository
 */
public interface CredentialStatusHistoryJpaRepository extends JpaRepository<CredentialStatusHistory, Long> {
}
