package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.kyc.domain.KycApplication;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

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

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
