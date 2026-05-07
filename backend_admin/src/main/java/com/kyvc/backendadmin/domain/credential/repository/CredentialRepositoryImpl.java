package com.kyvc.backendadmin.domain.credential.repository;

import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

/**
 * {@link CredentialRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class CredentialRepositoryImpl implements CredentialRepository {

    private static final String DEFAULT_ISSUER_DID = "did:kyvc:backend-admin";

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public boolean existsIssuingOrValidByKycId(Long kycId) {
        Number count = (Number) entityManager().createNativeQuery("""
                        select count(*)
                        from credentials credential
                        where credential.kyc_id = :kycId
                          and credential.credential_status_code in ('ISSUING', 'VALID')
                        """)
                .setParameter("kycId", kycId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public Long createIssuing(
            Long corporateId,
            Long kycId,
            String credentialExternalId,
            KyvcEnums.CredentialType credentialType,
            String issuerDid,
            KyvcEnums.CredentialStatus status
    ) {
        Object result = entityManager().createNativeQuery("""
                        insert into credentials (
                            corporate_id,
                            kyc_id,
                            credential_external_id,
                            credential_type_code,
                            issuer_did,
                            credential_status_code,
                            created_at,
                            updated_at,
                            wallet_saved_yn,
                            credential_status_purpose_code,
                            kyc_level_code,
                            jurisdiction_code
                        ) values (
                            :corporateId,
                            :kycId,
                            :credentialExternalId,
                            :credentialType,
                            :issuerDid,
                            :status,
                            now(),
                            now(),
                            'N',
                            'revocation',
                            'BASIC',
                            'KR'
                        )
                        returning credential_id
                        """)
                .setParameter("corporateId", corporateId)
                .setParameter("kycId", kycId)
                .setParameter("credentialExternalId", credentialExternalId)
                .setParameter("credentialType", credentialType.name())
                .setParameter("issuerDid", issuerDid == null ? DEFAULT_ISSUER_DID : issuerDid)
                .setParameter("status", status.name())
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
