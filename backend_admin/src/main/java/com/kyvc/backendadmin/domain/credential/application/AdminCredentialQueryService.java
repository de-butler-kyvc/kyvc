package com.kyvc.backendadmin.domain.credential.application;

import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialListResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialRequestHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSearchRequest;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialStatusHistoryResponse;
import com.kyvc.backendadmin.domain.credential.repository.CredentialQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backend Admin Credential 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminCredentialQueryService {

    private final CredentialQueryRepository credentialQueryRepository;

    /**
     * Credential 목록을 조회합니다.
     *
     * @param request 검색 조건
     * @return Credential 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminCredentialListResponse search(AdminCredentialSearchRequest request) {
        List<AdminCredentialListResponse.Item> items = credentialQueryRepository.search(request);
        long totalElements = credentialQueryRepository.count(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new AdminCredentialListResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    /**
     * Credential ID 기준으로 상세 정보를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 상세 응답
     */
    @Transactional(readOnly = true)
    public AdminCredentialDetailResponse getDetail(Long credentialId) {
        return credentialQueryRepository.findDetailById(credentialId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND));
    }

    /**
     * Credential 요청 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 요청 이력 목록
     */
    @Transactional(readOnly = true)
    public List<AdminCredentialRequestHistoryResponse> getRequestHistories(Long credentialId) {
        validateCredentialExists(credentialId);
        return credentialQueryRepository.findRequestHistories(credentialId);
    }

    /**
     * Credential 상태 변경 이력을 조회합니다.
     *
     * @param credentialId Credential ID
     * @return Credential 상태 변경 이력 목록
     */
    @Transactional(readOnly = true)
    public List<AdminCredentialStatusHistoryResponse> getStatusHistories(Long credentialId) {
        validateCredentialExists(credentialId);
        return credentialQueryRepository.findStatusHistories(credentialId);
    }

    private void validateCredentialExists(Long credentialId) {
        if (!credentialQueryRepository.existsById(credentialId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND);
        }
    }
}
