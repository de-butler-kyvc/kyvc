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

    @Column(name = "before_status_code", length = 30)
    private String beforeStatusCode;

    @Column(name = "after_status_code", nullable = false, length = 30)
    private String afterStatusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "changed_by_type_code", nullable = false, length = 30)
    private KyvcEnums.ActorType changedByTypeCode;

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
        CredentialStatusHistory history = new CredentialStatusHistory();
        history.credentialId = credentialId;
        history.beforeStatusCode = enumName(beforeStatusCode);
        history.afterStatusCode = enumName(afterStatusCode);
        history.changedByTypeCode = changedByTypeCode;
        history.changedById = changedById;
        history.reason = reason;
        history.changedAt = LocalDateTime.now();
        return history;
    }

    private static String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
