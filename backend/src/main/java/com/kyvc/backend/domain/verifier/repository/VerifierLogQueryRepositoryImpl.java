package com.kyvc.backend.domain.verifier.repository;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Verifier log QueryRepository 구현체
@Repository
@RequiredArgsConstructor
public class VerifierLogQueryRepositoryImpl implements VerifierLogQueryRepository {

    private final EntityManager entityManager;

    @Override
    public List<VerifierLog> findLogs(
            Long verifierId, // Verifier ID
            KyvcEnums.VerifierActionType actionTypeCode, // 작업 유형
            LocalDateTime from, // 시작 일시
            LocalDateTime to, // 종료 일시
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        TypedQuery<VerifierLog> query = entityManager.createQuery(
                """
                        select l
                        from VerifierLog l
                        where l.verifierId = :verifierId
                          and (:actionTypeCode is null or l.actionTypeCode = :actionTypeCode)
                          and (:from is null or l.requestedAt >= :from)
                          and (:to is null or l.requestedAt <= :to)
                        order by l.requestedAt desc
                        """,
                VerifierLog.class
        );
        applyLogParameters(query, verifierId, actionTypeCode, from, to);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    public long countLogs(
            Long verifierId, // Verifier ID
            KyvcEnums.VerifierActionType actionTypeCode, // 작업 유형
            LocalDateTime from, // 시작 일시
            LocalDateTime to // 종료 일시
    ) {
        TypedQuery<Long> query = entityManager.createQuery(
                """
                        select count(l)
                        from VerifierLog l
                        where l.verifierId = :verifierId
                          and (:actionTypeCode is null or l.actionTypeCode = :actionTypeCode)
                          and (:from is null or l.requestedAt >= :from)
                          and (:to is null or l.requestedAt <= :to)
                        """,
                Long.class
        );
        applyLogParameters(query, verifierId, actionTypeCode, from, to);
        return query.getSingleResult();
    }

    @Override
    public List<VerifierLog> findLogsForStats(
            Long verifierId, // Verifier ID
            LocalDateTime from, // 시작 일시
            LocalDateTime to // 종료 일시
    ) {
        TypedQuery<VerifierLog> query = entityManager.createQuery(
                """
                        select l
                        from VerifierLog l
                        where l.verifierId = :verifierId
                          and (:from is null or l.requestedAt >= :from)
                          and (:to is null or l.requestedAt <= :to)
                        order by l.requestedAt asc
                        """,
                VerifierLog.class
        );
        query.setParameter("verifierId", verifierId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    private void applyLogParameters(
            TypedQuery<?> query, // JPQL query
            Long verifierId, // Verifier ID
            KyvcEnums.VerifierActionType actionTypeCode, // 작업 유형
            LocalDateTime from, // 시작 일시
            LocalDateTime to // 종료 일시
    ) {
        query.setParameter("verifierId", verifierId);
        query.setParameter("actionTypeCode", actionTypeCode);
        query.setParameter("from", from);
        query.setParameter("to", to);
    }
}
