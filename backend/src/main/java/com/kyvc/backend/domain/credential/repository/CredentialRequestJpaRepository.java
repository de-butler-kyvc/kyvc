package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Credential 요청 JPA Repository
 */
public interface CredentialRequestJpaRepository extends JpaRepository<CredentialRequest, Long> {

    /**
     * Credential별 진행 중 요청 존재 여부 조회
     *
     * @param credentialId Credential ID
     * @param requestTypeCode 요청 유형
     * @param requestStatusCodes 요청 상태 목록
     * @return 진행 중 요청 존재 여부
     */
    boolean existsByCredentialIdAndRequestTypeCodeAndRequestStatusCodeIn(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestTypeCode, // 요청 유형
            Collection<KyvcEnums.CredentialRequestStatus> requestStatusCodes // 요청 상태 목록
    );
}
