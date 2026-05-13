package com.kyvc.backendadmin.domain.issuer.domain;

import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** issuer_policies 테이블과 매핑되는 Issuer 신뢰 정책 엔티티입니다. */
@Entity
@Table(name = "issuer_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssuerPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issuer_policy_id")
    private Long policyId;

    @Column(name = "issuer_did", nullable = false)
    private String issuerDid;

    @Column(name = "issuer_name", nullable = false)
    private String issuerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_policy_type_code", nullable = false, length = 50)
    private KyvcEnums.IssuerPolicyType policyType;

    @Column(name = "credential_type_code", length = 100)
    private String credentialTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuer_policy_status_code", nullable = false, length = 50)
    private KyvcEnums.IssuerPolicyStatus status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Issuer 정책을 생성합니다. */
    public static IssuerPolicy create(
            String issuerDid,
            String issuerName,
            KyvcEnums.IssuerPolicyType policyType,
            String credentialTypeCode,
            KyvcEnums.IssuerPolicyStatus status,
            String reason
    ) {
        IssuerPolicy policy = new IssuerPolicy();
        policy.issuerDid = issuerDid;
        policy.issuerName = issuerName;
        policy.policyType = policyType;
        policy.credentialTypeCode = credentialTypeCode;
        policy.status = status;
        policy.reason = reason;
        return policy;
    }

    /** Issuer 정책의 수정 가능한 값을 변경합니다. */
    public void update(String issuerName, String credentialTypeCode, KyvcEnums.IssuerPolicyStatus status, String reason) {
        if (issuerName != null) {
            this.issuerName = issuerName;
        }
        if (credentialTypeCode != null) {
            this.credentialTypeCode = credentialTypeCode;
        }
        if (status != null) {
            this.status = status;
        }
        if (reason != null) {
            this.reason = reason;
        }
    }

    /** 승인요청 상태 전이 */
    public void submitApproval(String comment) {
        this.status = KyvcEnums.IssuerPolicyStatus.PENDING;
        if (comment != null) {
            this.reason = comment;
        }
    }

    /** 승인 상태 전이 */
    public void approve(String comment) {
        this.status = KyvcEnums.IssuerPolicyStatus.ACTIVE;
        if (comment != null) {
            this.reason = comment;
        }
    }

    /** 반려 상태 전이 */
    public void reject(String reason) {
        this.status = KyvcEnums.IssuerPolicyStatus.REJECTED;
        this.reason = reason;
    }

    /** Issuer 정책을 비활성화합니다. */
    public void disable() {
        this.status = KyvcEnums.IssuerPolicyStatus.INACTIVE;
    }
}
