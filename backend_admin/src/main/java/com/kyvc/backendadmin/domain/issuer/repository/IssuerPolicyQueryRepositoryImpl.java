package com.kyvc.backendadmin.domain.issuer.repository;

import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicyResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerPolicySummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/** {@link IssuerPolicyQueryRepository}의 EntityManager 기반 구현체입니다. */
@Repository
@RequiredArgsConstructor
public class IssuerPolicyQueryRepositoryImpl implements IssuerPolicyQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<IssuerPolicyResponse> search(IssuerPolicySummaryResponse.SearchRequest request) {
        QueryParts parts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select issuer_policy_id, issuer_did, issuer_name, issuer_policy_type_code,
                       credential_type_code, issuer_policy_status_code, reason, created_at, updated_at
                from issuer_policies
                %s
                order by updated_at desc, issuer_policy_id desc
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        return query.getResultList().stream().map(row -> toResponse((Object[]) row)).toList();
    }

    @Override
    public long count(IssuerPolicySummaryResponse.SearchRequest request) {
        QueryParts parts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("select count(*) from issuer_policies %s".formatted(parts.whereClause()));
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<IssuerPolicyResponse> findDetailById(Long policyId) {
        Query query = entityManager().createNativeQuery("""
                select issuer_policy_id, issuer_did, issuer_name, issuer_policy_type_code,
                       credential_type_code, issuer_policy_status_code, reason, created_at, updated_at
                from issuer_policies
                where issuer_policy_id = :policyId
                """);
        query.setParameter("policyId", policyId);
        return query.getResultList().stream().findFirst().map(row -> toResponse((Object[]) row));
    }

    private QueryParts buildQueryParts(IssuerPolicySummaryResponse.SearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(request.keyword())) {
            where.append(" and (lower(issuer_did) like :keyword or lower(issuer_name) like :keyword)");
            params.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.policyType())) {
            where.append(" and issuer_policy_type_code = :policyType");
            params.put("policyType", request.policyType());
        }
        if (StringUtils.hasText(request.status())) {
            where.append(" and issuer_policy_status_code = :status");
            params.put("status", request.status());
        }
        if (StringUtils.hasText(request.issuerDid())) {
            where.append(" and issuer_did = :issuerDid");
            params.put("issuerDid", request.issuerDid());
        }
        if (StringUtils.hasText(request.issuerName())) {
            where.append(" and lower(issuer_name) like :issuerName");
            params.put("issuerName", "%" + request.issuerName().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.credentialType())) {
            where.append(" and credential_type_code like :credentialType");
            params.put("credentialType", "%" + request.credentialType() + "%");
        }
        return new QueryParts(where.toString(), params);
    }

    private IssuerPolicyResponse toResponse(Object[] row) {
        return new IssuerPolicyResponse(
                toLong(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
                splitCredentialTypes(toString(row[4])), toString(row[5]), toString(row[6]),
                toLocalDateTime(row[7]), toLocalDateTime(row[8])
        );
    }

    private List<String> splitCredentialTypes(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    private void bind(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private Long toLong(Object value) { return value == null ? null : ((Number) value).longValue(); }
    private String toString(Object value) { return value == null ? null : value.toString(); }
    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return (LocalDateTime) value;
    }
    private EntityManager entityManager() { return entityManagerProvider.getObject(); }
    private record QueryParts(String whereClause, Map<String, Object> parameters) {}
}
