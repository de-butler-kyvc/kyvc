package com.kyvc.backendadmin.domain.credential.repository;

import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSummaryResponse;
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
import java.util.Optional;

/**
 * {@link CredentialQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class CredentialQueryRepositoryImpl implements CredentialQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminCredentialSummaryResponse.Item> search(AdminCredentialSummaryResponse.SearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                with latest_core_request as (
                    select *
                    from (
                        select core_request.*,
                               row_number() over (
                                   partition by core_request.target_id
                                   order by coalesce(core_request.requested_at, core_request.created_at) desc,
                                            core_request.core_request_id desc
                               ) as rn
                        from core_requests core_request
                        where core_request.core_target_type_code = 'CREDENTIAL'
                          and core_request.core_request_type_code = 'VC_ISSUE'
                    ) ranked_core_request
                    where ranked_core_request.rn = 1
                )
                select credential.credential_id,
                       credential.kyc_id,
                       credential.corporate_id,
                       corporate.corporate_name,
                       corporate.business_registration_no,
                       credential.credential_type_code,
                       credential.credential_status_code,
                       latest_core_request.core_request_status_code,
                       credential.xrpl_tx_hash,
                       credential.issued_at,
                       credential.created_at
                from credentials credential
                join corporates corporate on corporate.corporate_id = credential.corporate_id
                left join latest_core_request on latest_core_request.target_id = credential.credential_id
                %s
                order by credential.created_at desc, credential.credential_id desc
                """.formatted(queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toSummaryItem((Object[]) row))
                .toList();
    }

    @Override
    public long count(AdminCredentialSummaryResponse.SearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                with latest_core_request as (
                    select *
                    from (
                        select core_request.*,
                               row_number() over (
                                   partition by core_request.target_id
                                   order by coalesce(core_request.requested_at, core_request.created_at) desc,
                                            core_request.core_request_id desc
                               ) as rn
                        from core_requests core_request
                        where core_request.core_target_type_code = 'CREDENTIAL'
                          and core_request.core_request_type_code = 'VC_ISSUE'
                    ) ranked_core_request
                    where ranked_core_request.rn = 1
                )
                select count(*)
                from credentials credential
                join corporates corporate on corporate.corporate_id = credential.corporate_id
                left join latest_core_request on latest_core_request.target_id = credential.credential_id
                %s
                """.formatted(queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<AdminCredentialDetailResponse> findDetailById(Long credentialId) {
        Query query = entityManager().createNativeQuery("""
                with latest_core_request as (
                    select *
                    from (
                        select core_request.*,
                               row_number() over (
                                   partition by core_request.target_id
                                   order by coalesce(core_request.requested_at, core_request.created_at) desc,
                                            core_request.core_request_id desc
                               ) as rn
                        from core_requests core_request
                        where core_request.core_target_type_code = 'CREDENTIAL'
                          and core_request.core_request_type_code = 'VC_ISSUE'
                    ) ranked_core_request
                    where ranked_core_request.rn = 1
                )
                select credential.credential_id,
                       credential.kyc_id,
                       kyc.kyc_status_code,
                       credential.corporate_id,
                       corporate.corporate_name,
                       corporate.business_registration_no,
                       credential.credential_type_code,
                       credential.credential_status_code,
                       latest_core_request.core_request_id,
                       latest_core_request.core_request_status_code,
                       latest_core_request.error_message,
                       credential.xrpl_tx_hash,
                       credential.issued_at,
                       credential.expires_at,
                       credential.created_at,
                       credential.updated_at
                from credentials credential
                join kyc_applications kyc on kyc.kyc_id = credential.kyc_id
                join corporates corporate on corporate.corporate_id = credential.corporate_id
                left join latest_core_request on latest_core_request.target_id = credential.credential_id
                where credential.credential_id = :credentialId
                """);
        query.setParameter("credentialId", credentialId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(row -> toDetail((Object[]) row));
    }

    private QueryParts buildQueryParts(AdminCredentialSummaryResponse.SearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();
        if (StringUtils.hasText(request.keyword())) {
            where.append(" and (lower(corporate.corporate_name) like :keyword or lower(corporate.business_registration_no) like :keyword)");
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.credentialStatus())) {
            where.append(" and credential.credential_status_code = :credentialStatus");
            parameters.put("credentialStatus", request.credentialStatus());
        }
        if (StringUtils.hasText(request.coreRequestStatus())) {
            where.append(" and latest_core_request.core_request_status_code = :coreRequestStatus");
            parameters.put("coreRequestStatus", request.coreRequestStatus());
        }
        if (StringUtils.hasText(request.corporateName())) {
            where.append(" and lower(corporate.corporate_name) like :corporateName");
            parameters.put("corporateName", "%" + request.corporateName().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.businessRegistrationNumber())) {
            where.append(" and corporate.business_registration_no = :businessRegistrationNumber");
            parameters.put("businessRegistrationNumber", request.businessRegistrationNumber());
        }
        if (request.fromDate() != null) {
            where.append(" and credential.created_at >= :fromDate");
            parameters.put("fromDate", request.fromDate().atStartOfDay());
        }
        if (request.toDate() != null) {
            where.append(" and credential.created_at < :toDateExclusive");
            parameters.put("toDateExclusive", request.toDate().plusDays(1).atStartOfDay());
        }
        return new QueryParts(where.toString(), parameters);
    }

    private AdminCredentialSummaryResponse.Item toSummaryItem(Object[] row) {
        // 민감정보 응답 제외: VC 원문, QR 토큰, holder DID, signing key 관련 값은 조회하지 않는다.
        return new AdminCredentialSummaryResponse.Item(
                toLong(row[0]), toLong(row[1]), toLong(row[2]), toString(row[3]), toString(row[4]),
                toString(row[5]), toString(row[6]), toString(row[7]), toString(row[8]),
                toLocalDateTime(row[9]), toLocalDateTime(row[10])
        );
    }

    private AdminCredentialDetailResponse toDetail(Object[] row) {
        // 민감정보 응답 제외: VC 원본, private key, signing key, qr_token은 상세 응답에 포함하지 않는다.
        return new AdminCredentialDetailResponse(
                toLong(row[0]), toLong(row[1]), toString(row[2]), toLong(row[3]), toString(row[4]),
                toString(row[5]), toString(row[6]), toString(row[7]), toString(row[8]), toString(row[9]),
                toString(row[10]), toString(row[11]), toLocalDateTime(row[12]), toLocalDateTime(row[13]),
                toLocalDateTime(row[14]), toLocalDateTime(row[15])
        );
    }

    private void bindParameters(Query query, Map<String, Object> parameters) {
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
