package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestListResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentDetailResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 사용자 문서함 조회 Repository 구현체
@Repository
@RequiredArgsConstructor
public class UserDocumentQueryRepositoryImpl implements UserDocumentQueryRepository {

    private static final String LATEST_DELETE_REQUEST_CTE = """
            with latest_delete_request as (
                select *
                from (
                    select delete_request.*,
                           row_number() over (
                               partition by delete_request.document_id
                               order by delete_request.requested_at desc,
                                        delete_request.request_id desc
                           ) as rn
                    from document_delete_requests delete_request
                ) ranked_delete_request
                where ranked_delete_request.rn = 1
            )
            """;

    private final EntityManager entityManager;

    // 사용자 소유 문서 목록 조회
    @Override
    public List<UserDocumentListResponse.Item> searchDocuments(
            Long userId, // 사용자 ID
            String documentTypeCode, // 문서 유형 코드
            String status, // 문서 상태 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        QueryParts parts = buildDocumentQueryParts(userId, documentTypeCode, status);
        Query query = entityManager.createNativeQuery("""
                %s
                select document.document_id,
                       document.kyc_id,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       document.document_type_code,
                       coalesce(common_code.code_name, document.document_type_code) as document_type_name,
                       document.file_name,
                       document.mime_type,
                       document.file_size,
                       document.upload_status_code,
                       document.uploaded_at,
                       kyc.kyc_status_code,
                       latest_delete_request.request_id,
                       latest_delete_request.request_status_code
                from kyc_documents document
                join kyc_applications kyc on kyc.kyc_id = document.kyc_id
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                left join common_code_groups code_group on code_group.code_group = 'DOCUMENT_TYPE'
                left join common_codes common_code
                       on common_code.code_group_id = code_group.code_group_id
                      and common_code.code = document.document_type_code
                left join latest_delete_request on latest_delete_request.document_id = document.document_id
                %s
                order by document.uploaded_at desc, document.document_id desc
                """.formatted(LATEST_DELETE_REQUEST_CTE, parts.whereClause()));
        bind(query, parts.parameters());
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream()
                .map(row -> toDocumentItem((Object[]) row))
                .toList();
    }

