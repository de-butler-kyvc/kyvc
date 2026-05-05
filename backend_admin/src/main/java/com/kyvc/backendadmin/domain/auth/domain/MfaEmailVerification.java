package com.kyvc.backendadmin.domain.auth.domain;

import com.kyvc.backendadmin.global.util.KyvcEnums;
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

import java.time.LocalDateTime;

// 이메일 MFA challenge와 인증번호 검증 상태를 저장하는 엔티티
/**
 * mfa_email_verifications 테이블과 매핑되는 이메일 MFA 인증 엔티티입니다.
 *
 * <p>MFA challenge ID, 대상 관리자, 목적, 대상 이메일, 인증번호 해시,
 * 실패 횟수와 인증 상태를 저장합니다.</p>
 */
@Entity
@Table(name = "mfa_email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfaEmailVerification {

    // mfa_email_verifications 기본 키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mfa_verification_id")
    private Long mfaEmailVerificationId;

    // MFA 대상 행위자 유형, Backend Admin에서는 ADMIN 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type_code", nullable = false, length = 50)
    private KyvcEnums.ActorType actorType;

    // MFA 대상 관리자 ID
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    // LOGIN, IMPORTANT_ACTION 등 MFA 목적
    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_purpose_code", nullable = false, length = 100)
    private KyvcEnums.MfaPurpose purpose;

    // 인증번호 발송 대상 이메일
    @Column(name = "email", nullable = false)
    private String targetEmail;

    // 원문 인증번호가 아닌 SHA-256 해시 값
    @Column(name = "verification_code_hash", nullable = false, length = 100)
    private String verificationCodeHash;

    // MFA challenge 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_status_code", nullable = false, length = 50)
    private KyvcEnums.MfaStatus status;

    // 인증번호 입력 실패 횟수
    @Column(name = "failed_attempt_count", nullable = false)
    private int failedCount;

    // 인증 요청 시각
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    // 인증번호 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 인증 성공 시각
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // challenge 생성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // challenge 수정 시각
    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 이메일 MFA challenge 신규 생성
    /**
     * 이메일 MFA challenge 엔티티를 생성합니다.
     *
     * @param actorId MFA 대상 관리자 ID
     * @param purpose MFA 목적
     * @param targetEmail 인증번호 발송 대상 이메일
     * @param verificationCodeHash 인증번호 원문의 SHA-256 해시
     * @param expiresAt 인증번호 만료 시각
     * @return 신규 이메일 MFA 인증 엔티티
     */
    public static MfaEmailVerification create(
            Long actorId,
            KyvcEnums.MfaPurpose purpose,
            String targetEmail,
            String verificationCodeHash,
            LocalDateTime expiresAt
    ) {
        MfaEmailVerification verification = new MfaEmailVerification();
        verification.actorType = KyvcEnums.ActorType.ADMIN;
        verification.actorId = actorId;
        verification.purpose = purpose;
        verification.targetEmail = targetEmail;
        verification.verificationCodeHash = verificationCodeHash;
        verification.status = KyvcEnums.MfaStatus.REQUESTED;
        verification.requestedAt = LocalDateTime.now();
        verification.expiresAt = expiresAt;
        return verification;
    }

    public String getChallengeId() {
        return mfaEmailVerificationId == null ? null : String.valueOf(mfaEmailVerificationId);
    }

    // 전달받은 기준 시각으로 만료 여부 확인
    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    // 이미 인증 완료된 challenge인지 확인
    public boolean isVerified() {
        return KyvcEnums.MfaStatus.VERIFIED == status;
    }

    // 인증번호 불일치 시 실패 횟수 증가
    public void increaseFailedCount() {
        this.failedCount++;
    }

    // 인증 성공 처리
    public void verify(LocalDateTime verifiedAt) {
        this.status = KyvcEnums.MfaStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
    }
}
