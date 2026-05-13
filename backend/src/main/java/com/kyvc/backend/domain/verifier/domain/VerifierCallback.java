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

// Verifier callback Entity
@Entity
@Table(name = "verifier_callbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerifierCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "callback_id")
    private Long callbackId; // Callback ID

    @Column(name = "verifier_id", nullable = false)
    private Long verifierId; // Verifier ID

    @Column(name = "callback_url", nullable = false, length = 500)
    private String callbackUrl; // Callback URL

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_status_code", nullable = false, length = 30)
    private KyvcEnums.VerifierCallbackStatus callbackStatusCode; // Callback 상태

    @Column(name = "enabled_yn", nullable = false, length = 1)
    private String enabledYn; // 사용 여부

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 일시
}
