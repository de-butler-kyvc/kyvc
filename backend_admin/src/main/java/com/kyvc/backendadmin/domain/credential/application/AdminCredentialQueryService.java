package com.kyvc.backendadmin.domain.credential.application;

import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialRequestResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialStatusHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSummaryResponse;
import com.kyvc.backendadmin.domain.credential.repository.CredentialQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backend Admin VC 발급 상태 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminCredentialQueryService {

    private final CredentialQueryRepository credentialQueryRepository;

    /**
     * VC 발급 상태 목록을 조회합니다.
     *
     * @param request 검색 조건
     * @return VC 발급 상태 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminCredentialSummaryResponse search(AdminCredentialSummaryResponse.SearchRequest request) {
        List<AdminCredentialSummaryResponse.Item> items = credentialQueryRepository.search(request);
        long totalElements = credentialQueryRepository.count(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new AdminCredentialSummaryResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    /**
     * Credential ID 기준으로 VC 발급 상세 정보를 조회합니다.
     *
     * @param credentialId Credential ID
     * @return VC 발급 상세 응답
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
    public List<AdminCredentialRequestResponse> getRequests(Long credentialId) {
        validateCredentialExists(credentialId);
        return credentialQueryRepository.findRequestsByCredentialId(credentialId);
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
        return credentialQueryRepository.findStatusHistoriesByCredentialId(credentialId);
    }

    private void validateCredentialExists(Long credentialId) {
        // 이력 데이터가 없어도 빈 배열을 반환해야 하므로, credentialId 자체의 존재 여부만 먼저 구분한다.
        if (!credentialQueryRepository.existsById(credentialId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND);
        }
    }
}
