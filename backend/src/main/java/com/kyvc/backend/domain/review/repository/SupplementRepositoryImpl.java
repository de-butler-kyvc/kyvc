package com.kyvc.backend.domain.review.repository;

import com.kyvc.backend.domain.review.domain.KycSupplement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 보완요청 Repository 구현체
@Repository
@RequiredArgsConstructor
public class SupplementRepositoryImpl implements SupplementRepository {

    private final SupplementJpaRepository supplementJpaRepository;

    // 보완요청 ID 기준 조회
    @Override
    public Optional<KycSupplement> findById(
            Long supplementId // 보완요청 ID
    ) {
        return supplementJpaRepository.findById(supplementId);
    }

    // 보완요청 ID와 KYC 요청 ID 기준 조회
    @Override
    public Optional<KycSupplement> findByIdAndKycId(
            Long supplementId, // 보완요청 ID
            Long kycId // KYC 요청 ID
    ) {
        return supplementJpaRepository.findBySupplementIdAndKycId(supplementId, kycId);
    }

    // KYC 요청 ID 기준 보완요청 목록 조회
    @Override
    public List<KycSupplement> findByKycId(
            Long kycId // KYC 요청 ID
    ) {
        return supplementJpaRepository.findByKycIdOrderByRequestedAtDesc(kycId);
    }

    // 보완요청 저장
    @Override
    public KycSupplement save(
            KycSupplement supplement // 저장 대상 보완요청
    ) {
        return supplementJpaRepository.save(supplement);
    }
}
