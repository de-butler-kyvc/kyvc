package com.kyvc.backendadmin.domain.corporate.repository;

import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserDetailResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserListResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateUserSearchRequest;

import java.util.List;
import java.util.Optional;

/**
 * 법인 사용자 목록 및 조인 검색 책임을 가지는 QueryRepository입니다.
 *
 * <p>users, corporates, kyc_applications를 조인하여 목록 검색, 사용자 상세,
 * 법인 상세 조회에 필요한 읽기 모델을 생성합니다.</p>
 */
public interface CorporateQueryRepository {

    /**
     * users, corporates, kyc_applications를 조인하여 법인 사용자 목록을 조회합니다.
     *
     * <p>users는 CORPORATE_USER만 대상으로 하며, corporates는 user_id로 조인하고,
     * kyc_applications는 corporate_id 기준 최신 KYC 1건을 조인합니다.</p>
     *
     * @param request 검색 조건
     * @return 법인 사용자 목록 항목
     */
    List<AdminCorporateUserListResponse.Item> searchUsers(AdminCorporateUserSearchRequest request);

    /**
     * users, corporates, kyc_applications 조인 조건으로 법인 사용자 목록 전체 건수를 조회합니다.
     *
     * <p>목록 조회와 동일하게 users.user_type_code, 사용자 상태, 법인명,
     * 최신 KYC 상태 조건을 적용합니다.</p>
     *
     * @param request 검색 조건
     * @return 전체 건수
     */
    long countUsers(AdminCorporateUserSearchRequest request);

    /**
     * users.user_id 기준으로 users, corporates, kyc_applications를 조인하여 상세를 조회합니다.
     *
     * <p>corporates는 user_id로 조인하고 kyc_applications는 corporate_id 기준 최신 KYC 1건을 조회합니다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 법인 사용자 상세 Optional
     */
    Optional<AdminCorporateUserDetailResponse> findUserDetail(Long userId);

    /**
     * corporates.corporate_id 기준으로 users, corporates, kyc_applications를 조인하여 법인 상세를 조회합니다.
     *
     * <p>users는 corporates.user_id로 조인하고 kyc_applications는 corporate_id 기준 최신 KYC 1건을 조회합니다.</p>
     *
     * @param corporateId 조회할 법인 ID
     * @return 법인 상세 Optional
     */
    Optional<AdminCorporateDetailResponse> findCorporateDetail(Long corporateId);
}
