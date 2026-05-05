package com.kyvc.backend.domain.issuer.domain;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

// Issuer 정책 Entity
@Entity
@Table(name = "issuer_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuerPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issuer_policy_id")
    private Long issuerPolicyId;

    @Column(name = "issuer_did", nullable = false, length = 255)
    private String issuerDid;

    @Column(name = "issuer_name", nullable = false, length = 255)
    private String issuerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_policy_type_code", nullable = false, length = 50)
    private KyvcEnums.IssuerPolicyType issuerPolicyType;

    @Column(name = "credential_type_code", length = 100)
    private String credentialTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_policy_status_code", nullable = false, length = 50)
    private KyvcEnums.IssuerPolicyStatus issuerPolicyStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 활성 정책 여부
    public boolean isActive() {
        return KyvcEnums.IssuerPolicyStatus.ACTIVE == issuerPolicyStatus;
    }

    // 화이트리스트 정책 여부
    public boolean isWhitelist() {
        return KyvcEnums.IssuerPolicyType.WHITELIST == issuerPolicyType;
    }

    // 블랙리스트 정책 여부
    public boolean isBlacklist() {
        return KyvcEnums.IssuerPolicyType.BLACKLIST == issuerPolicyType;
    }

    // Credential 유형 지원 여부
    public boolean supportsCredentialType(
            String credentialTypeCode // Credential 유형 코드
    ) {
        return StringUtils.hasText(credentialTypeCode) && credentialTypeCode.equals(this.credentialTypeCode);
    }
}
