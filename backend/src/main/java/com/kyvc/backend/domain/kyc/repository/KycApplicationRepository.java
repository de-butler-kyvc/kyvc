package com.kyvc.backend.domain.kyc.repository;

import com.kyvc.backend.domain.kyc.domain.KycApplication;

import java.util.List;
import java.util.Optional;

/**
 * KYC 신청 Repository
 */
public interface KycApplicationRepository {

    /**
     * KYC 신청 ID 기준 조회
     *
     * @param kycId KYC 신청 ID
     * @return KYC 신청 조회 결과
     */
    Optional<KycApplication> findById(
            Long kycId // KYC 신청 ID
    );

    /**
     * 신청 사용자 ID 기준 목록 조회
     *
     * @param userId 사용자 ID
     * @return KYC 신청 목록
     */
    List<KycApplication> findByApplicantUserId(
            Long userId // 사용자 ID
    );

    /**
     * 신청 사용자 ID 기준 현재 KYC 목록 조회
     *
     * @param userId 사용자 ID
     * @return 현재 KYC 신청 목록
     */
    List<KycApplication> findCurrentByApplicantUserId(
            Long userId // 사용자 ID
    );

    /**
     * 신청 사용자 ID 기준 최신 KYC 조회
     *
     * @param userId 사용자 ID
     * @return 최신 KYC 신청 조회 결과
     */
    Optional<KycApplication> findLatestByApplicantUserId(
            Long userId // 사용자 ID
    );

    /**
     * 신청 사용자 ID 기준 최신 현재 KYC 조회
     *
     * @param userId 사용자 ID
     * @return 최신 현재 KYC 신청 조회 결과
     */
    Optional<KycApplication> findLatestCurrentByApplicantUserId(
            Long userId // 사용자 ID
    );

    /**
     * 신청 사용자 ID 기준 진행 중 KYC 존재 여부
     *
     * @param userId 사용자 ID
     * @return 진행 중 KYC 존재 여부
     */
    boolean existsInProgressByApplicantUserId(
            Long userId // 사용자 ID
    );

    /**
     * KYC 신청 저장
     *
     * @param kycApplication 저장 대상 KYC 신청
     * @return 저장된 KYC 신청
     */
    KycApplication save(
            KycApplication kycApplication // 저장 대상 KYC 신청
    );
}
