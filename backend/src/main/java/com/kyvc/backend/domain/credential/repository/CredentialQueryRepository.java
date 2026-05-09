package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.List;

/**
 * Credential 조회 Repository
 */
public interface CredentialQueryRepository {

    /**
     * 법인 ID 기준 Credential 목록 조회
     *
     * @param corporateId 법인 ID
     * @param credentialStatus Credential 상태 필터
     * @return Credential 목록
     */
    List<Credential> findByCorporateId(
            Long corporateId, // 법인 ID
            KyvcEnums.CredentialStatus credentialStatus // Credential 상태 필터
    );
}
