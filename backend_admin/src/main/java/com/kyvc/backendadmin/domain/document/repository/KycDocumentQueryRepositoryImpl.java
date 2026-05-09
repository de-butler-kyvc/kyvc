package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.dto.AdminKycDocumentListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link KycDocumentQueryRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>kyc_documents와 DOCUMENT_TYPE 공통코드를 native SQL로 조인하여 문서 목록을 조회합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class KycDocumentQueryRepositoryImpl implements KycDocumentQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminKycDocumentListResponse.Item> findDocumentsByKycId(Long kycId) {
        Query query = entityManager().createNativeQuery("""
                select document.document_id,
                       document.document_type_code,
                       coalesce(common_code.code_name, document.document_type_code) as document_type_name,
                       document.file_name,
                       document.mime_type,
                       document.file_size,
                       document.upload_status_code,
                       document.uploaded_by_type_code,
                       document.uploaded_by_user_id,
                       document.uploaded_at
                from kyc_documents document
                left join common_code_groups code_group
                       on code_group.code_group = 'DOCUMENT_TYPE'
                left join common_codes common_code
                       on common_code.code_group_id = code_group.code_group_id
                      and common_code.code = document.document_type_code
                where document.kyc_id = :kycId
                order by document.uploaded_at desc, document.document_id desc
                """);
        query.setParameter("kycId", kycId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toItem((Object[]) row))
                .toList();
    }

    @Override
    public String findDocumentTypeName(String documentTypeCode) {
        Query query = entityManager().createNativeQuery("""
                select common_code.code_name
                from common_codes common_code
                join common_code_groups code_group on code_group.code_group_id = common_code.code_group_id
                where code_group.code_group = 'DOCUMENT_TYPE'
                  and common_code.code = :documentTypeCode
                """);
        query.setParameter("documentTypeCode", documentTypeCode);
        List<?> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(Object::toString)
                .orElse(null);
    }

    private AdminKycDocumentListResponse.Item toItem(Object[] row) {
        return new AdminKycDocumentListResponse.Item(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toLong(row[5]),
                toString(row[6]),
                toString(row[7]),
                toLong(row[8]),
                toLocalDateTime(row[9])
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
