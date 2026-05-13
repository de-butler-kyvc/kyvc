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
 * 다른 도메인 서비스에서 감사로그를 남길 때 사용하는 공통 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String SENSITIVE_KEYS = "coreRequestId|core_request_id|coreTrace|core_trace|rawPayload|raw_payload|vpJwt|vp_jwt|vcJson|vc_json|apiSecret|api_secret|password|token|authorization|cookie|jwt|secret|privateKey|private_key";

    private final AuditLogRepository auditLogRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    /**
     * 감사로그를 저장합니다.
     *
     * @param actorType 행위자 유형
     * @param actorId 행위자 ID
     * @param actionType 수행한 작업 유형
     * @param targetType 감사 대상 유형
     * @param targetId 감사 대상 ID
     * @param requestSummary 요청 또는 변경 내용 요약
     * @param beforeValue 변경 전 값
     * @param afterValue 변경 후 값
     * @return 저장된 감사로그 엔티티
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
                // target 정보 저장: 감사 대상 유형과 대상 ID를 별도 컬럼에 저장해 추적 가능하게 한다.
                targetType,
                targetId,
                summary,
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
        StringBuilder summary = new StringBuilder(StringUtils.hasText(requestSummary) ? maskSensitive(requestSummary) : "");
        append(summary, "result", resolveResult(afterValue));
        // requestId 저장: 별도 컬럼이 없어 request_summary에 추적 ID를 함께 남긴다.
        append(summary, "requestId", requestId);
        // beforeValue 저장: 별도 컬럼이 없어 request_summary에 변경 전 값을 함께 남긴다.
        append(summary, "beforeValue", maskSensitive(beforeValue));
        // afterValue 저장: 별도 컬럼이 없어 request_summary에 변경 후 값을 함께 남긴다.
        append(summary, "afterValue", maskSensitive(afterValue));
        append(summary, "userAgent", maskSensitive(userAgent));
        return summary.toString();
    }

    private String maskSensitive(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value
                .replaceAll("(?i)(" + SENSITIVE_KEYS + ")\"?\\s*[:=]\\s*\"?[^|,}\\s\"]+", "$1=***")
                .replaceAll("(?i)(" + SENSITIVE_KEYS + ")=[^|,}\\s]+", "$1=***");
    }

    private String resolveResult(String afterValue) {
        return "FAILURE".equalsIgnoreCase(afterValue) ? "FAILURE" : "SUCCESS";
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
            // 요청 IP 저장: 프록시 환경에서는 X-Forwarded-For의 첫 번째 IP를 우선 저장한다.
            return forwardedFor.split(",")[0].trim();
        }
        // 요청 IP 저장: 프록시 헤더가 없으면 servlet remoteAddr 값을 저장한다.
        return request.getRemoteAddr();
    }
}
