package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationCorporateResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationDetailResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationListResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationSearchRequest;

import java.util.List;
import java.util.Optional;

/**
 * KYC 신청 목록/검색 책임을 가지는 QueryRepository입니다.
 *
 * <p>kyc_applications, corporates, users, kyc_supplements를 조인하여
 * 목록 검색과 KYC 신청 법인정보 조회에 필요한 읽기 모델을 생성합니다.</p>
 */
public interface KycApplicationQueryRepository {

    /**
     * kyc_applications, corporates, users, kyc_supplements를 조인하여 KYC 신청 목록을 조회합니다.
     *
     * <p>kyc_applications는 기준 테이블이고 corporates는 corporate_id로,
     * users는 applicant_user_id로, kyc_supplements는 kyc_id 기준 최신 보완요청 1건으로 조인합니다.</p>
     *
     * @param request 검색 조건
     * @return KYC 신청 목록 항목
     */
    List<AdminKycApplicationListResponse.Item> search(AdminKycApplicationSearchRequest request);

    /**
     * kyc_applications, corporates, users, kyc_supplements 조인 조건으로 KYC 신청 전체 건수를 조회합니다.
     *
     * <p>목록 조회와 동일하게 신청 상태, 키워드, 제출일 범위, AI 심사 상태,
     * 보완요청 여부 조건을 적용합니다.</p>
     *
     * @param request 검색 조건
     * @return 전체 건수
     */
    long count(AdminKycApplicationSearchRequest request);

    /**
     * kyc_applications.kyc_id 기준으로 corporates와 users를 조인하여 신청 법인정보를 조회합니다.
     *
     * <p>KYC 신청의 corporate_id로 corporates를 조인하고 applicant_user_id로 users를 조인합니다.</p>
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return KYC 신청 법인정보 Optional
     */
    Optional<AdminKycApplicationCorporateResponse> findCorporateByKycId(Long kycId);

    /**
     * KYC 신청 ID 기준으로 관리자 심사용 상세 정보를 조회합니다.
     *
     * <p>kyc_applications, corporates, users, kyc_documents, core_requests,
     * credentials, kyc_review_histories를 조인 또는 집계하여 상세 화면에 필요한 읽기 모델을 생성합니다.</p>
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return KYC 신청 상세 정보 Optional
     */
    Optional<AdminKycApplicationDetailResponse> findDetailByKycId(Long kycId);
}
