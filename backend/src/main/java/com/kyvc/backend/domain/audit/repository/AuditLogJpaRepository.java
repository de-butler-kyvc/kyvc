package com.kyvc.backend.domain.audit.repository;

import com.kyvc.backend.domain.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

// 감사로그 JPA Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {
}
