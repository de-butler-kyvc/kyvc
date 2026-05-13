package com.kyvc.backend.domain.did.repository;

import com.kyvc.backend.domain.did.domain.DidInstitution;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// DID 기관 매핑 Repository 구현체
@Repository
@RequiredArgsConstructor
public class DidInstitutionRepositoryImpl implements DidInstitutionRepository {

    private final DidInstitutionJpaRepository didInstitutionJpaRepository;

    // DID 기준 기관 매핑 조회
    @Override
    public Optional<DidInstitution> findByDid(
            String did // DID
    ) {
        return didInstitutionJpaRepository.findById(did);
    }

    // ACTIVE DID 기준 기관 매핑 조회
    @Override
    public Optional<DidInstitution> findActiveByDid(
            String did // DID
    ) {
        return didInstitutionJpaRepository.findByDidAndStatusCode(
                did,
                KyvcEnums.DidInstitutionStatus.ACTIVE
        );
    }

    // DID 기준 존재 여부 조회
    @Override
    public boolean existsByDid(
            String did // DID
    ) {
        return didInstitutionJpaRepository.existsById(did);
    }

    // DID 기관 매핑 저장
    @Override
    public DidInstitution save(
            DidInstitution didInstitution // 저장 대상 DID 기관 매핑
    ) {
        return didInstitutionJpaRepository.save(didInstitution);
    }
}
