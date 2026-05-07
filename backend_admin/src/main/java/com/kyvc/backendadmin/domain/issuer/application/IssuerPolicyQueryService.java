package com.kyvc.backendadmin.domain.issuer.application;

import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicyResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicySummaryResponse;
import com.kyvc.backendadmin.domain.issuer.repository.IssuerPolicyQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Issuer 정책 조회 유스케이스를 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class IssuerPolicyQueryService {

    private final IssuerPolicyQueryRepository issuerPolicyQueryRepository;

    /** Issuer 정책 목록을 조회합니다. */
    @Transactional(readOnly = true)
    public IssuerPolicySummaryResponse search(IssuerPolicySummaryResponse.SearchRequest request) {
        List<IssuerPolicyResponse> items = issuerPolicyQueryRepository.search(request);
        long totalElements = issuerPolicyQueryRepository.count(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new IssuerPolicySummaryResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    /** Issuer 정책 상세를 조회합니다. */
    @Transactional(readOnly = true)
    public IssuerPolicyResponse getDetail(Long policyId) {
        return issuerPolicyQueryRepository.findDetailById(policyId)
                .orElseThrow(() -> new ApiException(ErrorCode.ISSUER_POLICY_NOT_FOUND));
    }
}
