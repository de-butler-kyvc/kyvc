package com.kyvc.backend.domain.credential.domain;

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

// Credential 엔티티
@Entity
@Table(name = "credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id")
    private Long credentialId;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "kyc_id", nullable = false)
    private Long kycId;

    @Column(name = "credential_external_id", nullable = false, length = 255)
    private String credentialExternalId;

    @Column(name = "credential_type_code", nullable = false, length = 100)
    private String credentialTypeCode;

    @Column(name = "issuer_did", nullable = false, length = 255)
    private String issuerDid;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_status_code", nullable = false, length = 50)
    private KyvcEnums.CredentialStatus credentialStatus;

    @Column(name = "vc_hash", length = 255)
    private String vcHash;

    @Column(name = "xrpl_tx_hash", length = 255)
    private String xrplTxHash;

    @Column(name = "qr_token", length = 255)
    private String qrToken;

    @Column(name = "qr_expires_at")
    private LocalDateTime qrExpiresAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "wallet_saved_yn", nullable = false, length = 1)
    private String walletSavedYn;

    @Column(name = "wallet_saved_at")
    private LocalDateTime walletSavedAt;

    @Column(name = "wallet_device_id", length = 255)
    private String walletDeviceId;

    @Column(name = "holder_did", length = 255)
    private String holderDid;

    @Column(name = "holder_xrpl_address", length = 255)
    private String holderXrplAddress;

    @Column(name = "credential_status_id", length = 500)
    private String credentialStatusId;

    @Column(name = "credential_status_purpose_code", nullable = false, length = 50)
    private String credentialStatusPurposeCode;

    // 발급 완료 여부
    public boolean isIssued() {
        return KyvcEnums.CredentialStatus.VALID == credentialStatus;
    }
}
