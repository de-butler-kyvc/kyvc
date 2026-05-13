package com.kyvc.backend.domain.audit.repository;

import com.kyvc.backend.domain.audit.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 감사로그 Repository 구현체
@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository auditLogJpaRepository;

    // 감사로그 저장
    @Override
    public AuditLog save(
            AuditLog auditLog // 저장 대상 감사로그
    ) {
        return auditLogJpaRepository.save(auditLog);
    }
}
