package com.kyvc.backend.domain.credential.repository;

import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Credential 요청 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CredentialRequestQueryRepositoryImpl implements CredentialRequestQueryRepository {

    private final EntityManager entityManager;
    private final CredentialRequestJpaRepository credentialRequestJpaRepository;

    // 법인 기준 Credential 요청 목록 조회
    @Override
    public List<CredentialRequest> findByCorporateId(
            Long corporateId, // 법인 ID
            KyvcEnums.CredentialRequestType requestTypeCode, // 요청 유형
            KyvcEnums.CredentialRequestStatus requestStatusCode // 요청 상태
    ) {
        StringBuilder jpql = new StringBuilder("""
                select cr
                from CredentialRequest cr, Credential c
                where cr.credentialId = c.credentialId
                  and c.corporateId = :corporateId
                """); // Credential 소유 법인 기준 조회 JPQL
        if (requestTypeCode != null) {
            jpql.append(" and cr.requestType = :requestTypeCode");
        }
        if (requestStatusCode != null) {
            jpql.append(" and cr.requestStatus = :requestStatusCode");
        }
        jpql.append(" order by cr.requestedAt desc, cr.credentialRequestId desc");

        TypedQuery<CredentialRequest> query = entityManager.createQuery(jpql.toString(), CredentialRequest.class);
        query.setParameter("corporateId", corporateId);
        if (requestTypeCode != null) {
            query.setParameter("requestTypeCode", requestTypeCode);
        }
        if (requestStatusCode != null) {
            query.setParameter("requestStatusCode", requestStatusCode);
        }
        return query.getResultList();
    }

    // 법인 기준 Credential 요청 상세 조회
    @Override
    public Optional<CredentialRequest> findByCredentialRequestIdAndCorporateId(
            Long credentialRequestId, // Credential 요청 ID
            Long corporateId // 법인 ID
    ) {
        TypedQuery<CredentialRequest> query = entityManager.createQuery("""
                select cr
                from CredentialRequest cr, Credential c
                where cr.credentialId = c.credentialId
                  and cr.credentialRequestId = :credentialRequestId
                  and c.corporateId = :corporateId
                """, CredentialRequest.class);
        query.setParameter("credentialRequestId", credentialRequestId);
        query.setParameter("corporateId", corporateId);
        return query.getResultStream().findFirst();
    }

    // Credential ID 목록 기준 요청 이력 조회
    @Override
    public List<CredentialRequest> findByCredentialIds(
            Collection<Long> credentialIds, // Credential ID 목록
            KyvcEnums.CredentialRequestType requestType, // 요청 유형 필터
            KyvcEnums.CredentialRequestStatus requestStatus // 요청 상태 필터
    ) {
        if (credentialIds == null || credentialIds.isEmpty()) {
            return List.of();
        }
        return credentialRequestJpaRepository.findByCredentialIdInOrderByRequestedAtDesc(credentialIds).stream()
                .filter(request -> requestType == null || requestType == request.getRequestType())
                .filter(request -> requestStatus == null || requestStatus == request.getRequestStatus())
                .toList();
    }
}
