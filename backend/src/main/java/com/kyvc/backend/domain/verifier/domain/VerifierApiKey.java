package com.kyvc.backend.domain.verifier.domain;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Verifier API Key Entity
@Entity
@Table(name = "verifier_api_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerifierApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "api_key_id")
    private Long apiKeyId; // API Key ID

    @Column(name = "verifier_id", nullable = false)
    private Long verifierId; // Verifier ID

    @Column(name = "key_name", length = 100)
    private String keyName; // API Key 이름

    @Column(name = "api_key_prefix", length = 30)
    private String apiKeyPrefix; // API Key 표시 prefix

    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash; // API Key hash

    @Enumerated(EnumType.STRING)
    @Column(name = "key_status_code", nullable = false, length = 30)
    private KyvcEnums.VerifierApiKeyStatus keyStatusCode; // API Key 상태

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt; // 발급 일시

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 만료 일시

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt; // 폐기 일시

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt; // 마지막 사용 일시

    // 활성 상태 여부
    public boolean isActive() {
        return KyvcEnums.VerifierApiKeyStatus.ACTIVE == keyStatusCode && revokedAt == null;
    }

    // 만료 여부
    public boolean isExpired(
            LocalDateTime now // 기준 일시
    ) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    // 마지막 사용 일시 갱신
    public void markUsed(
            LocalDateTime usedAt // 사용 일시
    ) {
        this.lastUsedAt = usedAt;
    }
}
