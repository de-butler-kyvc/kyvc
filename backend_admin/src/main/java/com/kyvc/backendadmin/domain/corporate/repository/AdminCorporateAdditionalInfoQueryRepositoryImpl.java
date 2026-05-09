package com.kyvc.backendadmin.domain.corporate.repository;

import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateAgentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateDocumentResponse;
import com.kyvc.backendadmin.domain.corporate.dto.AdminCorporateRepresentativeResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link AdminCorporateAdditionalInfoQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class AdminCorporateAdditionalInfoQueryRepositoryImpl implements AdminCorporateAdditionalInfoQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminCorporateRepresentativeResponse> findRepresentativesByCorporateId(Long corporateId) {
        Query query = entityManager().createNativeQuery("""
                select representative.representative_id,
                       representative.corporate_id,
                       representative.representative_name,
                       representative.birth_date,
                       representative.nationality_code,
                       representative.phone,
                       representative.email,
                       representative.identity_document_id,
                       identity_document.file_name as identity_document_name,
                       representative.active_yn,
                       representative.created_at,
                       representative.updated_at
                from corporate_representatives representative
                left join corporate_documents identity_document
                       on identity_document.corporate_document_id = representative.identity_document_id
                where representative.corporate_id = :corporateId
                order by representative.created_at desc, representative.representative_id desc
                """);
        query.setParameter("corporateId", corporateId);
        return query.getResultList()
                .stream()
                .map(row -> toRepresentative((Object[]) row))
                .toList();
    }

    @Override
    public List<AdminCorporateAgentResponse> findAgentsByCorporateId(Long corporateId) {
        Query query = entityManager().createNativeQuery("""
                select agent.agent_id,
                       agent.corporate_id,
                       agent.agent_name,
                       agent.agent_birth_date,
                       agent.agent_phone,
                       agent.agent_email,
                       agent.authority_scope,
                       agent.authority_status_code,
                       agent.identity_document_id,
                       identity_document.file_name as identity_document_name,
                       agent.delegation_document_id,
                       delegation_document.file_name as delegation_document_name,
                       agent.valid_from,
                       agent.valid_to,
                       agent.created_at,
                       agent.updated_at
                from corporate_agents agent
                left join corporate_documents identity_document
                       on identity_document.corporate_document_id = agent.identity_document_id
                left join corporate_documents delegation_document
                       on delegation_document.corporate_document_id = agent.delegation_document_id
                where agent.corporate_id = :corporateId
                order by agent.created_at desc, agent.agent_id desc
                """);
        query.setParameter("corporateId", corporateId);
        return query.getResultList()
                .stream()
                .map(row -> toAgent((Object[]) row))
                .toList();
    }

    @Override
    public List<AdminCorporateDocumentResponse> findDocumentsByCorporateId(Long corporateId) {
        Query query = entityManager().createNativeQuery("""
                select document.corporate_document_id,
                       document.corporate_id,
                       document.document_type_code,
                       coalesce(common_code.code_name, document.document_type_code) as document_type_name,
                       document.file_name,
                       null as file_path,
                       document.mime_type,
                       document.file_size,
                       document.document_hash,
                       document.upload_status_code,
                       document.uploaded_by_type_code,
                       document.uploaded_by_user_id,
                       coalesce(uploader.user_name, uploader.email) as uploaded_by_user_name,
                       document.uploaded_at,
                       document.created_at,
                       document.updated_at
                from corporate_documents document
                left join common_code_groups code_group
                       on code_group.code_group = 'DOCUMENT_TYPE'
                left join common_codes common_code
                       on common_code.code_group_id = code_group.code_group_id
                      and common_code.code = document.document_type_code
                left join users uploader
                       on uploader.user_id = document.uploaded_by_user_id
                where document.corporate_id = :corporateId
                order by document.uploaded_at desc, document.corporate_document_id desc
                """);
        query.setParameter("corporateId", corporateId);
        return query.getResultList()
                .stream()
                .map(row -> toDocument((Object[]) row))
                .toList();
    }

    private AdminCorporateRepresentativeResponse toRepresentative(Object[] row) {
        return new AdminCorporateRepresentativeResponse(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toLocalDate(row[3]),
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

    private AdminCorporateAgentResponse toAgent(Object[] row) {
        return new AdminCorporateAgentResponse(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toLocalDate(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toLong(row[8]),
                toString(row[9]),
                toLong(row[10]),
                toString(row[11]),
                toLocalDate(row[12]),
                toLocalDate(row[13]),
                toLocalDateTime(row[14]),
                toLocalDateTime(row[15])
        );
    }

    private AdminCorporateDocumentResponse toDocument(Object[] row) {
        return new AdminCorporateDocumentResponse(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toLong(row[7]),
                toString(row[8]),
                toString(row[9]),
                toString(row[10]),
                toLong(row[11]),
                toString(row[12]),
                toLocalDateTime(row[13]),
                toLocalDateTime(row[14]),
                toLocalDateTime(row[15])
        );
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return (LocalDate) value;
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
