package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 법인 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CorporateRepositoryImpl implements CorporateRepository {

    private final CorporateJpaRepository corporateJpaRepository;

    // 법인 ID 기준 법인 조회
    @Override
    public Optional<Corporate> findById(
            Long corporateId // 법인 ID
    ) {
        return corporateJpaRepository.findById(corporateId);
    }

    // 사용자 ID 기준 법인 조회
    @Override
    public Optional<Corporate> findByUserId(
            Long userId // 사용자 ID
    ) {
        return corporateJpaRepository.findByUserId(userId);
    }

    // 사업자등록번호 기준 법인 조회
    @Override
    public Optional<Corporate> findByBusinessRegistrationNo(
            String businessRegistrationNo // 사업자등록번호
    ) {
        return corporateJpaRepository.findByBusinessRegistrationNo(businessRegistrationNo);
    }

    // 사용자 ID 기준 법인 존재 여부
    @Override
    public boolean existsByUserId(
            Long userId // 사용자 ID
    ) {
        return corporateJpaRepository.existsByUserId(userId);
    }

    // 사업자등록번호 기준 법인 존재 여부
    @Override
    public boolean existsByBusinessRegistrationNo(
            String businessRegistrationNo // 사업자등록번호
    ) {
        return corporateJpaRepository.existsByBusinessRegistrationNo(businessRegistrationNo);
    }

    // 다른 법인의 사업자등록번호 사용 여부
    @Override
    public boolean existsByBusinessRegistrationNoAndCorporateIdNot(
            String businessRegistrationNo, // 사업자등록번호
            Long corporateId // 제외할 법인 ID
    ) {
        return corporateJpaRepository.existsByBusinessRegistrationNoAndCorporateIdNot(
                businessRegistrationNo,
                corporateId
        );
    }

    // 법인 저장
    @Override
    public Corporate save(
            Corporate corporate // 저장 대상 법인
    ) {
        return corporateJpaRepository.save(corporate);
    }
}
