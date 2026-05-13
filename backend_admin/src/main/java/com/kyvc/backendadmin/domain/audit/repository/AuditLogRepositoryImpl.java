package com.kyvc.backendadmin.domain.audit.repository;

import com.kyvc.backendadmin.domain.audit.domain.AuditLog;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public AuditLog save(AuditLog auditLog) {
        entityManager().persist(auditLog);
        return auditLog;
    }

    @Override
    public Optional<AuditLog> findById(Long auditId) {
        return Optional.ofNullable(entityManager().find(AuditLog.class, auditId));
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
