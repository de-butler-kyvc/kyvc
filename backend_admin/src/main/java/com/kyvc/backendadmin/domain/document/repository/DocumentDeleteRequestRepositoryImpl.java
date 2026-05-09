package com.kyvc.backendadmin.domain.document.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * {@link DocumentDeleteRequestRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class DocumentDeleteRequestRepositoryImpl implements DocumentDeleteRequestRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public Optional<Row> findById(Long requestId) {
        Query query = entityManager().createNativeQuery("""
                select request_id,
                       document_id,
                       requested_by_user_id,
                       request_status_code,
                       request_reason,
                       processed_by_admin_id,
                       processed_reason,
                       requested_at,
                       processed_at
                from document_delete_requests
                where request_id = :requestId
                """);
        query.setParameter("requestId", requestId);
        return query.getResultList().stream()
                .findFirst()
                .map(row -> toRow((Object[]) row));
    }

    @Override
    public int approve(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt) {
        return updateStatus(requestId, "APPROVED", adminId, processedReason, processedAt);
    }

    @Override
    public int reject(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt) {
        return updateStatus(requestId, "REJECTED", adminId, processedReason, processedAt);
    }

    private int updateStatus(
            Long requestId,
            String status,
            Long adminId,
            String processedReason,
            LocalDateTime processedAt
    ) {
        return entityManager().createNativeQuery("""
                        update document_delete_requests
                        set request_status_code = :status,
                            processed_by_admin_id = :adminId,
                            processed_reason = :processedReason,
                            processed_at = :processedAt
                        where request_id = :requestId
                        """)
                .setParameter("status", status)
                .setParameter("adminId", adminId)
                .setParameter("processedReason", processedReason)
                .setParameter("processedAt", processedAt)
                .setParameter("requestId", requestId)
                .executeUpdate();
    }

    private Row toRow(Object[] row) {
        return new Row(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toString(row[4]),
                toLong(row[5]),
                toString(row[6]),
                toLocalDateTime(row[7]),
                toLocalDateTime(row[8])
        );
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