    // 사용자 소유 문서 건수 조회
    @Override
    public long countDocuments(
            Long userId, // 사용자 ID
            String documentTypeCode, // 문서 유형 코드
            String status // 문서 상태 코드
    ) {
        QueryParts parts = buildDocumentQueryParts(userId, documentTypeCode, status);
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from kyc_documents document
                join kyc_applications kyc on kyc.kyc_id = document.kyc_id
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                %s
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    // 사용자 소유 문서 상세 조회
    @Override
    public Optional<UserDocumentDetailResponse> findDocumentDetail(
            Long userId, // 사용자 ID
            Long documentId // 문서 ID
    ) {
        Query query = entityManager.createNativeQuery("""
                %s
                select document.document_id,
                       document.kyc_id,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       document.document_type_code,
                       coalesce(common_code.code_name, document.document_type_code) as document_type_name,
                       document.file_name,
                       document.mime_type,
                       document.file_size,
                       document.upload_status_code,
                       document.uploaded_by_type_code,
                       document.uploaded_by_user_id,
                       document.uploaded_at,
                       kyc.kyc_status_code,
                       kyc.submitted_at,
                       latest_delete_request.request_id,
                       latest_delete_request.request_status_code,
                       latest_delete_request.request_reason,
                       latest_delete_request.requested_at
                from kyc_documents document
                join kyc_applications kyc on kyc.kyc_id = document.kyc_id
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                left join common_code_groups code_group on code_group.code_group = 'DOCUMENT_TYPE'
                left join common_codes common_code
                       on common_code.code_group_id = code_group.code_group_id
                      and common_code.code = document.document_type_code
                left join latest_delete_request on latest_delete_request.document_id = document.document_id
                where corporate.user_id = :userId
                  and document.document_id = :documentId
                """.formatted(LATEST_DELETE_REQUEST_CTE));
        query.setParameter("userId", userId);
        query.setParameter("documentId", documentId);
        return query.getResultList().stream()
                .findFirst()
                .map(row -> toDocumentDetail((Object[]) row));
    }

    // 사용자 소유 문서 삭제 요청 이력 조회
    @Override
    public List<UserDocumentDeleteRequestListResponse.Item> searchDeleteRequests(
            Long userId, // 사용자 ID
            String status, // 삭제 요청 상태 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        QueryParts parts = buildDeleteRequestQueryParts(userId, status);
        Query query = entityManager.createNativeQuery("""
                select delete_request.request_id,
                       delete_request.document_id,
                       document.kyc_id,
                       corporate.corporate_id,
                       corporate.corporate_name,
                       document.document_type_code,
                       coalesce(common_code.code_name, document.document_type_code) as document_type_name,
                       document.file_name,
                       delete_request.request_status_code,
                       delete_request.request_reason,
                       delete_request.requested_at,
                       delete_request.processed_at
                from document_delete_requests delete_request
                join kyc_documents document on document.document_id = delete_request.document_id
                join kyc_applications kyc on kyc.kyc_id = document.kyc_id
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                left join common_code_groups code_group on code_group.code_group = 'DOCUMENT_TYPE'
                left join common_codes common_code
                       on common_code.code_group_id = code_group.code_group_id
                      and common_code.code = document.document_type_code
                %s
                order by delete_request.requested_at desc, delete_request.request_id desc
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream()
                .map(row -> toDeleteRequestItem((Object[]) row))
                .toList();
    }

    // 사용자 소유 문서 삭제 요청 건수 조회
    @Override
    public long countDeleteRequests(
            Long userId, // 사용자 ID
            String status // 삭제 요청 상태 코드
    ) {
        QueryParts parts = buildDeleteRequestQueryParts(userId, status);
        Query query = entityManager.createNativeQuery("""
                select count(*)
                from document_delete_requests delete_request
                join kyc_documents document on document.document_id = delete_request.document_id
                join kyc_applications kyc on kyc.kyc_id = document.kyc_id
                join corporates corporate on corporate.corporate_id = kyc.corporate_id
                %s
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    // 문서 조회 조건 조립
    private QueryParts buildDocumentQueryParts(
            Long userId, // 사용자 ID
            String documentTypeCode, // 문서 유형 코드
            String status // 문서 상태 코드
    ) {
        StringBuilder where = new StringBuilder("where corporate.user_id = :userId");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        if (StringUtils.hasText(documentTypeCode)) {
            where.append(" and document.document_type_code = :documentTypeCode");
            parameters.put("documentTypeCode", documentTypeCode);
        }
        if (StringUtils.hasText(status)) {
            where.append(" and document.upload_status_code = :status");
            parameters.put("status", status);
        }
        return new QueryParts(where.toString(), parameters);
    }

    // 삭제 요청 조회 조건 조립
    private QueryParts buildDeleteRequestQueryParts(
            Long userId, // 사용자 ID
            String status // 삭제 요청 상태 코드
    ) {
        StringBuilder where = new StringBuilder("where corporate.user_id = :userId");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        if (StringUtils.hasText(status)) {
            where.append(" and delete_request.request_status_code = :status");
            parameters.put("status", status);
        }
        return new QueryParts(where.toString(), parameters);
    }

    // 사용자 문서 목록 항목 변환
    private UserDocumentListResponse.Item toDocumentItem(Object[] row // 문서 원본 행
    ) {
        return new UserDocumentListResponse.Item(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toLong(row[8]),
                toString(row[9]),
                toLocalDateTime(row[10]),
                toString(row[11]),
                toLong(row[12]),
                toString(row[13])
        );
    }

    // 사용자 문서 상세 변환
    private UserDocumentDetailResponse toDocumentDetail(Object[] row // 문서 상세 원본 행
    ) {
        return new UserDocumentDetailResponse(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toLong(row[8]),
                toString(row[9]),
                toString(row[10]),
                toLong(row[11]),
                toLocalDateTime(row[12]),
                toString(row[13]),
                toLocalDateTime(row[14]),
                toLong(row[15]),
                toString(row[16]),
                toString(row[17]),
                toLocalDateTime(row[18])
        );
    }

    // 사용자 문서 삭제 요청 항목 변환
    private UserDocumentDeleteRequestListResponse.Item toDeleteRequestItem(Object[] row // 삭제 요청 원본 행
    ) {
        return new UserDocumentDeleteRequestListResponse.Item(
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
                toLocalDateTime(row[10]),
                toLocalDateTime(row[11])
        );
    }

    // 파라미터 바인딩
    private void bind(
            Query query, // native query
            Map<String, Object> parameters // 바인딩 파라미터
    ) {
        parameters.forEach(query::setParameter);
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

    private record QueryParts(
            String whereClause,
            Map<String, Object> parameters
    ) {
    }
}
