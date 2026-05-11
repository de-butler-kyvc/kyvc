package com.kyvc.backend.domain.user.domain;

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

// 사용자 Entity
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

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "onboarding_corporate_name", length = 255)
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
        return createCorporateUser(email, passwordHash, null, null, null);
    }

    // 법인 사용자 생성
    public static User createCorporateUser(
            String email, // 사용자 이메일
            String passwordHash, // 비밀번호 해시
            String userName, // 사용자명
            String phone // 사용자 연락처
    ) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.userName = userName;
        user.phone = phone;
        user.notificationEnabledYn = KyvcEnums.Yn.Y;
        user.mfaEnabledYn = KyvcEnums.Yn.N;
        user.lastPasswordChangedAt = LocalDateTime.now();
        user.userTypeCode = KyvcEnums.UserType.CORPORATE_USER;
        user.userStatusCode = KyvcEnums.UserStatus.ACTIVE;
        return user;
    }

    // 로그인 가능 상태 여부
    // 회원가입 온보딩 법인명 포함 법인 사용자 생성
    public static User createCorporateUser(
            String email, // 사용자 이메일
            String passwordHash, // 비밀번호 해시
            String userName, // 사용자명
            String phone, // 사용자 연락처
            String onboardingCorporateName // 회원가입 온보딩 단계 입력 법인명
    ) {
        User user = createCorporateUser(email, passwordHash, userName, phone);
        user.onboardingCorporateName = onboardingCorporateName;
        return user;
    }

    public boolean isActive() {
        return KyvcEnums.UserStatus.ACTIVE == userStatusCode;
    }

    // 비밀번호 해시 변경
    public void changePasswordHash(
            String passwordHash // 신규 비밀번호 해시
    ) {
        this.passwordHash = passwordHash;
        this.lastPasswordChangedAt = LocalDateTime.now();
    }

    // 기본 정보 변경
    public void updateProfile(
            String userName, // 사용자명
            String phone, // 사용자 연락처
            KyvcEnums.Yn notificationEnabledYn // 알림 수신 여부
    ) {
        this.userName = userName;
        this.phone = phone;
        this.notificationEnabledYn = notificationEnabledYn;
    }

    // MFA 설정 변경
    public void updateMfaEnabled(
            KyvcEnums.Yn mfaEnabledYn // MFA 사용 여부
    ) {
        this.mfaEnabledYn = mfaEnabledYn;
    }
}
