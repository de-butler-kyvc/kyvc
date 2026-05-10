package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Credential 요청 JPA Repository
 */
public interface CredentialRequestJpaRepository extends JpaRepository<CredentialRequest, Long> {

    /**
     * Credential 요청 ID 기준 조회
     *
     * @param credentialRequestId Credential 요청 ID
     * @return Credential 요청 조회 결과
     */
    Optional<CredentialRequest> findByCredentialRequestId(
            Long credentialRequestId // Credential 요청 ID
    );

    /**
     * Credential ID 목록 기준 요청 이력 조회
     *
     * @param credentialIds Credential ID 목록
     * @return Credential 요청 이력 목록
     */
    List<CredentialRequest> findByCredentialIdInOrderByRequestedAtDesc(
            Collection<Long> credentialIds // Credential ID 목록
    );

    /**
     * Credential ID와 요청 유형 기준 진행 중 요청 존재 여부
     *
     * @param credentialId Credential ID
     * @param requestType 요청 유형
     * @param requestStatuses 요청 상태 목록
     * @return 진행 중 요청 존재 여부
     */
    boolean existsByCredentialIdAndRequestTypeAndRequestStatusIn(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestType, // 요청 유형
            Collection<KyvcEnums.CredentialRequestStatus> requestStatuses // 요청 상태 목록
    );
}
