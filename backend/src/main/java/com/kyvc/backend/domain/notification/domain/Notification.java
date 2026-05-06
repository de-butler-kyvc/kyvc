package com.kyvc.backend.domain.notification.domain;

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
    }
}

