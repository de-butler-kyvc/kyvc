package com.kyvc.backend.domain.audit.repository;

import com.kyvc.backend.domain.corporate.dto.CorporateChangeHistoryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

// 감사로그 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class AuditLogQueryRepositoryImpl implements AuditLogQueryRepository {

    private static final String CORPORATE_TARGET_TYPE = "CORPORATE"; // 법인 감사 대상 유형

    private final EntityManager entityManager;

    // 법인 대상 감사로그 목록 조회
    @Override
    public List<CorporateChangeHistoryResponse.Item> findCorporateHistories(
            Long corporateId, // 법인 ID
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        Query query = entityManager.createNativeQuery("""
                select audit_log.audit_log_id,
                       audit_log.actor_type_code,
                       audit_log.actor_id,
                       audit_log.action_type,
                       audit_log.audit_target_type_code,
                       audit_log.target_id,
                       audit_log.request_summary,
                       audit_log.created_at
                from audit_logs audit_log
                where audit_log.audit_target_type_code = :targetType
                  and audit_log.target_id = :corporateId
                order by audit_log.created_at desc, audit_log.audit_log_id desc
                """);
        query.setParameter("targetType", CORPORATE_TARGET_TYPE);
        query.setParameter("corporateId", corporateId);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream()
                .map(row -> toItem((Object[]) row))
                .toList();
    }

    // 법인 대상 감사로그 건수 조회
    @Override
    public long countCorporateHistories(
            Long corporateId // 법인 ID
    ) {
        Number count = (Number) entityManager.createNativeQuery("""
                        select count(*)
                        from audit_logs audit_log
                        where audit_log.audit_target_type_code = :targetType
                          and audit_log.target_id = :corporateId
                        """)
                .setParameter("targetType", CORPORATE_TARGET_TYPE)
                .setParameter("corporateId", corporateId)
                .getSingleResult();
        return count.longValue();
    }

    // 변경 이력 항목 변환
    private CorporateChangeHistoryResponse.Item toItem(Object[] row // 감사로그 원본 행
    ) {
        return new CorporateChangeHistoryResponse.Item(
                toLong(row[0]),
                toString(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toString(row[4]),
                toLong(row[5]),
                toString(row[6]),
                toLocalDateTime(row[7])
        );
    }

    // Long 변환
    private Long toLong(Object value // 원본 값
    ) {
        return value == null ? null : ((Number) value).longValue();
    }

    // 문자열 변환
    private String toString(Object value // 원본 값
    ) {
        return value == null ? null : value.toString();
    }

    // LocalDateTime 변환
    private LocalDateTime toLocalDateTime(Object value // 원본 값
    ) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
