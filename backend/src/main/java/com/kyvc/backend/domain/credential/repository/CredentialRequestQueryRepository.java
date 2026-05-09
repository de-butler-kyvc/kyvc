package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.Collection;
import java.util.List;

/**
 * Credential 요청 조회 Repository
 */
public interface CredentialRequestQueryRepository {

    /**
     * Credential ID 목록 기준 요청 이력 조회
     *
     * @param credentialIds Credential ID 목록
     * @param requestType 요청 유형 필터
     * @param requestStatus 요청 상태 필터
     * @return Credential 요청 이력 목록
     */
    List<CredentialRequest> findByCredentialIds(
            Collection<Long> credentialIds, // Credential ID 목록
            KyvcEnums.CredentialRequestType requestType, // 요청 유형 필터
            KyvcEnums.CredentialRequestStatus requestStatus // 요청 상태 필터
    );
}
