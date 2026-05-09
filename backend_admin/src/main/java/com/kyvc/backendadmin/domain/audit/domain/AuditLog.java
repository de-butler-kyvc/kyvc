package com.kyvc.backendadmin.domain.audit.domain;

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

/**
 * Audit log entity mapped to audit_logs.
 */
@Entity(name = "AuditLogForAudit")
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type_code", nullable = false, length = 50)
    private KyvcEnums.ActorType actorType;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_target_type_code", nullable = false, length = 100)
    private KyvcEnums.AuditTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "request_summary", columnDefinition = "TEXT")
    private String requestSummary;

    @Column(name = "before_value_json", columnDefinition = "TEXT")
    private String beforeValueJson;

    @Column(name = "after_value_json", columnDefinition = "TEXT")
    private String afterValueJson;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AuditLog create(
            KyvcEnums.ActorType actorType,
            Long actorId,
            String actionType,
            KyvcEnums.AuditTargetType targetType,
            Long targetId,
            String requestSummary,
            String ipAddress
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorType = actorType;
        auditLog.actorId = actorId;
        auditLog.actionType = actionType;
        auditLog.targetType = targetType;
        auditLog.targetId = targetId;
        auditLog.requestSummary = requestSummary;
        auditLog.ipAddress = ipAddress;
        return auditLog;
    }

    public static AuditLog create(
            KyvcEnums.ActorType actorType,
            Long actorId,
            String actionType,
            KyvcEnums.AuditTargetType targetType,
            Long targetId,
            String requestSummary,
            String beforeValueJson,
            String afterValueJson,
            String ipAddress
    ) {
        AuditLog auditLog = create(actorType, actorId, actionType, targetType, targetId, requestSummary, ipAddress);
        auditLog.beforeValueJson = beforeValueJson;
        auditLog.afterValueJson = afterValueJson;
        return auditLog;
    }
}
