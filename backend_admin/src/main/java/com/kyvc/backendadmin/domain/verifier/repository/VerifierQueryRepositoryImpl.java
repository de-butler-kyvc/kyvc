package com.kyvc.backendadmin.domain.verifier.repository;

import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link VerifierQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class VerifierQueryRepositoryImpl implements VerifierQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminVerifierDtos.Response> search(int page, int size, String status, String keyword) {
        QueryParts parts = verifierWhere(status, keyword);
        Query query = entityManager().createNativeQuery("""
                select verifier_id, verifier_name, verifier_status_code, contact_email,
                       approved_at, suspended_at, created_at, updated_at
                from verifiers
                %s
                order by created_at desc, verifier_id desc
                """.formatted(parts.where()));
        bind(query, parts.parameters());
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream().map(row -> toVerifier((Object[]) row)).toList();
    }

    @Override
    public long count(String status, String keyword) {
        QueryParts parts = verifierWhere(status, keyword);
        Query query = entityManager().createNativeQuery("select count(*) from verifiers " + parts.where());
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<AdminVerifierDtos.ApiKeyResponse> findApiKeys(Long verifierId) {
        List<?> rows = entityManager().createNativeQuery("""
                        select api_key_id, key_name, api_key_prefix, key_status_code,
                               issued_at, expires_at, last_used_at, issued_at
                        from verifier_api_keys
                        where verifier_id = :verifierId
                        order by issued_at desc, api_key_id desc
                        """)
                .setParameter("verifierId", verifierId)
                .getResultList();
        return rows.stream().map(row -> toApiKey((Object[]) row)).toList();
    }

    @Override
    public List<AdminVerifierDtos.CallbackResponse> findCallbacks(Long verifierId) {
        List<?> rows = entityManager().createNativeQuery("""
                        select callback_id, verifier_id, callback_url, callback_status_code,
                               enabled_yn, created_at, updated_at
                        from verifier_callbacks
                        where verifier_id = :verifierId
                        order by created_at desc, callback_id desc
                        """)
                .setParameter("verifierId", verifierId)
                .getResultList();
        return rows.stream().map(row -> toCallback((Object[]) row)).toList();
    }

    @Override
    public List<AdminVerifierDtos.LogResponse> searchLogs(AdminVerifierDtos.LogSearchRequest request) {
        QueryParts parts = logWhere(request);
        Query query = entityManager().createNativeQuery("""
                select verifier_log_id, verifier_id, api_key_id, action_type_code, request_path, method,
                       status_code, result_code, latency_ms, client_sdk_version, policy_version, error_message, requested_at
                from verifier_logs
                %s
                order by requested_at desc, verifier_log_id desc
                """.formatted(parts.where()));
        bind(query, parts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        return query.getResultList().stream().map(row -> toLog((Object[]) row)).toList();
    }

    @Override
    public long countLogs(AdminVerifierDtos.LogSearchRequest request) {
        QueryParts parts = logWhere(request);
        Query query = entityManager().createNativeQuery("select count(*) from verifier_logs " + parts.where());
        bind(query, parts.parameters());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public AdminVerifierDtos.UsageStatsResponse usageStats(Long verifierId, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> params = new HashMap<>();
        String range = dateRange("requested_at", fromDate, toDate, params);
        Number vpCount = singleNumber("""
                select count(*) from vp_verifications
                where verifier_id = :verifierId %s
                """.formatted(range), params, verifierId);
        Number success = singleNumber("""
                select count(*) from vp_verifications
                where verifier_id = :verifierId and vp_verification_status_code = 'VALID' %s
                """.formatted(range), params, verifierId);
        Number failed = singleNumber("""
                select count(*) from vp_verifications
                where verifier_id = :verifierId and vp_verification_status_code in ('INVALID', 'FAILED', 'EXPIRED') %s
                """.formatted(range), params, verifierId);
        Number callbackSuccess = singleNumber("""
                select count(*) from vp_verifications
                where verifier_id = :verifierId and callback_status_code in ('SUCCESS', 'SENT') %s
                """.formatted(range), params, verifierId);
        Number callbackFailed = singleNumber("""
                select count(*) from vp_verifications
                where verifier_id = :verifierId and callback_status_code in ('FAILED', 'ERROR') %s
                """.formatted(range), params, verifierId);
        Map<String, Object> keyParams = new HashMap<>();
        Number keyUsage = singleNumber("""
                select count(*) from verifier_logs
                where verifier_id = :verifierId and api_key_id is not null %s
                """.formatted(dateRange("requested_at", fromDate, toDate, keyParams)), keyParams, verifierId);
        Query dailyQuery = entityManager().createNativeQuery("""
                select requested_at::date,
                       count(*),
                       count(*) filter (where vp_verification_status_code = 'VALID'),
                       count(*) filter (where vp_verification_status_code in ('INVALID', 'FAILED', 'EXPIRED'))
                from vp_verifications
                where verifier_id = :verifierId %s
                group by requested_at::date
                order by requested_at::date desc
                """.formatted(range));
        dailyQuery.setParameter("verifierId", verifierId);
        bind(dailyQuery, params);
        List<AdminVerifierDtos.DailyUsage> daily = dailyQuery.getResultList().stream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new AdminVerifierDtos.DailyUsage(toLocalDate(values[0]), toLong(values[1]), toLong(values[2]), toLong(values[3]));
                }).toList();
        return new AdminVerifierDtos.UsageStatsResponse(vpCount.longValue(), success.longValue(), failed.longValue(),
                callbackSuccess.longValue(), callbackFailed.longValue(), keyUsage.longValue(), daily);
    }

    private Number singleNumber(String sql, Map<String, Object> params, Long verifierId) {
        Query query = entityManager().createNativeQuery(sql);
        query.setParameter("verifierId", verifierId);
        bind(query, params);
        return (Number) query.getSingleResult();
    }

    private QueryParts verifierWhere(String status, String keyword) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(status)) {
            where.append(" and verifier_status_code = :status");
            params.put("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" and (lower(verifier_name) like :keyword or lower(coalesce(contact_email, '')) like :keyword)");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }
        return new QueryParts(where.toString(), params);
    }

    private QueryParts logWhere(AdminVerifierDtos.LogSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (request.verifierId() != null) {
            where.append(" and verifier_id = :verifierId");
            params.put("verifierId", request.verifierId());
        }
        if (StringUtils.hasText(request.actionTypeCode())) {
            where.append(" and action_type_code = :actionTypeCode");
            params.put("actionTypeCode", request.actionTypeCode());
        }
        if (request.statusCode() != null) {
            where.append(" and status_code = :statusCode");
            params.put("statusCode", request.statusCode());
        }
        where.append(dateRange("requested_at", request.fromDate(), request.toDate(), params));
        return new QueryParts(where.toString(), params);
    }

    private String dateRange(String column, LocalDate fromDate, LocalDate toDate, Map<String, Object> params) {
        StringBuilder condition = new StringBuilder();
        if (fromDate != null) {
            condition.append(" and ").append(column).append(" >= :fromDate");
            params.put("fromDate", fromDate.atStartOfDay());
        }
        if (toDate != null) {
            condition.append(" and ").append(column).append(" < :toDateExclusive");
            params.put("toDateExclusive", toDate.plusDays(1).atStartOfDay());
        }
        return condition.toString();
    }

    private AdminVerifierDtos.Response toVerifier(Object[] row) {
        return new AdminVerifierDtos.Response(toLong(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
                toLocalDateTime(row[4]), toLocalDateTime(row[5]), toLocalDateTime(row[6]), toLocalDateTime(row[7]));
    }

    private AdminVerifierDtos.ApiKeyResponse toApiKey(Object[] row) {
        return new AdminVerifierDtos.ApiKeyResponse(toLong(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
                toLocalDateTime(row[4]), toLocalDateTime(row[5]), toLocalDateTime(row[6]), toLocalDateTime(row[7]));
    }

    private AdminVerifierDtos.CallbackResponse toCallback(Object[] row) {
        return new AdminVerifierDtos.CallbackResponse(toLong(row[0]), toLong(row[1]), toString(row[2]), toString(row[3]),
                toString(row[4]), toLocalDateTime(row[5]), toLocalDateTime(row[6]));
    }

    private AdminVerifierDtos.LogResponse toLog(Object[] row) {
        return new AdminVerifierDtos.LogResponse(toLong(row[0]), toLong(row[1]), toLong(row[2]), toString(row[3]), toString(row[4]),
                toString(row[5]), row[6] == null ? null : ((Number) row[6]).intValue(), toString(row[7]),
                row[8] == null ? null : ((Number) row[8]).intValue(), toString(row[9]), toString(row[10]), toString(row[11]),
                toLocalDateTime(row[12]));
    }

    private void bind(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private Long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime();
        return (LocalDateTime) value;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) return date.toLocalDate();
        return (LocalDate) value;
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }

    private record QueryParts(String where, Map<String, Object> parameters) {
    }
}
