package com.kyvc.backendadmin.domain.credential.repository;

import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialListResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialRequestHistoryResponse;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialSearchRequest;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialStatusHistoryResponse;
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
    public List<AdminCredentialListResponse.Item> search(AdminCredentialSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select credential.credential_id,
                       credential.credential_external_id,
                       credential.corporate_id,
                       corporate.corporate_name,
                       corporate.business_registration_no,
                       credential.credential_type_code,
                       credential.credential_status_code,
                       credential.issuer_did,
                       credential.xrpl_tx_hash,
                       credential.issued_at,
                       credential.expires_at,
                       credential.wallet_saved_yn
                from credentials credential
                join corporates corporate on corporate.corporate_id = credential.corporate_id
                %s
                order by credential.created_at desc, credential.credential_id desc
                """.formatted(queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toListItem((Object[]) row))
                .toList();
    }

    @Override
    public long count(AdminCredentialSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select count(*)
                from credentials credential
                join corporates corporate on corporate.corporate_id = credential.corporate_id
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
                       credential.credential_external_id,
                       credential.credential_type_code,
                       credential.credential_status_code,
                       credential.issuer_did,
                       credential.holder_did,
                       credential.holder_xrpl_address,
                       credential.xrpl_tx_hash,
                       credential.credential_status_id,
                       credential.wallet_saved_yn,
                       credential.wallet_saved_at,
                       credential.issued_at,
                       credential.expires_at,
                       credential.revoked_at,
                       credential.created_at,
                       credential.updated_at,
                       credential.corporate_id,
                       corporate.corporate_name,
                       corporate.business_registration_no,
                       corporate.corporate_phone,
                       credential.kyc_id,
                       kyc.kyc_status_code,
                       kyc.ai_review_status_code,
                       latest_core_request.core_request_id,
                       latest_core_request.core_request_status_code,
                       latest_core_request.error_message
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
                .map(row -> toDetail((Object[]) row, findRecentStatusHistories(credentialId)));
    }

    @Override
    public List<AdminCredentialRequestHistoryResponse> findRequestHistories(Long credentialId) {
        Query query = entityManager().createNativeQuery("""
                select credential_request_id,
                       credential_id,
                       request_type_code,
                       request_status_code,
                       reason_code,
                       reason,
                       requested_by_type_code,
                       requested_by_id,
                       requested_at,
                       completed_at,
                       core_request_id
                from credential_requests
                where credential_id = :credentialId
                order by requested_at desc, credential_request_id desc
                """);
        query.setParameter("credentialId", credentialId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toRequestHistory((Object[]) row))
                .toList();
    }

    @Override
    public List<AdminCredentialStatusHistoryResponse> findStatusHistories(Long credentialId) {
        Query query = entityManager().createNativeQuery("""
                select history_id,
                       credential_id,
                       before_status_code,
                       after_status_code,
                       reason_code,
                       reason,
                       changed_by_type_code,
                       changed_by_id,
                       changed_at
                from credential_status_histories
                where credential_id = :credentialId
                order by changed_at desc, history_id desc
                """);
        query.setParameter("credentialId", credentialId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(row -> toStatusHistory((Object[]) row))
                .toList();
    }

    @Override
    public boolean existsById(Long credentialId) {
        Number count = (Number) entityManager().createNativeQuery("""
                        select count(*)
                        from credentials
                        where credential_id = :credentialId
                        """)
                .setParameter("credentialId", credentialId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    private List<AdminCredentialStatusHistoryResponse> findRecentStatusHistories(Long credentialId) {
        return findStatusHistories(credentialId).stream()
                .limit(5)
                .toList();
    }

    private QueryParts buildQueryParts(AdminCredentialSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();
        if (StringUtils.hasText(request.status())) {
            where.append(" and credential.credential_status_code = :status");
            parameters.put("status", request.status());
        }
        if (StringUtils.hasText(request.corporateName())) {
            where.append(" and lower(corporate.corporate_name) like :corporateName");
            parameters.put("corporateName", "%" + request.corporateName().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.businessRegistrationNo())) {
            where.append(" and corporate.business_registration_no = :businessRegistrationNo");
            parameters.put("businessRegistrationNo", request.businessRegistrationNo());
        }
        if (StringUtils.hasText(request.issuerDid())) {
            where.append(" and lower(credential.issuer_did) like :issuerDid");
            parameters.put("issuerDid", "%" + request.issuerDid().toLowerCase() + "%");
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

    private AdminCredentialListResponse.Item toListItem(Object[] row) {
        // 민감정보 제외: credential_salt_hash, offer_token_hash, QR 토큰 원문은 조회하지 않는다.
        return new AdminCredentialListResponse.Item(
                toLong(row[0]),
                toString(row[1]),
                toLong(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toLocalDateTime(row[9]),
                toLocalDateTime(row[10]),
                toString(row[11])
        );
    }

    private AdminCredentialDetailResponse toDetail(
            Object[] row,
            List<AdminCredentialStatusHistoryResponse> recentStatusHistories
    ) {
        // 민감정보 제외: VC 원문, QR 토큰 원문, offer token hash, credential salt hash는 상세 응답에 포함하지 않는다.
        return new AdminCredentialDetailResponse(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toString(row[7]),
                toString(row[8]),
                toString(row[9]),
                toLocalDateTime(row[10]),
                toLocalDateTime(row[11]),
                toLocalDateTime(row[12]),
                toLocalDateTime(row[13]),
                toLocalDateTime(row[14]),
                toLocalDateTime(row[15]),
                toLong(row[16]),
                toString(row[17]),
                toString(row[18]),
                toString(row[19]),
                toLong(row[20]),
                toString(row[21]),
                toString(row[22]),
                toString(row[23]),
                toString(row[24]),
                toString(row[25]),
                recentStatusHistories
        );
    }

    private AdminCredentialRequestHistoryResponse toRequestHistory(Object[] row) {
        return new AdminCredentialRequestHistoryResponse(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toLong(row[7]),
                toLocalDateTime(row[8]),
                toLocalDateTime(row[9]),
                toString(row[10])
        );
    }

    private AdminCredentialStatusHistoryResponse toStatusHistory(Object[] row) {
        return new AdminCredentialStatusHistoryResponse(
                toLong(row[0]),
                toLong(row[1]),
                toString(row[2]),
                toString(row[3]),
                toString(row[4]),
                toString(row[5]),
                toString(row[6]),
                toLong(row[7]),
                toLocalDateTime(row[8])
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
        if (value == null) {
            return null;
        }
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
