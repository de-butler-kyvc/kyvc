package com.kyvc.backend.domain.mobile.domain;

import com.kyvc.backend.domain.mobile.dto.MobileDeviceRegisterRequest;
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

// 모바일 기기 바인딩 Entity
@Entity
@Table(name = "mobile_device_bindings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MobileDeviceBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_binding_id")
    private Long deviceBindingId; // 기기 바인딩 ID

    @Column(name = "user_id", nullable = false)
    private Long userId; // 사용자 ID

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId; // 모바일 기기 ID

    @Column(name = "device_name", length = 255)
    private String deviceName; // 모바일 기기명

    @Column(name = "os", length = 50)
    private String os; // 모바일 운영체제 정보

    @Column(name = "app_version", length = 50)
    private String appVersion; // 앱 버전

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey; // 기기 공개키

    @Enumerated(EnumType.STRING)
    @Column(name = "device_binding_status_code", nullable = false, length = 50)
    private KyvcEnums.DeviceBindingStatus deviceBindingStatus; // 기기 바인딩 상태

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt; // 등록 일시

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt; // 마지막 사용 일시

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정 일시

    // 모바일 기기 바인딩 생성
    public static MobileDeviceBinding create(
            Long userId, // 사용자 ID
            MobileDeviceRegisterRequest request, // 모바일 기기 등록 요청
            LocalDateTime now // 기준 일시
    ) {
        MobileDeviceBinding deviceBinding = new MobileDeviceBinding();
        deviceBinding.userId = userId;
        deviceBinding.deviceId = request.deviceId();
        deviceBinding.deviceName = request.deviceName();
        deviceBinding.os = request.os();
        deviceBinding.appVersion = request.appVersion();
        deviceBinding.publicKey = request.publicKey();
        deviceBinding.deviceBindingStatus = KyvcEnums.DeviceBindingStatus.ACTIVE;
        deviceBinding.registeredAt = now;
        deviceBinding.lastUsedAt = now;
        return deviceBinding;
    }

    // 소유자 여부
    public boolean isOwner(
            Long userId // 사용자 ID
    ) {
        return this.userId != null && this.userId.equals(userId);
    }

    // 활성 여부
    public boolean isActive() {
        return KyvcEnums.DeviceBindingStatus.ACTIVE == deviceBindingStatus;
    }

    // 마지막 사용 일시 갱신
    public void updateLastUsedAt(
            LocalDateTime now // 기준 일시
    ) {
        this.lastUsedAt = now;
    }

    // 기기 정보 갱신
    public void updateDeviceInfo(
            MobileDeviceRegisterRequest request, // 모바일 기기 등록 요청
            LocalDateTime now // 기준 일시
    ) {
        this.deviceId = request.deviceId();
        this.deviceName = request.deviceName();
        this.os = request.os();
        this.appVersion = request.appVersion();
        this.publicKey = request.publicKey();
        this.deviceBindingStatus = KyvcEnums.DeviceBindingStatus.ACTIVE;
        this.lastUsedAt = now;
    }
}
