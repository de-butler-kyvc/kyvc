package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// Credential 상태 이력 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialStatusHistoryRepositoryImpl implements CredentialStatusHistoryRepository {

    private final CredentialStatusHistoryJpaRepository credentialStatusHistoryJpaRepository;

    // Credential 상태 이력 저장
    @Override
    public CredentialStatusHistory save(
            CredentialStatusHistory history // 저장 대상 상태 이력
    ) {
        return credentialStatusHistoryJpaRepository.save(history);
    }
}
