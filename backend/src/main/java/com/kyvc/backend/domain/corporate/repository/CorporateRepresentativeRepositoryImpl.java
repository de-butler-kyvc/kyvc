package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 법인 대표자 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CorporateRepresentativeRepositoryImpl implements CorporateRepresentativeRepository {

    private final CorporateRepresentativeJpaRepository corporateRepresentativeJpaRepository;

    // 대표자 ID 기준 대표자 조회
    @Override
    public Optional<CorporateRepresentative> findById(
            Long representativeId // 대표자 ID
    ) {
        return corporateRepresentativeJpaRepository.findById(representativeId);
    }

    // 법인 ID 기준 대표자 조회
    @Override
    public Optional<CorporateRepresentative> findByCorporateId(
            Long corporateId // 법인 ID
    ) {
        return corporateRepresentativeJpaRepository.findByCorporateId(corporateId);
    }

    // 대표자 저장
    @Override
    public CorporateRepresentative save(
            CorporateRepresentative representative // 저장 대상 대표자
    ) {
        return corporateRepresentativeJpaRepository.save(representative);
    }
}
