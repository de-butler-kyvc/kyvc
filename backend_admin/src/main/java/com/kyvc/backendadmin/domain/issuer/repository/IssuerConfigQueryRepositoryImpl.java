package com.kyvc.backendadmin.domain.issuer.repository;

import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigDetailResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigSummaryResponse;
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

/** {@link IssuerConfigQueryRepository}의 EntityManager 기반 구현체입니다. */
@Repository
@RequiredArgsConstructor
public class IssuerConfigQueryRepositoryImpl implements IssuerConfigQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<IssuerConfigSummaryResponse.Item> search(IssuerConfigSummaryResponse.SearchRequest request) {
        QueryParts parts = buildQueryParts(request);
        // Backend Admin 화면에서는 Issuer 식별 정보만 제공하고 signing key, private key 등 기술 민감정보는 응답하지 않는다.
        Query query = entityManager().createNativeQuery("""
                select issuer_config_id, issuer_did, issuer_name, issuer_type_code,
                       credential_type_code, default_yn, issuer_config_status_code,
                       created_at, updated_at
                from issuer_configs
                %s
                order by updated_at desc, issuer_config_id desc
                """.formatted(parts.whereClause()));
        bind(query, parts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        return query.getResultList().stream().map(row -> toSummary((Object[]) row)).toList();
    }

    @Override
    public long count(IssuerConfigSummaryResponse.SearchRequest request) {
        QueryParts parts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("select count(*) from issuer_configs %s".formatted(parts.whereClause()));
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<IssuerConfigDetailResponse> findDetailById(Long issuerConfigId) {
        // 민감 기술 정보 제외: signing_key_ref, verification_method_id, credential_schema_id, issuer_xrpl_address는 select하지 않는다.
        Query query = entityManager().createNativeQuery("""
                select issuer_config_id, issuer_did, issuer_name, issuer_type_code,
                       credential_type_code, default_yn, issuer_config_status_code,
                       created_at, updated_at
                from issuer_configs
                where issuer_config_id = :issuerConfigId
                """);
        query.setParameter("issuerConfigId", issuerConfigId);
        return query.getResultList().stream().findFirst().map(row -> toDetail((Object[]) row));
    }

    private QueryParts buildQueryParts(IssuerConfigSummaryResponse.SearchRequest request) {
        // 목록 검색 조건 처리: null이 아닌 조건만 where 절과 바인딩 파라미터에 추가한다.
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(request.keyword())) {
            where.append(" and (lower(issuer_did) like :keyword or lower(issuer_name) like :keyword)");
            params.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.status())) {
            where.append(" and issuer_config_status_code = :status");
            params.put("status", request.status());
        }
        if (StringUtils.hasText(request.issuerType())) {
            where.append(" and issuer_type_code = :issuerType");
            params.put("issuerType", request.issuerType());
        }
        if (StringUtils.hasText(request.credentialType())) {
            where.append(" and credential_type_code = :credentialType");
            params.put("credentialType", request.credentialType());
        }
        return new QueryParts(where.toString(), params);
    }

    private IssuerConfigSummaryResponse.Item toSummary(Object[] row) {
        return new IssuerConfigSummaryResponse.Item(
                toLong(row[0]), toString(row[1]), toString(row[2]), toString(row[3]), toString(row[4]),
                toString(row[5]), toString(row[6]), toLocalDateTime(row[7]), toLocalDateTime(row[8])
        );
    }

    private IssuerConfigDetailResponse toDetail(Object[] row) {
        return new IssuerConfigDetailResponse(
                toLong(row[0]), toString(row[1]), toString(row[2]), toString(row[3]), toString(row[4]),
                toString(row[5]), toString(row[6]), toLocalDateTime(row[7]), toLocalDateTime(row[8])
        );
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
