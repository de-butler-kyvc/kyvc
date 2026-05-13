package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// Credential 상태 이력 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialStatusHistoryQueryRepositoryImpl implements CredentialStatusHistoryQueryRepository {

    private final EntityManager entityManager;

    // Credential 소유 법인 기준 상태 이력 조회
    @Override
    public List<CredentialStatusHistory> findByCredentialIdAndCorporateId(
            Long credentialId, // Credential ID
            Long corporateId // 법인 ID
    ) {
        TypedQuery<CredentialStatusHistory> query = entityManager.createQuery("""
                select h
                from CredentialStatusHistory h, Credential c
                where h.credentialId = c.credentialId
                  and h.credentialId = :credentialId
                  and c.corporateId = :corporateId
                order by h.changedAt desc, h.historyId desc
                """, CredentialStatusHistory.class);
        query.setParameter("credentialId", credentialId);
        query.setParameter("corporateId", corporateId);
        return query.getResultList();
    }
}
