package com.kyvc.backendadmin.domain.issuer.repository;

import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicyResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicySummaryResponse;

import java.util.List;
import java.util.Optional;

/** Issuer 정책 목록/상세 조회 QueryRepository입니다. */
public interface IssuerPolicyQueryRepository {

    /** Issuer 정책 목록을 조회합니다. */
    List<IssuerPolicyResponse> search(IssuerPolicySummaryResponse.SearchRequest request);

    /** Issuer 정책 목록 전체 건수를 조회합니다. */
    long count(IssuerPolicySummaryResponse.SearchRequest request);

    /** Issuer 정책 상세를 조회합니다. */
    Optional<IssuerPolicyResponse> findDetailById(Long policyId);
}
