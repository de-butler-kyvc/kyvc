package com.kyvc.backendadmin.domain.vp.repository;

import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationDetailResponse;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationSearchRequest;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationSummaryResponse;

import java.util.List;
import java.util.Optional;

/**
 * VP 검증 목록/상세 조회 QueryRepository입니다.
 */
public interface VpVerificationQueryRepository {

    /**
     * VP 검증 목록을 조회합니다.
     *
     * @param request 검색 조건
     * @return VP 검증 목록
     */
    List<AdminVpVerificationSummaryResponse> search(AdminVpVerificationSearchRequest request);

    /**
     * VP 검증 목록 전체 건수를 조회합니다.
     *
     * @param request 검색 조건
     * @return 전체 건수
     */
    long count(AdminVpVerificationSearchRequest request);

    /**
     * VP 검증 상세를 조회합니다.
     *
     * @param verificationId VP 검증 ID
     * @return VP 검증 상세 Optional
     */
    Optional<AdminVpVerificationDetailResponse> findDetailById(Long verificationId);
}
