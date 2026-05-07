package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.kyc.domain.KycApplication;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * {@link KycApplicationRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>kyc_applications 테이블의 기본 단건 조회를 처리합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class KycApplicationRepositoryImpl implements KycApplicationRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public Optional<KycApplication> findById(Long kycId) {
        return Optional.ofNullable(entityManager().find(KycApplication.class, kycId));
    }

    @Override
    public int updateAiReviewStatus(Long kycId, KyvcEnums.AiReviewStatus aiReviewStatus) {
        return entityManager().createNativeQuery("""
                        update kyc_applications
                        set ai_review_status_code = :aiReviewStatus,
                            updated_at = :updatedAt
                        where kyc_id = :kycId
                        """)
                .setParameter("aiReviewStatus", aiReviewStatus.name())
                .setParameter("updatedAt", LocalDateTime.now())
                .setParameter("kycId", kycId)
                .executeUpdate();
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
