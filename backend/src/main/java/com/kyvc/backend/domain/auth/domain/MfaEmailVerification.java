package com.kyvc.backend.domain.auth.domain;

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

// MFA 이메일 인증 Entity
@Entity
@Table(name = "mfa_email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfaEmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mfa_verification_id")
    private Long mfaVerificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type_code", nullable = false, length = 50)
    private KyvcEnums.ActorType actorTypeCode;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_purpose_code", nullable = false, length = 100)
    private KyvcEnums.MfaPurpose mfaPurposeCode;

    @Column(name = "verification_code_hash", nullable = false, length = 255)
    private String verificationCodeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_status_code", nullable = false, length = 50)
    private KyvcEnums.MfaStatus mfaStatusCode;

    @Column(name = "failed_attempt_count", nullable = false)
    private Integer failedAttemptCount;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // MFA 이메일 인증 생성
    public static MfaEmailVerification create(
            Long actorId, // 사용자 ID
            String email, // 사용자 이메일
            KyvcEnums.MfaPurpose purpose, // MFA 목적 코드
            String verificationCodeHash, // 인증번호 해시
            LocalDateTime requestedAt, // 요청 일시
            LocalDateTime expiresAt // 만료 일시
    ) {
        MfaEmailVerification verification = new MfaEmailVerification();
        verification.actorTypeCode = KyvcEnums.ActorType.USER;
        verification.actorId = actorId;
        verification.email = email;
        verification.mfaPurposeCode = purpose;
        verification.verificationCodeHash = verificationCodeHash;
        verification.mfaStatusCode = KyvcEnums.MfaStatus.REQUESTED;
        verification.failedAttemptCount = 0;
        verification.requestedAt = requestedAt;
        verification.expiresAt = expiresAt;
        return verification;
    }

    // 회원가입 이메일 인증 생성
    public static MfaEmailVerification createSignupEmailVerification(
            String email, // 정규화 이메일
            KyvcEnums.MfaPurpose purpose, // 인증 목적
            String verificationCodeHash, // 인증번호 해시
            LocalDateTime requestedAt, // 요청 일시
            LocalDateTime expiresAt // 만료 일시
    ) {
        MfaEmailVerification verification = new MfaEmailVerification();
        verification.actorTypeCode = KyvcEnums.ActorType.SYSTEM;
        verification.actorId = 0L;
        verification.email = email;
        verification.mfaPurposeCode = purpose;
        verification.verificationCodeHash = verificationCodeHash;
        verification.mfaStatusCode = KyvcEnums.MfaStatus.REQUESTED;
        verification.failedAttemptCount = 0;
        verification.requestedAt = requestedAt;
        verification.expiresAt = expiresAt;
        return verification;
    }

    // 본인 MFA 인증 여부
    public boolean isOwnedBy(
            Long actorId // 사용자 ID
    ) {
        return actorTypeCode == KyvcEnums.ActorType.USER && this.actorId != null && this.actorId.equals(actorId);
    }

    // 회원가입 이메일 인증 여부
    public boolean isSignupVerification() {
        return actorTypeCode == KyvcEnums.ActorType.SYSTEM
                && (mfaPurposeCode == KyvcEnums.MfaPurpose.SIGNUP
                || mfaPurposeCode == KyvcEnums.MfaPurpose.SIGNUP_EMAIL_VERIFICATION);
    }

    // 검증 가능 상태 여부
    public boolean isRequested() {
        return KyvcEnums.MfaStatus.REQUESTED == mfaStatusCode;
    }

    // 만료 여부
    public boolean isExpired(
            LocalDateTime now // 현재 시각
    ) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    // 인증번호 해시 일치 여부
    public boolean matchesCodeHash(
            String verificationCodeHash // 입력 인증번호 해시
    ) {
        return this.verificationCodeHash != null && this.verificationCodeHash.equals(verificationCodeHash);
    }

    // 인증 성공 처리
    public void markVerified(
            LocalDateTime verifiedAt // 검증 일시
    ) {
        this.mfaStatusCode = KyvcEnums.MfaStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
    }

    // 만료 처리
    public void markExpired() {
        this.mfaStatusCode = KyvcEnums.MfaStatus.EXPIRED;
    }

    // 실패 처리
    public void increaseFailure(
            int maxAttempts // 최대 허용 실패 횟수
    ) {
        this.failedAttemptCount = (failedAttemptCount == null ? 0 : failedAttemptCount) + 1;
        if (this.failedAttemptCount >= maxAttempts) {
            this.mfaStatusCode = KyvcEnums.MfaStatus.FAILED;
        }
    }

    // 실패 횟수 초과 여부
    public boolean isAttemptExceeded(
            int maxAttempts // 최대 허용 실패 횟수
    ) {
        return failedAttemptCount != null && failedAttemptCount >= maxAttempts;
    }
}
