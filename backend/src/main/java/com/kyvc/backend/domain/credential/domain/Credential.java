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

    @Column(name = "offer_token_hash", length = 255)
    private String offerTokenHash;

    @Column(name = "offer_expires_at")
    private LocalDateTime offerExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_used_yn", length = 1)
    private KyvcEnums.Yn offerUsedYn;

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

    // Core VC 발급 대기 Credential 생성
    public static Credential createIssuing(
            Long corporateId, // 법인 ID
            Long kycId, // KYC 신청 ID
            String credentialExternalId, // Core Credential 외부 ID
            String credentialTypeCode, // Credential 유형 코드
            String issuerDid, // Issuer DID
            String credentialStatusPurposeCode, // Credential Status 목적 코드
            String kycLevelCode, // KYC 레벨 코드
            String jurisdictionCode, // 관할 코드
            String holderDid, // Holder DID
            String holderXrplAddress // Holder XRPL 주소
    ) {
        Credential credential = new Credential();
        credential.corporateId = corporateId;
        credential.kycId = kycId;
        credential.credentialExternalId = credentialExternalId;
        credential.credentialTypeCode = credentialTypeCode;
        credential.issuerDid = issuerDid;
        credential.credentialStatus = KyvcEnums.CredentialStatus.ISSUING;
        credential.walletSavedYn = KyvcEnums.Yn.N.name();
        credential.offerUsedYn = KyvcEnums.Yn.N;
        credential.holderDid = holderDid;
        credential.holderXrplAddress = holderXrplAddress;
        credential.credentialStatusPurposeCode = credentialStatusPurposeCode;
        credential.kycLevelCode = kycLevelCode;
        credential.jurisdictionCode = jurisdictionCode;
        return credential;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public Long getCorporateId() {
        return corporateId;
    }

    public Long getKycId() {
        return kycId;
    }

    public String getCredentialExternalId() {
        return credentialExternalId;
    }

    public String getCredentialTypeCode() {
        return credentialTypeCode;
    }

    public String getIssuerDid() {
        return issuerDid;
    }

    public KyvcEnums.CredentialStatus getCredentialStatus() {
        return credentialStatus;
    }

    public String getVcHash() {
        return vcHash;
    }

    public String getXrplTxHash() {
        return xrplTxHash;
    }

    public String getQrToken() {
        return qrToken;
    }

    public LocalDateTime getQrExpiresAt() {
        return qrExpiresAt;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getWalletSavedYn() {
        return walletSavedYn;
    }

    public LocalDateTime getWalletSavedAt() {
        return walletSavedAt;
    }

    public String getWalletDeviceId() {
        return walletDeviceId;
    }

    public String getHolderDid() {
        return holderDid;
    }

    public String getHolderXrplAddress() {
        return holderXrplAddress;
    }

    public String getCredentialStatusId() {
        return credentialStatusId;
    }

    public String getCredentialStatusPurposeCode() {
        return credentialStatusPurposeCode;
    }

    public String getKycLevelCode() {
        return kycLevelCode;
    }

    public String getJurisdictionCode() {
        return jurisdictionCode;
    }

    // 발급 완료 여부
    public boolean isIssued() {
        return KyvcEnums.CredentialStatus.VALID == credentialStatus;
    }

    // Core 발급 진행 중 여부
    public boolean isIssuing() {
        return KyvcEnums.CredentialStatus.ISSUING == credentialStatus;
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

    // Credential 폐기 상태 반영
    public void revoke(
            LocalDateTime revokedAt // 폐기 일시
    ) {
        this.credentialStatus = KyvcEnums.CredentialStatus.REVOKED;
        this.revokedAt = revokedAt == null ? LocalDateTime.now() : revokedAt;
    }

    // XRPL 기록 메타데이터 반영
    public void applyXrplTransactionMetadata(
            String xrplTxHash // XRPL 트랜잭션 해시
    ) {
        if (hasText(xrplTxHash)) {
            this.xrplTxHash = xrplTxHash;
        }
    }

    // Core VC 발급 메타데이터 반영
    public void applyIssuanceMetadata(
            String credentialExternalId, // Core Credential 외부 ID
            String issuerDid, // Issuer DID
            KyvcEnums.CredentialStatus credentialStatus, // Credential 상태
            String vcHash, // VC 해시
            String xrplTxHash, // XRPL 트랜잭션 해시
            String credentialStatusId, // Credential Status ID
            LocalDateTime issuedAt, // 발급 일시
            LocalDateTime expiresAt // 만료 일시
    ) {
        if (hasText(credentialExternalId)) {
            this.credentialExternalId = credentialExternalId;
        }
        if (hasText(issuerDid)) {
            this.issuerDid = issuerDid;
        }
        if (credentialStatus != null) {
            this.credentialStatus = credentialStatus;
        }
        if (hasText(vcHash)) {
            this.vcHash = vcHash;
        }
        if (hasText(xrplTxHash)) {
            this.xrplTxHash = xrplTxHash;
        }
        if (hasText(credentialStatusId)) {
            this.credentialStatusId = credentialStatusId;
        }
        if (issuedAt != null) {
            this.issuedAt = issuedAt;
        }
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
    }

    private static boolean hasText(
            String value // 검증 대상 문자열
    ) {
        return value != null && !value.isBlank();
    }
}
