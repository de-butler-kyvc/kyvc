package com.kyvc.backendadmin.domain.admin.domain;

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

// 관리자 계정 주요 변경 이력을 저장하는 audit_logs 엔티티
/**
 * audit_logs 테이블과 매핑되는 감사 로그 엔티티입니다.
 *
 * <p>관리자 계정 생성과 수정 같은 주요 변경 작업의 행위자, 대상, 작업 코드,
 * 변경 설명을 저장합니다.</p>
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    // audit_logs 기본 키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long auditLogId;

    // 변경을 수행한 행위자 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type_code", nullable = false, length = 50)
    private KyvcEnums.ActorType actorType;

    // 변경을 수행한 관리자 ID
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    // 변경 대상 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_target_type_code", nullable = false, length = 50)
    private KyvcEnums.AuditTargetType targetType;

    // 변경 대상 ID
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 수행한 작업 코드
    @Column(name = "action_type", nullable = false, length = 100)
    private String action;

    // 변경 내용 요약
    @Column(name = "request_summary", length = 1000)
    private String description;

    // 감사 로그 생성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 관리자 계정 대상 감사 로그 생성
    /**
     * 관리자 계정 대상 감사 로그 엔티티를 생성합니다.
     *
     * @param actorId 변경을 수행한 관리자 ID
     * @param targetId 변경 대상 관리자 ID
     * @param action 작업 코드
     * @param description 변경 내용 요약
     * @return 관리자 계정 감사 로그 엔티티
     */
    public static AuditLog adminUser(
            Long actorId, // 변경을 수행한 관리자 ID
            Long targetId, // 변경 대상 관리자 ID
            String action, // 작업 코드
            String description // 변경 내용 요약
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorType = KyvcEnums.ActorType.ADMIN;
        auditLog.actorId = actorId;
        auditLog.targetType = KyvcEnums.AuditTargetType.ADMIN_USER;
        auditLog.targetId = targetId;
        auditLog.action = action;
        auditLog.description = description;
        return auditLog;
    }

    /**
     * 일반 사용자 계정 대상 감사 로그 엔티티를 생성합니다.
     *
     * @param actorId 변경을 수행한 관리자 ID
     * @param targetId 변경 대상 사용자 ID
     * @param action 작업 코드
     * @param description 변경 내용 요약
     * @return 사용자 계정 감사 로그 엔티티
     */
    public static AuditLog user(
            Long actorId,
            Long targetId,
            String action,
            String description
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorType = KyvcEnums.ActorType.ADMIN;
        auditLog.actorId = actorId;
        auditLog.targetType = KyvcEnums.AuditTargetType.USER;
        auditLog.targetId = targetId;
        auditLog.action = action;
        auditLog.description = description;
        return auditLog;
    }
    /**
     * KYC 필수서류 정책 대상 감사 로그 엔티티를 생성합니다.
     *
     * @param actorId 변경을 수행한 관리자 ID
     * @param targetId 변경 대상 필수서류 정책 ID
     * @param action 작업 코드
     * @param description 변경 내용 요약
     * @return KYC 필수서류 정책 감사 로그 엔티티
     */
    public static AuditLog documentRequirement(
            Long actorId,
            Long targetId,
            String action,
            String description
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorType = KyvcEnums.ActorType.ADMIN;
        auditLog.actorId = actorId;
        auditLog.targetType = KyvcEnums.AuditTargetType.DOCUMENT_REQUIREMENT;
        auditLog.targetId = targetId;
        auditLog.action = action;
        auditLog.description = description;
        return auditLog;
    }

    /**
     * KYC 신청 대상 감사 로그 엔티티를 생성합니다.
     *
     * @param actorId 변경을 수행한 관리자 ID
     * @param targetId 변경 대상 KYC 신청 ID
     * @param action 작업 코드
     * @param description 변경 내용 요약
     * @return KYC 신청 감사 로그 엔티티
     */
    public static AuditLog kycApplication(
            Long actorId,
            Long targetId,
            String action,
            String description
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorType = KyvcEnums.ActorType.ADMIN;
        auditLog.actorId = actorId;
        auditLog.targetType = KyvcEnums.AuditTargetType.KYC_APPLICATION;
        auditLog.targetId = targetId;
        auditLog.action = action;
        auditLog.description = description;
        return auditLog;
    }

    /**
     * KYC 보완요청 대상 감사 로그 엔티티를 생성합니다.
     *
     * @param actorId 변경을 수행한 관리자 ID
     * @param targetId 변경 대상 보완요청 ID
     * @param action 작업 코드
     * @param description 변경 내용 요약
     * @return KYC 보완요청 감사 로그 엔티티
     */
    public static AuditLog kycSupplement(
            Long actorId,
            Long targetId,
            String action,
            String description
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorType = KyvcEnums.ActorType.ADMIN;
        auditLog.actorId = actorId;
        auditLog.targetType = KyvcEnums.AuditTargetType.KYC_SUPPLEMENT;
        auditLog.targetId = targetId;
        auditLog.action = action;
        auditLog.description = description;
        return auditLog;
    }

    /**
     * 공통코드 대상 감사 로그 엔티티를 생성합니다.
     *
     * @param actorId 변경을 수행한 관리자 ID
     * @param targetId 변경 대상 공통코드 ID
     * @param action 작업 코드
     * @param description 변경 내용 요약
     * @return 공통코드 감사 로그 엔티티
     */
    public static AuditLog commonCode(
            Long actorId,
            Long targetId,
            String action,
            String description
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorType = KyvcEnums.ActorType.ADMIN;
        auditLog.actorId = actorId;
        auditLog.targetType = KyvcEnums.AuditTargetType.COMMON_CODE;
        auditLog.targetId = targetId;
        auditLog.action = action;
        auditLog.description = description;
        return auditLog;
    }
}
