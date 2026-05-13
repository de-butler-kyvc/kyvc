package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Credential 요청 조회 Repository
 */
public interface CredentialRequestQueryRepository {

    /**
     * 법인 기준 Credential 요청 목록 조회
     *
     * @param corporateId 법인 ID
     * @param requestTypeCode 요청 유형
     * @param requestStatusCode 요청 상태
     * @return Credential 요청 목록
     */
    List<CredentialRequest> findByCorporateId(
            Long corporateId, // 법인 ID
            KyvcEnums.CredentialRequestType requestTypeCode, // 요청 유형
            KyvcEnums.CredentialRequestStatus requestStatusCode // 요청 상태
    );

    /**
     * 법인 기준 Credential 요청 상세 조회
     *
     * @param credentialRequestId Credential 요청 ID
     * @param corporateId 법인 ID
     * @return Credential 요청 상세 조회 결과
     */
    Optional<CredentialRequest> findByCredentialRequestIdAndCorporateId(
            Long credentialRequestId, // Credential 요청 ID
            Long corporateId // 법인 ID
    );

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
