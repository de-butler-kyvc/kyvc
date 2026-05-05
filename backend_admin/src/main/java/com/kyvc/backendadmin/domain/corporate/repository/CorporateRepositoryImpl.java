package com.kyvc.backendadmin.domain.corporate.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.corporate.domain.Corporate;
import com.kyvc.backendadmin.domain.user.domain.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link CorporateRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>users와 corporates 테이블의 기본 단건 조회와 audit_logs 저장을 처리합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class CorporateRepositoryImpl implements CorporateRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public Optional<User> findUserById(Long userId) {
        return Optional.ofNullable(entityManager().find(User.class, userId));
    }

    @Override
    public Optional<Corporate> findCorporateById(Long corporateId) {
        return Optional.ofNullable(entityManager().find(Corporate.class, corporateId));
    }

    @Override
    public AuditLog saveAuditLog(AuditLog auditLog) {
        entityManager().persist(auditLog);
        return auditLog;
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
