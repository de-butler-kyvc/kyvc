package com.kyvc.backend.domain.mobile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 모바일 보안 설정 Entity
@Entity
@Table(name = "mobile_security_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileSecuritySetting {

    private static final String YES = "Y"; // Y 값
    private static final String NO = "N"; // N 값

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "security_setting_id")
    private Long securitySettingId; // 보안 설정 ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 사용자 ID

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId; // 모바일 기기 ID

    @Column(name = "pin_enabled_yn", nullable = false, length = 1)
    private String pinEnabledYn; // PIN 사용 여부

    @Column(name = "biometric_enabled_yn", nullable = false, length = 1)
    private String biometricEnabledYn; // 생체인증 사용 여부

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정 일시

    // 모바일 보안 설정 생성
    public static MobileSecuritySetting create(
            Long userId, // 사용자 ID
            String deviceId, // 모바일 기기 ID
            Boolean pinEnabled, // PIN 사용 여부
            Boolean biometricEnabled // 생체인증 사용 여부
    ) {
        MobileSecuritySetting securitySetting = new MobileSecuritySetting();
        securitySetting.userId = userId;
        securitySetting.deviceId = deviceId;
        securitySetting.pinEnabledYn = toYn(pinEnabled);
        securitySetting.biometricEnabledYn = toYn(biometricEnabled);
        return securitySetting;
    }

    // 소유자 여부
    public boolean isOwner(
            Long userId // 사용자 ID
    ) {
        return this.userId != null && this.userId.equals(userId);
    }

    // PIN 사용 여부
    public boolean isPinEnabled() {
        return YES.equals(pinEnabledYn);
    }

    // 생체인증 사용 여부
    public boolean isBiometricEnabled() {
        return YES.equals(biometricEnabledYn);
    }

    // 보안 설정 갱신
    public void update(
            Boolean pinEnabled, // PIN 사용 여부
            Boolean biometricEnabled // 생체인증 사용 여부
    ) {
        this.pinEnabledYn = toYn(pinEnabled);
        this.biometricEnabledYn = toYn(biometricEnabled);
    }

    // Y/N 값 변환
    private static String toYn(
            Boolean value // 원본 Boolean 값
    ) {
        return Boolean.TRUE.equals(value) ? YES : NO;
    }
}
