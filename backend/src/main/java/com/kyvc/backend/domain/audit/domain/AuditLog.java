package com.kyvc.backend.domain.audit.domain;

import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 감사로그 Entity
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId; // 감사로그 ID

    @Column(name = "actor_type_code", nullable = false, length = 50)
    private String actorType; // 행위자 유형 코드

    @Column(name = "actor_id", nullable = false)
    private Long actorId; // 행위자 ID

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType; // 작업 유형

    @Column(name = "audit_target_type_code", nullable = false, length = 100)
    private String auditTargetType; // 감사 대상 유형 코드

    @Column(name = "target_id", nullable = false)
    private Long targetId; // 대상 ID

    @Column(name = "request_summary", columnDefinition = "TEXT")
    private String requestSummary; // 요청 요약

    @Column(name = "ip_address", length = 100)
    private String ipAddress; // 요청 IP 주소

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    private AuditLog(
            String actorType, // 행위자 유형 코드
            Long actorId, // 행위자 ID
            String actionType, // 작업 유형
            String auditTargetType, // 감사 대상 유형 코드
            Long targetId, // 대상 ID
            String requestSummary, // 요청 요약
            String ipAddress // 요청 IP 주소
    ) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.actionType = actionType;
        this.auditTargetType = auditTargetType;
        this.targetId = targetId;
        this.requestSummary = requestSummary;
        this.ipAddress = ipAddress;
    }

    // 감사로그 생성
    public static AuditLog create(
            AuditLogCreateCommand command // 감사로그 저장 명령
    ) {
        return new AuditLog(
                command.actorType(),
                command.actorId(),
                command.actionType(),
                command.auditTargetType(),
                command.targetId(),
                command.requestSummary(),
                command.ipAddress()
        );
    }
}
