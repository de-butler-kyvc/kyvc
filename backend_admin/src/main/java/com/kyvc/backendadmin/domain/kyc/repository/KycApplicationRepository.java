package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.kyc.domain.KycApplication;

import java.util.Optional;

/**
 * KYC 신청 단건 조회 책임을 가지는 Repository입니다.
 *
 * <p>kyc_applications 테이블에서 kycId 기준 단건 조회를 담당하고,
 * 목록/검색과 조인 조회는 {@link KycApplicationQueryRepository}로 분리합니다.</p>
 */
public interface KycApplicationRepository {

    /**
     * kyc_applications 테이블에서 KYC 신청 ID 기준으로 신청을 조회합니다.
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return KYC 신청 Optional
     */
    Optional<KycApplication> findById(Long kycId);
}
