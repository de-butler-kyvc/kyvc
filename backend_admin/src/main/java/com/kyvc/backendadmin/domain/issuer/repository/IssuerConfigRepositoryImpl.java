package com.kyvc.backendadmin.domain.issuer.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

/** {@link IssuerConfigRepository}의 EntityManager 기반 구현체입니다. */
@Repository
@RequiredArgsConstructor
public class IssuerConfigRepositoryImpl implements IssuerConfigRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public boolean existsById(Long issuerConfigId) {
        Number count = (Number) entityManagerProvider.getObject().createNativeQuery("""
                        select count(*)
                        from issuer_configs
                        where issuer_config_id = :issuerConfigId
                        """)
                .setParameter("issuerConfigId", issuerConfigId)
                .getSingleResult();
        return count.longValue() > 0;
    }
}
