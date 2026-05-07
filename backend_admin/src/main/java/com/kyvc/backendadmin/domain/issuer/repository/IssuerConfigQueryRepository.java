package com.kyvc.backendadmin.domain.issuer.repository;

import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigDetailResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigSummaryResponse;

import java.util.List;
import java.util.Optional;

/** Issuer 발급 설정 목록/상세 조회 QueryRepository입니다. */
public interface IssuerConfigQueryRepository {

    /** Issuer 발급 설정 목록을 조회합니다. */
    List<IssuerConfigSummaryResponse.Item> search(IssuerConfigSummaryResponse.SearchRequest request);

    /** Issuer 발급 설정 목록 전체 건수를 조회합니다. */
    long count(IssuerConfigSummaryResponse.SearchRequest request);

    /** Issuer 발급 설정 상세를 조회합니다. */
    Optional<IssuerConfigDetailResponse> findDetailById(Long issuerConfigId);
}
