package com.kyvc.backend.domain.notification.domain;

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

import java.time.LocalDateTime;

// 알림 Entity
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    private static final String READ_YES = "Y"; // 읽음 여부 Y 값
    private static final String READ_NO = "N"; // 읽지 않음 여부 N 값

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId; // 알림 ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 사용자 ID

    @Column(name = "notification_type_code", nullable = false, length = 100)
    private String notificationType; // 알림 유형 코드

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 알림 제목

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message; // 알림 메시지

    @Column(name = "read_yn", nullable = false, length = 1)
    private String readYn; // 읽음 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_code", length = 30)
    private KyvcEnums.NotificationChannel channelCode; // 알림 채널

    @Column(name = "target_type_code", length = 30)
    private String targetTypeCode; // 대상 유형 코드

    @Column(name = "target_id")
    private Long targetId; // 대상 ID

    @Column(name = "template_code", length = 100)
    private String templateCode; // 템플릿 코드

    @Enumerated(EnumType.STRING)
    @Column(name = "sent_status_code", length = 30)
    private KyvcEnums.NotificationSendStatus sentStatusCode; // 발송 상태

    @Column(name = "sent_at")
    private LocalDateTime sentAt; // 발송 일시

    @Column(name = "read_at")
    private LocalDateTime readAt; // 읽음 일시

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    private Notification(
            Long userId, // 수신 사용자 ID
            String notificationType, // 알림 유형 코드
            String title, // 알림 제목
            String message, // 알림 메시지
            String readYn // 읽음 여부
    ) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.readYn = readYn;
    }

    // 내부 알림 생성
    public static Notification create(
            Long userId, // 수신 사용자 ID
            String notificationType, // 알림 유형 코드
            String title, // 알림 제목
            String message // 알림 메시지
    ) {
        return new Notification(
                userId,
                notificationType,
                title,
                message,
                READ_NO
        );
    }

    // 소유자 여부
    // 템플릿 기반 내부 알림 생성
    public static Notification createFromTemplate(
            Long userId, // 수신 사용자 ID
            String notificationType, // 알림 유형 코드
            String title, // 알림 제목
            String message, // 알림 메시지
            KyvcEnums.NotificationChannel channelCode, // 알림 채널
            String targetTypeCode, // 대상 유형 코드
            Long targetId, // 대상 ID
            String templateCode // 템플릿 코드
    ) {
        Notification notification = new Notification(userId, notificationType, title, message, READ_NO);
        notification.channelCode = channelCode;
        notification.targetTypeCode = targetTypeCode;
        notification.targetId = targetId;
        notification.templateCode = templateCode;
        notification.sentStatusCode = KyvcEnums.NotificationSendStatus.SENT;
        notification.sentAt = LocalDateTime.now();
        return notification;
    }

    public boolean isOwner(
            Long userId // 사용자 ID
    ) {
        return this.userId != null && this.userId.equals(userId);
    }

    // 읽음 여부
    public boolean isRead() {
        return READ_YES.equals(readYn);
    }

    // 읽음 처리
    public void markAsRead() {
        this.readYn = READ_YES;
        this.readAt = LocalDateTime.now();
    }
}

