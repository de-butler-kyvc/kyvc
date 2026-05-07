package com.kyvc.backendadmin.domain.issuer.repository;

import com.kyvc.backendadmin.domain.issuer.domain.IssuerPolicy;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** {@link IssuerPolicyRepository}의 EntityManager 기반 구현체입니다. */
@Repository
@RequiredArgsConstructor
public class IssuerPolicyRepositoryImpl implements IssuerPolicyRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public IssuerPolicy save(IssuerPolicy policy) {
        if (policy.getPolicyId() == null) {
            entityManager().persist(policy);
            return policy;
        }
        return entityManager().merge(policy);
    }

    @Override
    public Optional<IssuerPolicy> findById(Long policyId) {
        return Optional.ofNullable(entityManager().find(IssuerPolicy.class, policyId));
    }

    @Override
    public boolean existsActiveByIssuerDidAndType(String issuerDid, KyvcEnums.IssuerPolicyType policyType) {
        Number count = (Number) entityManager().createNativeQuery("""
                        select count(*)
                        from issuer_policies
                        where issuer_did = :issuerDid
                          and issuer_policy_type_code = :policyType
                          and issuer_policy_status_code = 'ACTIVE'
                        """)
                .setParameter("issuerDid", issuerDid)
                .setParameter("policyType", policyType.name())
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public List<IssuerPolicy> findActiveByIssuerDid(String issuerDid) {
        return entityManager().createQuery("""
                        select policy
                        from IssuerPolicy policy
                        where policy.issuerDid = :issuerDid
                          and policy.status = :active
                        order by policy.updatedAt desc
                        """, IssuerPolicy.class)
                .setParameter("issuerDid", issuerDid)
                .setParameter("active", KyvcEnums.IssuerPolicyStatus.ACTIVE)
                .getResultList();
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
