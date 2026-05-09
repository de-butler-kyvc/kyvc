package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;

/**
 * Credential 상태 이력 Repository
 */
public interface CredentialStatusHistoryRepository {

    /**
     * Credential 상태 이력 저장
     *
     * @param history 저장 대상 상태 이력
     * @return 저장된 상태 이력
     */
    CredentialStatusHistory save(
            CredentialStatusHistory history // 저장 대상 상태 이력
    );
}
