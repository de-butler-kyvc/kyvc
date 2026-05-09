package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.Optional;

/**
 * Credential 요청 Repository
 */
public interface CredentialRequestRepository {

    /**
     * Credential 요청 저장
     *
     * @param credentialRequest 저장 대상 Credential 요청
     * @return 저장된 Credential 요청
     */
    CredentialRequest save(
            CredentialRequest credentialRequest // 저장 대상 Credential 요청
    );

    /**
     * Credential 요청 ID 기준 조회
     *
     * @param credentialRequestId Credential 요청 ID
     * @return Credential 요청 조회 결과
     */
    Optional<CredentialRequest> findById(
            Long credentialRequestId // Credential 요청 ID
    );

    /**
     * Credential별 진행 중 요청 존재 여부 조회
     *
     * @param credentialId Credential ID
     * @param requestTypeCode 요청 유형
     * @return 진행 중 요청 존재 여부
     */
    boolean existsInProgressByCredentialIdAndType(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestTypeCode // 요청 유형
    );
}
