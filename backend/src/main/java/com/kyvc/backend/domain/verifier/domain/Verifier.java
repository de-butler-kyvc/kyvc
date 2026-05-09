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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// Verifier 엔티티
@Entity
@Table(name = "verifiers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Verifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verifier_id")
    private Long verifierId;

    @Column(name = "verifier_name", nullable = false, length = 150)
    private String verifierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "verifier_status_code", nullable = false, length = 30)
    private KyvcEnums.VerifierStatus verifierStatus;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 인증 사용자 기준 Verifier 생성
    public static Verifier createForAuthenticatedUser(
            String verifierName, // Verifier 이름
            String contactEmail // 연락 이메일
    ) {
        Verifier verifier = new Verifier();
        verifier.verifierName = verifierName;
        verifier.contactEmail = contactEmail;
        verifier.verifierStatus = KyvcEnums.VerifierStatus.ACTIVE;
        verifier.approvedAt = LocalDateTime.now();
        return verifier;
    }

    // 활성 Verifier 여부
    public boolean isActive() {
        return KyvcEnums.VerifierStatus.ACTIVE == verifierStatus
                || KyvcEnums.VerifierStatus.APPROVED == verifierStatus;
    }
}
