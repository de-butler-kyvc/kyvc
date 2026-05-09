package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestDetailResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestListResponse;
import com.kyvc.backendadmin.domain.document.dto.AdminDocumentDeleteRequestSearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AdminDocumentDeleteRequestQueryRepositoryImpl implements AdminDocumentDeleteRequestQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminDocumentDeleteRequestListResponse.Item> findDeleteRequests(AdminDocumentDeleteRequestSearchRequest request) {
        StringBuilder sql = new StringBuilder("""
                select delete_request.request_id,
                       delete_request.document_id,
                       document.kyc_id,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       document.document_type_code,
                       document.file_name,
                       delete_request.requested_by_user_id,
                       requested_user.email as requested_by_user_email,
                       delete_request.request_status_code,
                       delete_request.requested_at,
                       delete_request.processed_at
                from document_delete_requests delete_request
                left join kyc_documents document
                       on document.document_id = delete_request.document_id
                left join kyc_applications application
                       on application.kyc_id = document.kyc_id
                left join corporates corporate
                       on corporate.corporate_id = application.corporate_id
                left join users requested_user
                       on requested_user.user_id = delete_request.requested_by_user_id
                where 1 = 1
                """);
        appendFilters(sql, request);
        sql.append(" order by delete_request.requested_at desc, delete_request.request_id desc");
        sql.append(" limit :limit offset :offset");

        Query query = entityManager().createNativeQuery(sql.toString());
        bindFilters(query, request);
        query.setParameter("limit", request.normalizedSize());
        query.setParameter("offset", request.normalizedPage() * request.normalizedSize());
        return query.getResultList()
                .stream()
                .map(row -> toSummary((Object[]) row))
                .toList();
    }

    @Override
    public long countDeleteRequests(AdminDocumentDeleteRequestSearchRequest request) {
        StringBuilder sql = new StringBuilder("""
                select count(*)
                from document_delete_requests delete_request
                left join kyc_documents document
                       on document.document_id = delete_request.document_id
                left join kyc_applications application
                       on application.kyc_id = document.kyc_id
                left join corporates corporate
                       on corporate.corporate_id = application.corporate_id
                left join users requested_user
                       on requested_user.user_id = delete_request.requested_by_user_id
                where 1 = 1
                """);
        appendFilters(sql, request);

        Query query = entityManager().createNativeQuery(sql.toString());
        bindFilters(query, request);
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<AdminDocumentDeleteRequestDetailResponse> findDeleteRequestDetail(Long requestId) {
        Query query = entityManager().createNativeQuery("""
                select delete_request.request_id,
                       delete_request.document_id,
                       document.kyc_id,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       document.document_type_code,
                       document.file_name,
                       null as file_path,
                       delete_request.request_reason,
                       delete_request.request_status_code,
                       delete_request.requested_by_user_id,
                       requested_user.email as requested_by_user_email,
                       delete_request.processed_by_admin_id,
                       processed_admin.name as processed_by_admin_name,
                       delete_request.processed_reason,
                       delete_request.requested_at,
                       delete_request.processed_at
                from document_delete_requests delete_request
                left join kyc_documents document
                       on document.document_id = delete_request.document_id
                left join kyc_applications application
                       on application.kyc_id = document.kyc_id
                left join corporates corporate
                       on corporate.corporate_id = application.corporate_id
                left join users requested_user
                       on requested_user.user_id = delete_request.requested_by_user_id
                left join admin_users processed_admin
                       on processed_admin.admin_id = delete_request.processed_by_admin_id
                where delete_request.request_id = :requestId
                """);
        query.setParameter("requestId", requestId);
        List<?> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toDetail((Object[]) rows.get(0)));
    }

    @Override
    public Optional<DocumentDeleteRequestState> findDeleteRequestState(Long requestId) {
        Query query = entityManager().createNativeQuery("""
                select request_id,
                       document_id,
                       request_status_code,
                       processed_by_admin_id,
                       processed_reason,
                       processed_at
                from document_delete_requests
                where request_id = :requestId
                """);
        query.setParameter("requestId", requestId);
        List<?> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = (Object[]) rows.get(0);
        return Optional.of(new DocumentDeleteRequestState(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toLong(row[3]),
                toString(row[4]),
                toLocalDateTime(row[5])
        ));
    }

    @Override
    public Optional<KycDocumentDeleteState> findKycDocumentState(Long documentId) {
        Query query = entityManager().createNativeQuery("""
                select document_id,
                       kyc_id,
                       upload_status_code
                from kyc_documents
                where document_id = :documentId
                """);
        query.setParameter("documentId", documentId);
        List<?> rows = query.getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = (Object[]) rows.get(0);
        return Optional.of(new KycDocumentDeleteState(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2])
        ));
    }

    @Override
    public int markKycDocumentDeleted(Long documentId) {
        Query query = entityManager().createNativeQuery("""
                update kyc_documents
                   set upload_status_code = 'DELETED'
                 where document_id = :documentId
                """);
        query.setParameter("documentId", documentId);
        return query.executeUpdate();
    }

    @Override
    public int approveDeleteRequest(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt) {
        return processDeleteRequest(requestId, adminId, processedReason, processedAt, "APPROVED");
    }

    @Override
    public int rejectDeleteRequest(Long requestId, Long adminId, String processedReason, LocalDateTime processedAt) {
        return processDeleteRequest(requestId, adminId, processedReason, processedAt, "REJECTED");
    }

    private int processDeleteRequest(
            Long requestId,
            Long adminId,
            String processedReason,
            LocalDateTime processedAt,
            String targetStatus
    ) {
        Query query = entityManager().createNativeQuery("""
                update document_delete_requests
                   set request_status_code = :targetStatus,
                       processed_by_admin_id = :adminId,
                       processed_reason = :processedReason,
                       processed_at = :processedAt
                 where request_id = :requestId
                   and request_status_code = 'REQUESTED'
                """);
        query.setParameter("targetStatus", targetStatus);
        query.setParameter("adminId", adminId);
        query.setParameter("processedReason", processedReason);
        query.setParameter("processedAt", processedAt);
        query.setParameter("requestId", requestId);
        return query.executeUpdate();
    }

    private void appendFilters(StringBuilder sql, AdminDocumentDeleteRequestSearchRequest request) {
        if (StringUtils.hasText(request.status())) {
            sql.append(" and delete_request.request_status_code = :status");
        }
        if (StringUtils.hasText(request.keyword())) {
            sql.append("""
                     and (
                         lower(coalesce(corporate.corporate_name, '')) like :keyword
                      or lower(coalesce(document.file_name, '')) like :keyword
                      or lower(coalesce(requested_user.email, '')) like :keyword
                     )
                    """);
        }
        if (request.fromDate() != null) {
            sql.append(" and delete_request.requested_at >= :fromDate");
        }
        if (request.toDate() != null) {
            sql.append(" and delete_request.requested_at < :toDate");
        }
    }

    private void bindFilters(Query query, AdminDocumentDeleteRequestSearchRequest request) {
        if (StringUtils.hasText(request.status())) {
            query.setParameter("status", request.status());
        }
        if (StringUtils.hasText(request.keyword())) {
            query.setParameter("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (request.fromDate() != null) {
            query.setParameter("fromDate", request.fromDate().atStartOfDay());
        }
        if (request.toDate() != null) {
            query.setParameter("toDate", request.toDate().plusDays(1).atStartOfDay());
        }
    }

    private AdminDocumentDeleteRequestListResponse.Item toSummary(Object[] row) {
        return new AdminDocumentDeleteRequestListResponse.Item(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toLong(row[7]),
                toString(row[8]),
                toString(row[9]),
                toLocalDateTime(row[10]),
                toLocalDateTime(row[11])
        );
    }

    private AdminDocumentDeleteRequestDetailResponse toDetail(Object[] row) {
        return new AdminDocumentDeleteRequestDetailResponse(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toLong(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toString(row[9]),
                toLong(row[10]),
                toString(row[11]),
                toLong(row[12]),
                toString(row[13]),
                toString(row[14]),
                toLocalDateTime(row[15]),
                toLocalDateTime(row[16])
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
