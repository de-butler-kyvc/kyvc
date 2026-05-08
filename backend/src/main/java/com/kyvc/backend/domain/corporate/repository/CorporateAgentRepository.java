package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateAgent;

import java.util.List;
import java.util.Optional;

/**
 * 법인 대리인 Repository
 */
public interface CorporateAgentRepository {

    /**
     * 대리인 ID 기준 대리인 조회
     *
     * @param agentId 대리인 ID
     * @return 대리인 조회 결과
     */
    Optional<CorporateAgent> findById(
            Long agentId // 대리인 ID
    );

    /**
     * 법인 ID 기준 대리인 목록 조회
     *
     * @param corporateId 법인 ID
     * @return 대리인 목록
     */
    List<CorporateAgent> findByCorporateId(
            Long corporateId // 법인 ID
    );

    /**
     * 대리인 저장
     *
     * @param agent 저장 대상 대리인
     * @return 저장된 대리인
     */
    CorporateAgent save(
            CorporateAgent agent // 저장 대상 대리인
    );
}
