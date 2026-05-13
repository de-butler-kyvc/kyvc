package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.review.domain.AiReviewPolicy;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link AiReviewPolicyRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class AiReviewPolicyRepositoryImpl implements AiReviewPolicyRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public AiReviewPolicy save(AiReviewPolicy policy) {
        if (policy.getAiPolicyId() == null) {
            entityManager().persist(policy);
            return policy;
        }
        return entityManager().merge(policy);
    }

    @Override
    public Optional<AiReviewPolicy> findById(Long aiPolicyId) {
        return Optional.ofNullable(entityManager().find(AiReviewPolicy.class, aiPolicyId));
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
