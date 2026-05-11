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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 알림 템플릿 Entity
@Entity
@Table(name = "notification_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId; // 템플릿 ID

    @Column(name = "template_code", nullable = false, length = 100)
    private String templateCode; // 템플릿 코드

    @Column(name = "template_name", nullable = false, length = 150)
    private String templateName; // 템플릿 이름

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_code", nullable = false, length = 30)
    private KyvcEnums.NotificationChannel channelCode; // 알림 채널

    @Column(name = "title_template", nullable = false, length = 255)
    private String titleTemplate; // 제목 템플릿

    @Column(name = "message_template", nullable = false, columnDefinition = "TEXT")
    private String messageTemplate; // 본문 템플릿

    @Column(name = "enabled_yn", nullable = false, length = 1)
    private String enabledYn; // 사용 여부

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 일시
}
