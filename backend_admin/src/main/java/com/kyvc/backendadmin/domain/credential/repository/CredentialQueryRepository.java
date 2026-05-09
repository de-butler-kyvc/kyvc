package com.kyvc.backendadmin.domain.credential.repository;

import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialRequestResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialStatusHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSummaryResponse;

import java.util.List;
import java.util.Optional;

/**
 * Credential 목록, 상세, 이력 조회 QueryRepository입니다.
 */
public interface CredentialQueryRepository {

    /**
     * VC 발급 상태 목록을 조회합니다.
     *
     * @param request 검색 조건
     * @return VC 발급 상태 목록
     */
    List<AdminCredentialSummaryResponse.Item> search(AdminCredentialSummaryResponse.SearchRequest request);

    /**
     * VC 발급 상태 목록 전체 건수를 조회합니다.
     *
     * @param request 검색 조건
     * @return 전체 건수
     */
    long count(AdminCredentialSummaryResponse.SearchRequest request);

    /**
     * Credential ID 기준으로 상세 정보를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return VC 발급 상세 Optional
     */
    Optional<AdminCredentialDetailResponse> findDetailById(Long credentialId);

    /**
     * Credential 존재 여부를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return 존재하면 true
     */
    boolean existsById(Long credentialId);

    /**
     * Credential 요청 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 요청 이력 목록
     */
    List<AdminCredentialRequestResponse> findRequestsByCredentialId(Long credentialId);

    /**
     * Credential 상태 변경 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 상태 변경 이력 목록
     */
    List<AdminCredentialStatusHistoryResponse> findStatusHistoriesByCredentialId(Long credentialId);
}
