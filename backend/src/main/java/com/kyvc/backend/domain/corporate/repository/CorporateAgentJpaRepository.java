package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateAgent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 법인 대리인 JPA Repository
 */
public interface CorporateAgentJpaRepository extends JpaRepository<CorporateAgent, Long> {

    /**
     * 법인 ID 기준 대리인 목록 조회
     *
     * @param corporateId 법인 ID
     * @return 대리인 목록
     */
    List<CorporateAgent> findByCorporateIdOrderByCreatedAtDesc(
            Long corporateId // 법인 ID
    );
}
