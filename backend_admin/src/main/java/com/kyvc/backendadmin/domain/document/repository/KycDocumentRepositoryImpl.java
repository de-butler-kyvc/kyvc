package com.kyvc.backendadmin.domain.document.repository;

import com.kyvc.backendadmin.domain.document.domain.KycDocument;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link KycDocumentRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>kyc_documents 테이블의 documentId 기준 단건 조회를 처리합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class KycDocumentRepositoryImpl implements KycDocumentRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public Optional<KycDocument> findById(Long documentId) {
        return Optional.ofNullable(entityManager().find(KycDocument.class, documentId));
    }

    @Override
    public Optional<KycDocument> findByKycIdAndDocumentId(Long kycId, Long documentId) {
        return entityManager().createQuery("""
                        select document
                        from KycDocument document
                        where document.kycId = :kycId
                          and document.documentId = :documentId
                        """, KycDocument.class)
                .setParameter("kycId", kycId)
                .setParameter("documentId", documentId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public int updateUploadStatus(Long documentId, KyvcEnums.DocumentUploadStatus status) {
        return entityManager().createNativeQuery("""
                        update kyc_documents
                        set upload_status_code = :status
                        where document_id = :documentId
                        """)
                .setParameter("status", status.name())
                .setParameter("documentId", documentId)
                .executeUpdate();
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
