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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// Backend Admin 관리자 계정 엔티티
/**
 * admin_users 테이블과 매핑되는 Backend Admin 관리자 계정 엔티티입니다.
 *
 * <p>관리자 로그인 이메일, BCrypt 비밀번호 해시, 계정 상태, 표시 이름,
 * 마지막 로그인 시각을 저장합니다.</p>
 */
@Entity
@Table(name = "admin_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUser {

    // admin_users 기본 키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    // 관리자 로그인 이메일
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // BCrypt로 저장된 비밀번호 해시
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // 관리자 계정 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private KyvcEnums.AdminUserStatus status;

    // 화면 표시용 관리자 이름
    @Column(name = "display_name", length = 100)
    private String displayName;

    // 마지막 로그인 시각
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 관리자 계정 생성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 관리자 계정 수정 시각
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 로그인 가능한 활성 상태인지 확인
    public boolean isActive() {
        return KyvcEnums.AdminUserStatus.ACTIVE == status;
    }

    // 잠금 상태인지 확인
    public boolean isLocked() {
        return KyvcEnums.AdminUserStatus.LOCKED == status;
    }

    // 로그인 성공 시 마지막 로그인 시각 갱신
    public void updateLastLoginAt(LocalDateTime loginAt) {
        this.lastLoginAt = loginAt;
    }

    // BCrypt 해시로 비밀번호 변경
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // 신규 관리자 계정 생성
    /**
     * 신규 관리자 계정 엔티티를 생성합니다.
     *
     * @param email 관리자 이메일
     * @param displayName 관리자 표시 이름
     * @param passwordHash BCrypt 비밀번호 해시
     * @param status 관리자 계정 상태
     * @return 신규 관리자 계정 엔티티
     */
    public static AdminUser create(
            String email, // 관리자 이메일
            String displayName, // 관리자 표시 이름
            String passwordHash, // BCrypt 비밀번호 해시
            KyvcEnums.AdminUserStatus status // 관리자 상태
    ) {
        AdminUser adminUser = new AdminUser();
        adminUser.email = email;
        adminUser.displayName = displayName;
        adminUser.passwordHash = passwordHash;
        adminUser.status = status;
        return adminUser;
    }

    // 관리자 표시 이름과 상태 수정
    /**
     * 관리자 표시 이름과 상태를 수정합니다.
     *
     * @param displayName 변경할 표시 이름
     * @param status 변경할 관리자 상태
     */
    public void update(
            String displayName, // 변경할 표시 이름
            KyvcEnums.AdminUserStatus status // 변경할 상태
    ) {
        this.displayName = displayName;
        this.status = status;
    }
}
