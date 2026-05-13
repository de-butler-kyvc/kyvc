package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 법인 대리인 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CorporateAgentRepositoryImpl implements CorporateAgentRepository {

    private final CorporateAgentJpaRepository corporateAgentJpaRepository;

    // 대리인 ID 기준 대리인 조회
    @Override
    public Optional<CorporateAgent> findById(
            Long agentId // 대리인 ID
    ) {
        return corporateAgentJpaRepository.findById(agentId);
    }

    // 법인 ID 기준 대리인 목록 조회
    @Override
    public List<CorporateAgent> findByCorporateId(
            Long corporateId // 법인 ID
    ) {
        return corporateAgentJpaRepository.findByCorporateIdOrderByCreatedAtDesc(corporateId);
    }

    // 대리인 저장
    @Override
    public CorporateAgent save(
            CorporateAgent agent // 저장 대상 대리인
    ) {
        return corporateAgentJpaRepository.save(agent);
    }
}
