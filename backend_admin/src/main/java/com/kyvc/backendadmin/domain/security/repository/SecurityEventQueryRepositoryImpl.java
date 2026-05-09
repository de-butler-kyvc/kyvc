package com.kyvc.backendadmin.domain.security.repository;

import com.kyvc.backendadmin.domain.security.dto.AdminSecurityDtos;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link SecurityEventQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class SecurityEventQueryRepositoryImpl implements SecurityEventQueryRepository {
    private static final String SECURITY_ACTIONS = "('LOGIN_FAILED','MFA_FAILED','PERMISSION_DENIED','ADMIN_ROLE_CHANGED','API_KEY_REVOKED','VERIFIER_SUSPENDED','SENSITIVE_DATA_ACCESSED','VERIFIER_API_KEY_REVOKED','VERIFIER_SUSPENDED')";
    private static final String SENSITIVE_KEYS = "coreRequestId|core_request_id|coreTrace|core_trace|rawPayload|raw_payload|vpJwt|vp_jwt|vcJson|vc_json|apiSecret|api_secret|password_hash|password|token_hash|token|api_key_hash|credential_salt_hash|credential_salt|authorization|cookie|jwt|secret|privateKey|private_key";
    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminSecurityDtos.EventResponse> search(boolean dataAccess, int page, int size) {
        Query query = em().createNativeQuery("""
                select audit_log_id, action_type, actor_id, audit_target_type_code, target_id, request_summary, ip_address, created_at
                from audit_logs
                where %s
                order by created_at desc, audit_log_id desc
                """.formatted(condition(dataAccess)));
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        return query.getResultList().stream().map(row -> toEvent((Object[]) row)).toList();
    }

    @Override
    public long count(boolean dataAccess) {
        return ((Number) em().createNativeQuery("select count(*) from audit_logs where " + condition(dataAccess)).getSingleResult()).longValue();
    }

    private String condition(boolean dataAccess) {
        if (dataAccess) {
            return "action_type in ('SENSITIVE_DATA_ACCESSED','AUDIT_LOG_DETAIL_VIEWED','CREDENTIAL_DETAIL_VIEWED','VP_VERIFICATION_DETAIL_VIEWED') or lower(coalesce(request_summary,'')) like '%sensitive%'";
        }
        return "action_type in " + SECURITY_ACTIONS;
    }

    private AdminSecurityDtos.EventResponse toEvent(Object[] row) {
        String summary = str(row[5]);
        return new AdminSecurityDtos.EventResponse(l(row[0]), str(row[1]), l(row[2]), str(row[3]), l(row[4]), mask(summary), str(row[6]), trace(summary), dt(row[7]));
    }
    private String mask(String value) {
        return value == null ? null : value
                .replaceAll("(?i)(" + SENSITIVE_KEYS + ")\"?\\s*[:=]\\s*\"?[^|,}\\s\"]+", "$1=***")
                .replaceAll("(?i)(" + SENSITIVE_KEYS + ")=[^|,}\\s]+", "$1=***");
    }
    private String trace(String summary) {
        if (summary == null) return null;
        for (String part : summary.split("\\|")) {
            String t = part.trim();
            if (t.startsWith("requestId=")) return t.substring("requestId=".length());
        }
        return null;
    }
    private Long l(Object v) { return v == null ? null : ((Number) v).longValue(); }
    private String str(Object v) { return v == null ? null : v.toString(); }
    private LocalDateTime dt(Object v) { return v instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) v; }
    private EntityManager em() { return entityManagerProvider.getObject(); }
}
