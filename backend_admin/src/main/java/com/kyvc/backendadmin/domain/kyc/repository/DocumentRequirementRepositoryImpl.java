package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.kyc.domain.DocumentRequirement;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

/**
 * {@link DocumentRequirementRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>document_requirements 저장, 중복 정책 확인, audit_logs 저장을 처리합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class DocumentRequirementRepositoryImpl implements DocumentRequirementRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public DocumentRequirement save(DocumentRequirement documentRequirement) {
        entityManager().persist(documentRequirement);
        return documentRequirement;
    }

    @Override
    public boolean existsByCorporateTypeAndDocumentType(String corporateType, String documentType) {
        Long count = entityManager()
                .createQuery("""
                        select count(requirement)
                        from DocumentRequirement requirement
                        where requirement.corporateTypeCode = :corporateType
                          and requirement.documentTypeCode = :documentType
                        """, Long.class)
                .setParameter("corporateType", corporateType)
                .setParameter("documentType", documentType)
                .getSingleResult();
        return count > 0;
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
