package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;

import java.util.List;

/**
 * Credential 상태 이력 조회 Repository
 */
public interface CredentialStatusHistoryQueryRepository {

    /**
     * Credential 소유 법인 기준 상태 이력 조회
     *
     * @param credentialId Credential ID
     * @param corporateId 법인 ID
     * @return 상태 이력 목록
     */
    List<CredentialStatusHistory> findByCredentialIdAndCorporateId(
            Long credentialId, // Credential ID
            Long corporateId // 법인 ID
    );
}
