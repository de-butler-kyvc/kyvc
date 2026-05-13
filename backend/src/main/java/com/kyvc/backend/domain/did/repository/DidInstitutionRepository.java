package com.kyvc.backend.domain.did.repository;

import com.kyvc.backend.domain.did.domain.DidInstitution;

import java.util.Optional;

/**
 * DID 기관 매핑 Repository
 */
public interface DidInstitutionRepository {

    /**
     * DID 기준 기관 매핑 조회
     *
     * @param did DID
     * @return DID 기관 매핑 조회 결과
     */
    Optional<DidInstitution> findByDid(
            String did // DID
    );

    /**
     * ACTIVE DID 기준 기관 매핑 조회
     *
     * @param did DID
     * @return ACTIVE DID 기관 매핑 조회 결과
     */
    Optional<DidInstitution> findActiveByDid(
            String did // DID
    );

    /**
     * DID 기준 존재 여부 조회
     *
     * @param did DID
     * @return DID 존재 여부
     */
    boolean existsByDid(
            String did // DID
    );

    /**
     * DID 기관 매핑 저장
     *
     * @param didInstitution 저장 대상 DID 기관 매핑
     * @return 저장된 DID 기관 매핑
     */
    DidInstitution save(
            DidInstitution didInstitution // 저장 대상 DID 기관 매핑
    );
}
