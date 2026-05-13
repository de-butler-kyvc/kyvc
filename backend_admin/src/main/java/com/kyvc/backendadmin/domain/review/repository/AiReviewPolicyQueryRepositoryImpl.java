package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicyResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySearchRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewPolicySummaryResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link AiReviewPolicyQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class AiReviewPolicyQueryRepositoryImpl implements AiReviewPolicyQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AiReviewPolicySummaryResponse.Item> search(AiReviewPolicySearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select policy.ai_policy_id,
                       policy.policy_name,
                       policy.corporate_type_code,
                       policy.auto_approve_enabled_yn,
                       policy.auto_approve_min_score,
                       policy.manual_review_below_score,
                       policy.supplement_below_score,
                       policy.enabled_yn,
                       case when policy.enabled_yn = 'Y' then 'ACTIVE' else 'INACTIVE' end as status,
                       policy.updated_at
                from ai_review_policies policy
                %s
                order by policy.updated_at desc, policy.ai_policy_id desc
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
    public long count(AiReviewPolicySearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        Query query = entityManager().createNativeQuery("""
                select count(*)
                from ai_review_policies policy
                %s
                """.formatted(queryParts.whereClause()));
        bindParameters(query, queryParts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public Optional<AiReviewPolicyResponse> findDetailById(Long aiPolicyId) {
        Query query = entityManager().createNativeQuery("""
                select policy.ai_policy_id,
                       policy.policy_name,
                       policy.corporate_type_code,
                       policy.auto_approve_enabled_yn,
                       policy.auto_approve_min_score,
                       policy.manual_review_below_score,
                       policy.supplement_below_score,
                       policy.enabled_yn,
                       case when policy.enabled_yn = 'Y' then 'ACTIVE' else 'INACTIVE' end as status,
                       null as description,
                       policy.effective_from,
                       policy.effective_to,
                       policy.created_by_admin_id,
                       policy.updated_by_admin_id,
                       policy.created_at,
                       policy.updated_at
                from ai_review_policies policy
                where policy.ai_policy_id = :aiPolicyId
                """);
        query.setParameter("aiPolicyId", aiPolicyId);
        List<?> rows = query.getResultList();
        return rows.stream()
                .findFirst()
                .map(row -> toResponse((Object[]) row));
    }

    private QueryParts buildQueryParts(AiReviewPolicySearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();

        if (StringUtils.hasText(request.keyword())) {
            where.append(" and lower(policy.policy_name) like :keyword");
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.corporateType())) {
            where.append(" and policy.corporate_type_code = :corporateType");
            parameters.put("corporateType", request.corporateType());
        }
        if (StringUtils.hasText(request.enabledYn())) {
            where.append(" and policy.enabled_yn = :enabledYn");
            parameters.put("enabledYn", request.enabledYn());
        }
        if (StringUtils.hasText(request.status())) {
            where.append(" and case when policy.enabled_yn = 'Y' then 'ACTIVE' else 'INACTIVE' end = :status");
            parameters.put("status", request.status());
        }

        return new QueryParts(where.toString(), parameters);
    }

    private AiReviewPolicySummaryResponse.Item toSummaryItem(Object[] row) {
        return new AiReviewPolicySummaryResponse.Item(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toString(row[3]),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                toString(row[7]),
                toString(row[8]),
                toLocalDateTime(row[9])
        );
    }

    private AiReviewPolicyResponse toResponse(Object[] row) {
        return new AiReviewPolicyResponse(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toString(row[3]),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                toString(row[7]),
                toString(row[8]),
                toString(row[9]),
                toLocalDateTime(row[10]),
                toLocalDateTime(row[11]),
                toLong(row[12]),
                toLong(row[13]),
                toLocalDateTime(row[14]),
                toLocalDateTime(row[15])
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

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? null : (BigDecimal) value;
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

    private record QueryParts(
            String whereClause,
            Map<String, Object> parameters
    ) {
    }
}
