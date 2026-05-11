package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 금융사 방문 KYC Repository 구현체
@Repository
@RequiredArgsConstructor
public class FinanceKycApplicationRepositoryImpl implements FinanceKycApplicationRepository {

    private final KycApplicationJpaRepository kycApplicationJpaRepository;

    // KYC 신청 ID 기준 조회
    @Override
    public Optional<KycApplication> findById(
            Long kycId // KYC 신청 ID
    ) {
        return kycApplicationJpaRepository.findById(kycId);
    }

    // KYC 신청 저장
    @Override
    public KycApplication save(
            KycApplication kycApplication // 저장 대상 KYC 신청
    ) {
        return kycApplicationJpaRepository.save(kycApplication);
    }
}
