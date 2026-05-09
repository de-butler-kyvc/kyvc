package com.kyvc.backendadmin.domain.audit.application;

import com.kyvc.backendadmin.domain.audit.domain.AuditLog;
import com.kyvc.backendadmin.domain.audit.repository.AuditLogRepository;
import com.kyvc.backendadmin.global.filter.RequestIdFilter;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Shared component for writing audit logs from domain services.
 */
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";

    private final AuditLogRepository auditLogRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    /**
     * Writes an audit log.
     *
     * @param actorType actor type
     * @param actorId actor ID
     * @param actionType action type
     * @param targetType audit target type
     * @param targetId audit target ID
     * @param requestSummary request or change summary
     * @param beforeValue value before the change
     * @param afterValue value after the change
     * @return saved audit log entity
     */
    @Transactional
    public AuditLog write(
            KyvcEnums.ActorType actorType,
            Long actorId,
            String actionType,
            KyvcEnums.AuditTargetType targetType,
            Long targetId,
            String requestSummary,
            String beforeValue,
            String afterValue
    ) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        String requestId = resolveRequestId(request);
        String ipAddress = resolveIpAddress(request);
        String userAgent = request == null ? null : request.getHeader(USER_AGENT_HEADER);
        String summary = buildSummary(requestSummary, requestId, userAgent, beforeValue, afterValue);

        return auditLogRepository.save(AuditLog.create(
                actorType,
                actorId,
                actionType,
                targetType,
                targetId,
                summary,
                beforeValue,
                afterValue,
                ipAddress
        ));
    }

    private String buildSummary(
            String requestSummary,
            String requestId,
            String userAgent,
            String beforeValue,
            String afterValue
    ) {
        StringBuilder summary = new StringBuilder(StringUtils.hasText(requestSummary) ? requestSummary : "");
        append(summary, "requestId", requestId);
        append(summary, "beforeValue", beforeValue);
        append(summary, "afterValue", afterValue);
        append(summary, "userAgent", userAgent);
        return summary.toString();
    }

    private void append(StringBuilder summary, String key, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!summary.isEmpty()) {
            summary.append(" | ");
        }
        summary.append(key).append("=").append(value);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request == null ? null : request.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId)) {
            return requestId;
        }
        return MDC.get(RequestIdFilter.MDC_KEY);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
