package com.kyvc.backend.domain.audit.repository;

import com.kyvc.backend.domain.audit.domain.AuditLog;

// 감사로그 Repository
public interface AuditLogRepository {

    // 감사로그 저장
    AuditLog save(
            AuditLog auditLog // 저장 대상 감사로그
    );
}
