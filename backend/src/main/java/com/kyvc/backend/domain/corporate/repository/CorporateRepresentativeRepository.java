package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;

import java.util.Optional;

/**
 * 법인 대표자 Repository
 */
public interface CorporateRepresentativeRepository {

    /**
     * 대표자 ID 기준 대표자 조회
     *
     * @param representativeId 대표자 ID
     * @return 대표자 조회 결과
     */
    Optional<CorporateRepresentative> findById(
            Long representativeId // 대표자 ID
    );

    /**
     * 법인 ID 기준 대표자 조회
     *
     * @param corporateId 법인 ID
     * @return 대표자 조회 결과
     */
    Optional<CorporateRepresentative> findByCorporateId(
            Long corporateId // 법인 ID
    );

    /**
     * 대표자 저장
     *
     * @param representative 저장 대상 대표자
     * @return 저장된 대표자
     */
    CorporateRepresentative save(
            CorporateRepresentative representative // 저장 대상 대표자
    );
}
