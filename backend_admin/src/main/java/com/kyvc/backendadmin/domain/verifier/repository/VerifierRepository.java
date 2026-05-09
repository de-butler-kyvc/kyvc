package com.kyvc.backendadmin.domain.verifier.repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Verifier 변경 Repository입니다.
 */
public interface VerifierRepository {

    Optional<VerifierRow> findById(Long verifierId);

    Optional<ApiKeyRow> findApiKey(Long verifierId, Long keyId);

    boolean existsByName(String name);

    Long create(String name, String contactEmail);

    void update(Long verifierId, String name, String contactEmail);

    void updateStatus(Long verifierId, String status, LocalDateTime approvedAt, LocalDateTime suspendedAt);

    Long saveCallback(Long verifierId, String callbackUrl, String status);

    void revokeActiveKeys(Long verifierId);

    Long createApiKey(Long verifierId, String keyName, String keyPrefix, String keyHash, LocalDateTime expiresAt);

    void updateApiKeyStatus(Long keyId, String status, LocalDateTime revokedAt);

    record VerifierRow(Long verifierId, String name, String status, String contactEmail,
                       LocalDateTime approvedAt, LocalDateTime suspendedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    record ApiKeyRow(Long keyId, Long verifierId, String keyName, String keyPrefix, String status, LocalDateTime expiresAt) {
    }
}
