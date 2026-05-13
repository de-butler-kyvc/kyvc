package com.kyvc.backendadmin.domain.credential.repository;

import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialListResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialRequestHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSearchRequest;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialStatusHistoryResponse;

import java.util.List;
import java.util.Optional;

/**
 * Credential 목록/상세/이력 조회 QueryRepository입니다.
 */
public interface CredentialQueryRepository {

    /**
     * Credential 목록을 조회합니다.
     *
     * @param request 검색 조건
     * @return Credential 목록
     */
    List<AdminCredentialListResponse.Item> search(AdminCredentialSearchRequest request);

    /**
     * Credential 목록 전체 건수를 조회합니다.
     *
     * @param request 검색 조건
     * @return 전체 건수
     */
    long count(AdminCredentialSearchRequest request);

    /**
     * Credential ID 기준으로 상세 정보를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 상세 Optional
     */
    Optional<AdminCredentialDetailResponse> findDetailById(Long credentialId);

    /**
     * Credential 요청 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return 요청 이력 목록
     */
    List<AdminCredentialRequestHistoryResponse> findRequestHistories(Long credentialId);

    /**
     * Credential 상태 변경 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return 상태 변경 이력 목록
     */
    List<AdminCredentialStatusHistoryResponse> findStatusHistories(Long credentialId);

    /**
     * Credential 존재 여부를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return 존재하면 true
     */
    boolean existsById(Long credentialId);
}
