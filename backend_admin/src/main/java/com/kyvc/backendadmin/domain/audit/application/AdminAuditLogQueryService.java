package com.kyvc.backendadmin.domain.audit.application;

import com.kyvc.backendadmin.domain.audit.domain.AuditLog;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogListResponse;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogPageResponse;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogResponse;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogSearchRequest;
import com.kyvc.backendadmin.domain.audit.repository.AuditLogQueryRepository;
import com.kyvc.backendadmin.domain.audit.repository.AuditLogRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 감사로그 목록/상세 조회 유스케이스를 담당하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogQueryRepository auditLogQueryRepository;

    /**
     * 감사로그 목록을 검색 조건으로 조회합니다.
     *
     * <p>QueryRepository를 통해 actorType, actorId, actionType, targetType, targetId,
     * from, to 조건을 적용해 목록과 전체 건수를 조회합니다. 조회 자체도 백엔드 관리자 감사 대상 흐름으로
     * application.log에 info 로그를 남기고, data.items와 data.page 구조의 응답 DTO를 반환합니다.</p>
     *
     * @param request 감사로그 목록 검색 조건
     * @return 감사로그 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminAuditLogListResponse search(AdminAuditLogSearchRequest request) {
        log.info("admin audit log list requested. page={} size={} actorType={} actorId={} actionType={} targetType={} targetId={} from={} to={}",
                request.page(), request.size(), request.actorType(), request.actorId(), request.actionType(),
                request.targetType(), request.targetId(), request.from(), request.to());
        List<AdminAuditLogResponse> items = auditLogQueryRepository.search(request)
                .stream()
                .map(this::toResponse)
                .toList();
        long totalElements = auditLogQueryRepository.count(request);
        return new AdminAuditLogListResponse(
                items,
                new AdminAuditLogPageResponse(
                        request.page(),
                        request.size(),
                        totalElements,
                        totalPages(totalElements, request.size())
                )
        );
    }

    /**
     * 감사로그 상세를 감사로그 ID 기준으로 조회합니다.
     *
     * <p>AuditLogRepository로 audit_logs.audit_log_id 기준 단건을 조회하고, 존재하지 않으면
     * AUDIT_LOG_NOT_FOUND 예외를 발생시킵니다. 상세 조회 요청도 application.log에 info 로그로 남긴 뒤
     * 감사로그 상세 DTO를 반환합니다.</p>
     *
     * @param auditId 감사로그 ID
     * @return 감사로그 상세 응답
     */
    @Transactional(readOnly = true)
    public AdminAuditLogResponse get(Long auditId) {
        log.info("admin audit log detail requested. auditId={}", auditId);
        return auditLogRepository.findById(auditId)
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.AUDIT_LOG_NOT_FOUND));
    }

    private AdminAuditLogResponse toResponse(AuditLog auditLog) {
        return new AdminAuditLogResponse(
                auditLog.getAuditLogId(),
                auditLog.getActorType().name(),
                auditLog.getActorId(),
                null,
                auditLog.getActionType(),
                auditLog.getTargetType().name(),
                auditLog.getTargetId(),
                auditLog.getRequestSummary(),
                maskSensitive(auditLog.getBeforeValueJson()),
                maskSensitive(auditLog.getAfterValueJson()),
                extractTraceId(auditLog.getRequestSummary()),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt()
        );
    }

    private String maskSensitive(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("(?i)(password_hash|token_hash|api_key_hash|credential_salt_hash|credential_salt|secret|vp_jwt_hash|vp_jwt)\"?\\s*[:=]\\s*\"?[^\"]+", "$1=***")
                .replaceAll("(?i)(password_hash|token_hash|api_key_hash|credential_salt_hash|credential_salt|secret|vp_jwt_hash|vp_jwt)=[^|,}\\s]+", "$1=***");
    }

    private String extractTraceId(String summary) {
        if (summary == null) {
            return null;
        }
        for (String part : summary.split("\\|")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("requestId=")) {
                return trimmed.substring("requestId=".length());
            }
        }
        return null;
    }

    private int totalPages(long totalElements, int size) {
        return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
