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

// Credential Offer 엔티티
@Entity
@Table(name = "credential_offers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CredentialOffer {

    private static final int FAILURE_REASON_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_offer_id")
    private Long credentialOfferId;

    @Column(name = "kyc_id", nullable = false)
    private Long kycId;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "offer_token_hash", nullable = false, length = 255)
    private String offerTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_status_code", nullable = false, length = 30)
    private KyvcEnums.CredentialOfferStatus offerStatus;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "credential_id")
    private Long credentialId;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "holder_did", length = 255)
    private String holderDid;

    @Column(name = "holder_xrpl_address", length = 255)
    private String holderXrplAddress;

    @Column(name = "failure_reason_code", length = 100)
    private String failureReasonCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Credential Offer 생성
    public static CredentialOffer create(
            Long kycId, // KYC 신청 ID
            Long corporateId, // 법인 ID
            String offerTokenHash, // Offer 토큰 해시
            LocalDateTime expiresAt // 만료 일시
    ) {
        LocalDateTime now = LocalDateTime.now(); // 생성 기준 일시
        CredentialOffer offer = new CredentialOffer();
        offer.kycId = kycId;
        offer.corporateId = corporateId;
        offer.offerTokenHash = offerTokenHash;
        offer.offerStatus = KyvcEnums.CredentialOfferStatus.ACTIVE;
        offer.expiresAt = expiresAt;
        offer.createdAt = now;
        offer.updatedAt = now;
        return offer;
    }

    // 활성 상태 여부
    public boolean isActive(
            LocalDateTime now // 기준 일시
    ) {
        return KyvcEnums.CredentialOfferStatus.ACTIVE == offerStatus && !isExpired(now);
    }

    // 만료 여부
    public boolean isExpired(
            LocalDateTime now // 기준 일시
    ) {
        LocalDateTime baseTime = now == null ? LocalDateTime.now() : now; // 기준 일시
        return expiresAt == null || !expiresAt.isAfter(baseTime);
    }

    // 법인 소유 여부
    public boolean isOwnedByCorporate(
            Long corporateId // 법인 ID
    ) {
        return this.corporateId != null && this.corporateId.equals(corporateId);
    }

    // 준비 완료 여부
    public boolean isPrepared() {
        return credentialId != null;
    }

    // 사용 완료 여부
    public boolean isUsed() {
        return KyvcEnums.CredentialOfferStatus.USED == offerStatus;
    }

    // 만료 상태 반영
    public void markExpired() {
        this.offerStatus = KyvcEnums.CredentialOfferStatus.EXPIRED;
        touchUpdatedAt();
    }

    // 취소 상태 반영
    public void cancel() {
        this.offerStatus = KyvcEnums.CredentialOfferStatus.CANCELLED;
        touchUpdatedAt();
    }

    // 발급 준비 Credential 연결
    public void bindPreparedCredential(
            Long credentialId, // Credential ID
            String deviceId, // 모바일 기기 ID
            String holderDid, // Holder DID
            String holderXrplAddress // Holder XRPL 주소
    ) {
        this.credentialId = credentialId;
        this.deviceId = deviceId;
        this.holderDid = holderDid;
        this.holderXrplAddress = holderXrplAddress;
        touchUpdatedAt();
    }

    // 사용 완료 상태 반영
    public void markUsed() {
        this.offerStatus = KyvcEnums.CredentialOfferStatus.USED;
        this.usedAt = LocalDateTime.now();
        touchUpdatedAt();
    }

    // 실패 상태 반영
    public void markFailed(
            String failureReasonCode // 실패 사유 코드
    ) {
        this.offerStatus = KyvcEnums.CredentialOfferStatus.FAILED;
        this.failureReasonCode = truncate(failureReasonCode);
        touchUpdatedAt();
    }

    // 수정 일시 갱신
    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String truncate(
            String value // 길이 제한 대상
    ) {
        if (value == null || value.length() <= FAILURE_REASON_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, FAILURE_REASON_MAX_LENGTH);
    }
}
