package com.kyvc.backendadmin.domain.verifier.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@link VerifierRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class VerifierRepositoryImpl implements VerifierRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public Optional<VerifierRow> findById(Long verifierId) {
        List<?> rows = entityManager().createNativeQuery("""
                        select verifier_id, verifier_name, verifier_status_code, contact_email,
                               approved_at, suspended_at, created_at, updated_at
                        from verifiers
                        where verifier_id = :verifierId
                        """)
                .setParameter("verifierId", verifierId)
                .getResultList();
        return rows.stream().findFirst().map(row -> toVerifier((Object[]) row));
    }

    @Override
    public Optional<ApiKeyRow> findApiKey(Long verifierId, Long keyId) {
        List<?> rows = entityManager().createNativeQuery("""
                        select api_key_id, verifier_id, key_name, api_key_prefix, key_status_code, expires_at
                        from verifier_api_keys
                        where verifier_id = :verifierId and api_key_id = :keyId
                        """)
                .setParameter("verifierId", verifierId)
                .setParameter("keyId", keyId)
                .getResultList();
        return rows.stream().findFirst().map(row -> {
            Object[] values = (Object[]) row;
            return new ApiKeyRow(toLong(values[0]), toLong(values[1]), toString(values[2]), toString(values[3]),
                    toString(values[4]), toLocalDateTime(values[5]));
        });
    }

    @Override
    public boolean existsByName(String name) {
        Number count = (Number) entityManager().createNativeQuery("""
                        select count(*)
                        from verifiers
                        where lower(verifier_name) = lower(:name)
                        """)
                .setParameter("name", name)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public Long create(String name, String contactEmail) {
        Object result = entityManager().createNativeQuery("""
                        insert into verifiers (verifier_name, verifier_status_code, contact_email, created_at, updated_at)
                        values (:name, 'PENDING', :contactEmail, now(), now())
                        returning verifier_id
                        """)
                .setParameter("name", name)
                .setParameter("contactEmail", contactEmail)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    @Override
    public void update(Long verifierId, String name, String contactEmail) {
        entityManager().createNativeQuery("""
                        update verifiers
                        set verifier_name = coalesce(:name, verifier_name),
                            contact_email = coalesce(:contactEmail, contact_email),
                            updated_at = now()
                        where verifier_id = :verifierId
                        """)
                .setParameter("name", name)
                .setParameter("contactEmail", contactEmail)
                .setParameter("verifierId", verifierId)
                .executeUpdate();
    }

    @Override
    public void updateStatus(Long verifierId, String status, LocalDateTime approvedAt, LocalDateTime suspendedAt) {
        entityManager().createNativeQuery("""
                        update verifiers
                        set verifier_status_code = :status,
                            approved_at = coalesce(:approvedAt, approved_at),
                            suspended_at = coalesce(:suspendedAt, suspended_at),
                            updated_at = now()
                        where verifier_id = :verifierId
                        """)
                .setParameter("status", status)
                .setParameter("approvedAt", approvedAt)
                .setParameter("suspendedAt", suspendedAt)
                .setParameter("verifierId", verifierId)
                .executeUpdate();
    }

    @Override
    public Long saveCallback(Long verifierId, String callbackUrl, String status) {
        Object result = entityManager().createNativeQuery("""
                        insert into verifier_callbacks (verifier_id, callback_url, callback_status_code, enabled_yn, created_at, updated_at)
                        values (:verifierId, :callbackUrl, :status, 'Y', now(), now())
                        returning callback_id
                        """)
                .setParameter("verifierId", verifierId)
                .setParameter("callbackUrl", callbackUrl)
                .setParameter("status", status)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    @Override
    public void revokeActiveKeys(Long verifierId) {
        entityManager().createNativeQuery("""
                        update verifier_api_keys
                        set key_status_code = 'REVOKED',
                            revoked_at = coalesce(revoked_at, now())
                        where verifier_id = :verifierId and key_status_code = 'ACTIVE'
                        """)
                .setParameter("verifierId", verifierId)
                .executeUpdate();
    }

    @Override
    public Long createApiKey(Long verifierId, String keyName, String keyPrefix, String keyHash, LocalDateTime expiresAt) {
        Object result = entityManager().createNativeQuery("""
                        insert into verifier_api_keys (verifier_id, key_name, api_key_prefix, api_key_hash, key_status_code, issued_at, expires_at)
                        values (:verifierId, :keyName, :keyPrefix, :keyHash, 'ACTIVE', now(), :expiresAt)
                        returning api_key_id
                        """)
                .setParameter("verifierId", verifierId)
                .setParameter("keyName", keyName)
                .setParameter("keyPrefix", keyPrefix)
                .setParameter("keyHash", keyHash)
                .setParameter("expiresAt", expiresAt)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    @Override
    public void updateApiKeyStatus(Long keyId, String status, LocalDateTime revokedAt) {
        entityManager().createNativeQuery("""
                        update verifier_api_keys
                        set key_status_code = :status,
                            revoked_at = coalesce(:revokedAt, revoked_at)
                        where api_key_id = :keyId
                        """)
                .setParameter("status", status)
                .setParameter("revokedAt", revokedAt)
                .setParameter("keyId", keyId)
                .executeUpdate();
    }

    private VerifierRow toVerifier(Object[] row) {
        return new VerifierRow(toLong(row[0]), toString(row[1]), toString(row[2]), toString(row[3]),
                toLocalDateTime(row[4]), toLocalDateTime(row[5]), toLocalDateTime(row[6]), toLocalDateTime(row[7]));
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
