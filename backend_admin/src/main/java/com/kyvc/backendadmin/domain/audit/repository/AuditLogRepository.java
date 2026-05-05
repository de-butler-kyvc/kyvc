package com.kyvc.backendadmin.domain.audit.repository;

import com.kyvc.backendadmin.domain.audit.domain.AuditLog;

import java.util.Optional;

/**
 * 감사로그 저장과 단건 조회 책임을 가지는 Repository입니다.
 */
public interface AuditLogRepository {

    /**
     * actorType, actorId, actionType, targetType, targetId, 기간 조건 검색과 무관하게 감사로그를 저장합니다.
     *
     * @param auditLog 저장할 감사로그 엔티티
     * @return 저장된 감사로그 엔티티
     */
    AuditLog save(AuditLog auditLog);

    /**
     * 감사로그 ID 기준으로 단건 감사로그를 조회합니다.
     *
     * @param auditId 감사로그 ID
     * @return 감사로그 Optional
     */
    Optional<AuditLog> findById(Long auditId);
}
