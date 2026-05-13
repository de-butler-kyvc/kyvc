package com.kyvc.backend.domain.kyc.repository;

import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * KYC 신청 JPA Repository
 */
public interface KycApplicationJpaRepository extends JpaRepository<KycApplication, Long> {

    /**
     * 신청 사용자 ID 기준 목록 조회
     *
     * @param applicantUserId 신청 사용자 ID
     * @return KYC 신청 목록
     */
    List<KycApplication> findByApplicantUserIdOrderByCreatedAtDesc(
            Long applicantUserId // 신청 사용자 ID
    );

    /**
     * 신청 사용자 ID와 상태 목록 기준 현재 KYC 목록 조회
     *
     * @param applicantUserId 신청 사용자 ID
     * @param statuses 현재 상태 목록
     * @return 현재 KYC 신청 목록
     */
    List<KycApplication> findByApplicantUserIdAndKycStatusInOrderByUpdatedAtDesc(
            Long applicantUserId, // 신청 사용자 ID
            Collection<KyvcEnums.KycStatus> statuses // 현재 상태 목록
    );

    /**
     * 신청 사용자 ID 기준 최신 KYC 조회
     *
     * @param applicantUserId 신청 사용자 ID
     * @return 최신 KYC 신청 조회 결과
     */
    Optional<KycApplication> findFirstByApplicantUserIdOrderByCreatedAtDesc(
            Long applicantUserId // 신청 사용자 ID
    );

    /**
     * 신청 사용자 ID와 상태 목록 기준 최신 현재 KYC 조회
     *
     * @param applicantUserId 신청 사용자 ID
     * @param statuses 현재 상태 목록
     * @return 최신 현재 KYC 신청 조회 결과
     */
    Optional<KycApplication> findFirstByApplicantUserIdAndKycStatusInOrderByUpdatedAtDesc(
            Long applicantUserId, // 신청 사용자 ID
            Collection<KyvcEnums.KycStatus> statuses // 현재 상태 목록
    );

    /**
     * 진행 중 KYC 존재 여부 조회
     *
     * @param applicantUserId 신청 사용자 ID
     * @param statuses 진행 중 상태 목록
     * @return 진행 중 KYC 존재 여부
     */
    @Query("""
            select count(kycApplication) > 0
            from KycApplication kycApplication
            where kycApplication.applicantUserId = :applicantUserId
              and kycApplication.kycStatus in :statuses
            """)
    boolean existsInProgressByApplicantUserId(
            @Param("applicantUserId") Long applicantUserId, // 신청 사용자 ID
            @Param("statuses") Collection<KyvcEnums.KycStatus> statuses // 진행 중 상태 목록
    );
}
