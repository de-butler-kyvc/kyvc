package com.kyvc.backendadmin.domain.credential.repository;

import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * {@link CredentialRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class CredentialRepositoryImpl implements CredentialRepository {

    private static final String DEFAULT_ISSUER_DID = "did:kyvc:backend-admin";
    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_PROCESSING = "PROCESSING";

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

    @Override
    public Long saveCredentialRequest(
            Long credentialId,
            String requestTypeCode,
            String requestStatusCode,
            String requestedByTypeCode,
            Long requestedById,
            String reasonCode,
            String reason,
            String coreRequestId
    ) {
        return saveCredentialRequest(
                credentialId,
                requestTypeCode,
                requestStatusCode,
                requestedByTypeCode,
                requestedById,
                reasonCode,
                reason,
                coreRequestId,
                LocalDateTime.now()
        ).credentialRequestId();
    }

    @Override
    public CredentialRequestSaveResult saveCredentialRequest(
            Long credentialId,
            String requestTypeCode,
            String requestStatusCode,
            String requestedByTypeCode,
            Long requestedById,
            String reasonCode,
            String reason,
            String coreRequestId,
            LocalDateTime requestedAt
    ) {
        Object result = entityManager().createNativeQuery("""
                        insert into credential_requests (
                            credential_id,
                            request_type_code,
                            request_status_code,
                            requested_by_type_code,
                            requested_by_id,
                            reason_code,
                            reason,
                            core_request_id,
                            requested_at
                        ) values (
                            :credentialId,
                            :requestTypeCode,
                            :requestStatusCode,
                            :requestedByTypeCode,
                            :requestedById,
                            :reasonCode,
                            :reason,
                            :coreRequestId,
                            :requestedAt
                        )
                        returning credential_request_id
                        """)
                .setParameter("credentialId", credentialId)
                .setParameter("requestTypeCode", requestTypeCode)
                .setParameter("requestStatusCode", requestStatusCode)
                .setParameter("requestedByTypeCode", requestedByTypeCode)
                .setParameter("requestedById", requestedById)
                .setParameter("reasonCode", reasonCode)
                .setParameter("reason", reason)
                .setParameter("coreRequestId", coreRequestId)
                .setParameter("requestedAt", requestedAt)
                .getSingleResult();
        return new CredentialRequestSaveResult(((Number) result).longValue(), requestedAt);
    }

    @Override
    public boolean existsInProgressCredentialRequest(Long credentialId, String requestTypeCode) {
        Number count = (Number) entityManager().createNativeQuery("""
                        select count(*)
                        from credential_requests credential_request
                        where credential_request.credential_id = :credentialId
                          and credential_request.request_type_code = :requestTypeCode
                          and credential_request.request_status_code in (:requestedStatus, :processingStatus)
                        """)
                .setParameter("credentialId", credentialId)
                .setParameter("requestTypeCode", requestTypeCode)
                .setParameter("requestedStatus", STATUS_REQUESTED)
                .setParameter("processingStatus", STATUS_PROCESSING)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public Long saveStatusHistory(
            Long credentialId,
            String beforeStatusCode,
            String afterStatusCode,
            String changedByTypeCode,
            Long changedById,
            String reasonCode,
            String reason
    ) {
        Object result = entityManager().createNativeQuery("""
                        insert into credential_status_histories (
                            credential_id,
                            before_status_code,
                            after_status_code,
                            changed_by_type_code,
                            changed_by_id,
                            reason_code,
                            reason,
                            changed_at
                        ) values (
                            :credentialId,
                            :beforeStatusCode,
                            :afterStatusCode,
                            :changedByTypeCode,
                            :changedById,
                            :reasonCode,
                            :reason,
                            now()
                        )
                        returning history_id
                        """)
                .setParameter("credentialId", credentialId)
                .setParameter("beforeStatusCode", beforeStatusCode)
                .setParameter("afterStatusCode", afterStatusCode)
                .setParameter("changedByTypeCode", changedByTypeCode)
                .setParameter("changedById", changedById)
                .setParameter("reasonCode", reasonCode)
                .setParameter("reason", reason)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
