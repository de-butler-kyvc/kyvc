package com.kyvc.backend.domain.kyc.repository;

import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// KYC 신청 Repository 구현체
@Repository
@RequiredArgsConstructor
public class KycApplicationRepositoryImpl implements KycApplicationRepository {

    private static final List<KyvcEnums.KycStatus> IN_PROGRESS_STATUSES = List.of(
            KyvcEnums.KycStatus.DRAFT,
            KyvcEnums.KycStatus.SUBMITTED,
            KyvcEnums.KycStatus.AI_REVIEWING,
            KyvcEnums.KycStatus.NEED_SUPPLEMENT,
            KyvcEnums.KycStatus.MANUAL_REVIEW,
            KyvcEnums.KycStatus.APPROVED
    ); // 진행 중 KYC 상태 목록

    private final KycApplicationJpaRepository kycApplicationJpaRepository;

    // KYC 신청 ID 기준 조회
    @Override
    public Optional<KycApplication> findById(
            Long kycId // KYC 신청 ID
    ) {
        return kycApplicationJpaRepository.findById(kycId);
    }

    // 신청 사용자 ID 기준 목록 조회
    @Override
    public List<KycApplication> findByApplicantUserId(
            Long userId // 사용자 ID
    ) {
        return kycApplicationJpaRepository.findByApplicantUserIdOrderByCreatedAtDesc(userId);
    }

    // 신청 사용자 ID 기준 현재 KYC 목록 조회
    @Override
    public List<KycApplication> findCurrentByApplicantUserId(
            Long userId // 사용자 ID
    ) {
        return kycApplicationJpaRepository.findByApplicantUserIdAndKycStatusInOrderByUpdatedAtDesc(
                userId,
                IN_PROGRESS_STATUSES
        );
    }

    // 신청 사용자 ID 기준 최신 KYC 조회
    @Override
    public Optional<KycApplication> findLatestByApplicantUserId(
            Long userId // 사용자 ID
    ) {
        return kycApplicationJpaRepository.findFirstByApplicantUserIdOrderByCreatedAtDesc(userId);
    }

    // 신청 사용자 ID 기준 최신 현재 KYC 조회
    @Override
    public Optional<KycApplication> findLatestCurrentByApplicantUserId(
            Long userId // 사용자 ID
    ) {
        return kycApplicationJpaRepository.findFirstByApplicantUserIdAndKycStatusInOrderByUpdatedAtDesc(
                userId,
                IN_PROGRESS_STATUSES
        );
    }

    // 신청 사용자 ID 기준 진행 중 KYC 존재 여부
    @Override
    public boolean existsInProgressByApplicantUserId(
            Long userId // 사용자 ID
    ) {
        return kycApplicationJpaRepository.existsInProgressByApplicantUserId(userId, IN_PROGRESS_STATUSES);
    }

    // 현재 상태 기준 KYC 상태 조건부 변경
    @Override
    public int updateStatusIfCurrentStatus(
            Long kycId, // KYC 신청 ID
            Long applicantUserId, // 신청 사용자 ID
            KyvcEnums.KycStatus currentStatus, // 현재 KYC 상태
            KyvcEnums.KycStatus nextStatus, // 변경 KYC 상태
            LocalDateTime submittedAt, // 제출 일시
            LocalDateTime updatedAt // 수정 일시
    ) {
        return kycApplicationJpaRepository.updateStatusIfCurrentStatus(
                kycId,
                applicantUserId,
                currentStatus,
                nextStatus,
                submittedAt,
                updatedAt
        );
    }

    // KYC 신청 저장
    @Override
    public KycApplication save(
            KycApplication kycApplication // 저장 대상 KYC 신청
    ) {
        return kycApplicationJpaRepository.save(kycApplication);
    }
}
