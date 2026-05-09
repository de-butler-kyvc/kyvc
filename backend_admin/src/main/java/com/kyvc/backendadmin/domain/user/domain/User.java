package com.kyvc.backendadmin.domain.user.domain;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 사용자 엔티티
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "onboarding_corporate_name")
    private String onboardingCorporateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_enabled_yn", length = 1)
    private KyvcEnums.Yn notificationEnabledYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_enabled_yn", length = 1)
    private KyvcEnums.Yn mfaEnabledYn;

    @Column(name = "mfa_type_code", length = 30)
    private String mfaTypeCode;

    @Column(name = "last_password_changed_at")
    private LocalDateTime lastPasswordChangedAt;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type_code", nullable = false, length = 50)
    private KyvcEnums.UserType userTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status_code", nullable = false, length = 50)
    private KyvcEnums.UserStatus userStatusCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 법인 사용자 생성
    public static User createCorporateUser(
            String email, // 사용자 이메일
            String passwordHash // 비밀번호 해시
    ) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.notificationEnabledYn = KyvcEnums.Yn.Y;
        user.mfaEnabledYn = KyvcEnums.Yn.N;
        user.lastPasswordChangedAt = LocalDateTime.now();
        user.userTypeCode = KyvcEnums.UserType.CORPORATE_USER;
        user.userStatusCode = KyvcEnums.UserStatus.ACTIVE;
        return user;
    }

    // 로그인 가능 상태 여부
    public boolean isActive() {
        return KyvcEnums.UserStatus.ACTIVE == userStatusCode;
    }

    /**
     * 관리자 상태 변경 요청에 따라 사용자 계정 상태를 변경합니다.
     *
     * @param status 변경할 사용자 상태
     */
    public void changeStatus(KyvcEnums.UserStatus status) {
        this.userStatusCode = status;
    }
}
