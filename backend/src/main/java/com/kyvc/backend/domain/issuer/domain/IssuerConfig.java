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

import java.time.LocalDateTime;

// Issuer 설정 Entity
@Entity
@Table(name = "issuer_configs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuerConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issuer_config_id")
    private Long issuerConfigId;

    @Column(name = "issuer_did", nullable = false, length = 255)
    private String issuerDid;

    @Column(name = "issuer_name", nullable = false, length = 255)
    private String issuerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_type_code", nullable = false, length = 50)
    private KyvcEnums.IssuerType issuerType;

    @Column(name = "issuer_xrpl_address", nullable = false, length = 255)
    private String issuerXrplAddress;

    @Column(name = "verification_method_id", nullable = false, length = 255)
    private String verificationMethodId;

    @Column(name = "signing_key_ref", nullable = false, length = 255)
    private String signingKeyRef;

    @Column(name = "cryptosuite", nullable = false, length = 100)
    private String cryptosuite;

    @Column(name = "credential_type_code", nullable = false, length = 100)
    private String credentialTypeCode;

    @Column(name = "credential_schema_id", nullable = false, length = 255)
    private String credentialSchemaId;

    @Column(name = "valid_days", nullable = false)
    private Integer validDays;

    @Column(name = "default_yn", nullable = false, length = 1)
    private String defaultYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_config_status_code", nullable = false, length = 50)
    private KyvcEnums.IssuerConfigStatus issuerConfigStatus;

    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;

    @Column(name = "updated_by_admin_id")
    private Long updatedByAdminId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 활성 설정 여부
    public boolean isActive() {
        return KyvcEnums.IssuerConfigStatus.ACTIVE == issuerConfigStatus;
    }
}
