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

    @Column(name = "kyc_level_code", length = 50)
    private String kycLevelCode;

    @Column(name = "jurisdiction_code", length = 50)
    private String jurisdictionCode;

    @Column(name = "credential_salt_hash", length = 255)
    private String credentialSaltHash;

    // 발급 완료 여부
    public boolean isIssued() {
        return KyvcEnums.CredentialStatus.VALID == credentialStatus;
    }

    // 법인 소유 여부
    public boolean isOwnedByCorporate(
            Long corporateId // 법인 ID
    ) {
        return this.corporateId != null && this.corporateId.equals(corporateId);
    }

    // 유효한 Credential 여부
    public boolean isValid(
            LocalDateTime now // 기준 일시
    ) {
        return KyvcEnums.CredentialStatus.VALID == credentialStatus
                && expiresAt != null
                && !expiresAt.isBefore(now);
    }

    // 만료 여부
    public boolean isExpired(
            LocalDateTime now // 기준 일시
    ) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    // Wallet 저장 여부
    public boolean isWalletSaved() {
        return KyvcEnums.Yn.Y.name().equals(walletSavedYn);
    }

    // Credential Offer 만료 여부
    public boolean isOfferExpired(
            LocalDateTime now // 기준 일시
    ) {
        return qrExpiresAt == null || qrExpiresAt.isBefore(now);
    }

    // Credential Offer 발급
    public void issueOffer(
            String qrToken, // QR 토큰
            LocalDateTime qrExpiresAt // QR 만료 일시
    ) {
        this.qrToken = qrToken;
        this.qrExpiresAt = qrExpiresAt;
    }

    // Wallet 저장 처리
    public void acceptToWallet(
            String walletDeviceId, // Wallet 기기 ID
            String holderDid, // Holder DID
            String holderXrplAddress, // Holder XRPL 주소
            LocalDateTime walletSavedAt // Wallet 저장 일시
    ) {
        this.walletSavedYn = KyvcEnums.Yn.Y.name();
        this.walletSavedAt = walletSavedAt;
        this.walletDeviceId = walletDeviceId;
        this.holderDid = holderDid;
        this.holderXrplAddress = holderXrplAddress;
    }

    // 상태 갱신
    public void refreshStatus(
            KyvcEnums.CredentialStatus credentialStatus // Credential 상태
    ) {
        this.credentialStatus = credentialStatus;
    }
}
