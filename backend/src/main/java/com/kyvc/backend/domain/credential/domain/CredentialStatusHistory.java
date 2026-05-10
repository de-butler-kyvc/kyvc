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

import java.time.LocalDateTime;

// Credential 상태 이력 Entity
// Credential 상태 이력 엔티티
@Entity
@Table(name = "credential_status_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CredentialStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "credential_id", nullable = false)
    private Long credentialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_status_code", length = 30)
    private KyvcEnums.CredentialStatus beforeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_status_code", nullable = false, length = 30)
    private KyvcEnums.CredentialStatus afterStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type_code", nullable = false, length = 30)
    private KyvcEnums.ActorType changedByType;

    @Column(name = "changed_by_id")
    private Long changedById;

    @Column(name = "reason_code", length = 50)
    private String reasonCode;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // Credential 상태 이력 생성
    public static CredentialStatusHistory create(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialStatus beforeStatusCode, // 이전 상태
            KyvcEnums.CredentialStatus afterStatusCode, // 이후 상태
            KyvcEnums.ActorType changedByTypeCode, // 변경자 유형
            Long changedById, // 변경자 ID
            String reason // 변경 사유
    ) {
        return create(credentialId, beforeStatusCode, afterStatusCode, changedByTypeCode, changedById, null, reason);
    }

    // Credential 상태 이력 생성
    public static CredentialStatusHistory create(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialStatus beforeStatus, // 변경 전 상태
            KyvcEnums.CredentialStatus afterStatus, // 변경 후 상태
            KyvcEnums.ActorType changedByType, // 변경자 유형
            Long changedById, // 변경자 ID
            String reasonCode, // 변경 사유 코드
            String reason // 변경 사유
    ) {
        CredentialStatusHistory history = new CredentialStatusHistory();
        history.credentialId = credentialId;
        history.beforeStatus = beforeStatus;
        history.afterStatus = afterStatus;
        history.changedByType = changedByType;
        history.changedById = changedById;
        history.reasonCode = reasonCode;
        history.reason = reason;
        history.changedAt = LocalDateTime.now();
        return history;
    }

    public String getBeforeStatusCode() {
        return enumName(beforeStatus);
    }

    public String getAfterStatusCode() {
        return enumName(afterStatus);
    }

    public KyvcEnums.ActorType getChangedByTypeCode() {
        return changedByType;
    }

    private static String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
