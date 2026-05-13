package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSearchRequest;
import com.kyvc.backendadmin.domain.document.dto.DocumentDeleteRequestSummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link DocumentDeleteRequestQueryRepository}의 native SQL 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class DocumentDeleteRequestQueryRepositoryImpl implements DocumentDeleteRequestQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<DocumentDeleteRequestSummaryResponse> search(DocumentDeleteRequestSearchRequest request) {
        QueryParts parts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select delete_request.request_id,
                       document.kyc_id,
                       delete_request.document_id,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       document.document_type_code,
                       delete_request.requested_by_user_id,
                       delete_request.request_reason,
                       delete_request.request_status_code,
                       delete_request.requested_at,
                       delete_request.processed_by_admin_id,
                       delete_request.processed_at
                from document_delete_requests delete_request
                join kyc_documents document on document.document_id = delete_request.document_id
                left join kyc_applications kyc on kyc.kyc_id = document.kyc_id
                left join corporates corporate on corporate.corporate_id = kyc.corporate_id
                left join users requested_user on requested_user.user_id = delete_request.requested_by_user_id
                %s
                order by delete_request.requested_at desc, delete_request.request_id desc
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        return query.getResultList().stream()
                .map(row -> toSummary((Object[]) row))
                .toList();
    }

    @Override
    public long count(DocumentDeleteRequestSearchRequest request) {
        QueryParts parts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select count(*)
                from document_delete_requests delete_request
                join kyc_documents document on document.document_id = delete_request.document_id
                left join kyc_applications kyc on kyc.kyc_id = document.kyc_id
                left join corporates corporate on corporate.corporate_id = kyc.corporate_id
                left join users requested_user on requested_user.user_id = delete_request.requested_by_user_id
                %s
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    private QueryParts buildQueryParts(DocumentDeleteRequestSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();
        if (StringUtils.hasText(request.status())) {
            where.append(" and delete_request.request_status_code = :status");
            parameters.put("status", request.status());
        }
        if (StringUtils.hasText(request.keyword())) {
            where.append("""
                     and (
                         lower(coalesce(corporate.corporate_name, '')) like :keyword
                         or lower(document.file_name) like :keyword
                         or lower(document.document_type_code) like :keyword
                         or lower(coalesce(requested_user.email, '')) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + request.keyword().trim().toLowerCase() + "%");
        }
        if (request.kycId() != null) {
            where.append(" and document.kyc_id = :kycId");
            parameters.put("kycId", request.kycId());
        }
        if (request.corporateId() != null) {
            where.append(" and corporate.corporate_id = :corporateId");
            parameters.put("corporateId", request.corporateId());
        }
        if (request.documentId() != null) {
            where.append(" and delete_request.document_id = :documentId");
            parameters.put("documentId", request.documentId());
        }
        if (request.requesterId() != null) {
            where.append(" and delete_request.requested_by_user_id = :requesterId");
            parameters.put("requesterId", request.requesterId());
        }
        if (request.startDate() != null) {
            where.append(" and delete_request.requested_at >= :startDate");
            parameters.put("startDate", request.startDate().atStartOfDay());
        }
        if (request.endDate() != null) {
            where.append(" and delete_request.requested_at < :endDateExclusive");
            parameters.put("endDateExclusive", request.endDate().plusDays(1).atStartOfDay());
        }
        return new QueryParts(where.toString(), parameters);
    }

    private DocumentDeleteRequestSummaryResponse toSummary(Object[] row) {
        LocalDateTime requestedAt = toLocalDateTime(row[9]);
        LocalDateTime processedAt = toLocalDateTime(row[11]);
        return new DocumentDeleteRequestSummaryResponse(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3]),
                toString(row[4]),
                toString(row[5]),
                toLong(row[6]),
                toString(row[7]),
                toString(row[8]),
                requestedAt == null ? null : requestedAt.toString(),
                toLong(row[10]),
                processedAt == null ? null : processedAt.toString()
        );
    }

    private void bind(Query query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
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

    private record QueryParts(String whereClause, Map<String, Object> parameters) {
    }
}
