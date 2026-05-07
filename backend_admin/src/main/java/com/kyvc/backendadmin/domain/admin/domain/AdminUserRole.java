package com.kyvc.backendadmin.domain.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 관리자와 권한의 다대다 매핑 엔티티
/**
 * admin_user_roles 테이블과 매핑되는 관리자-권한 매핑 엔티티입니다.
 *
 * <p>admin_users와 admin_roles 사이의 다대다 관계를 표현합니다.</p>
 */
@Entity
@Table(name = "admin_user_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUserRole {

    // admin_user_roles 기본 키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_user_role_id")
    private Long adminUserRoleId;

    // 권한이 할당된 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminUser adminUser;

    // 관리자에게 할당된 권한
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole adminRole;

    // 관리자와 권한 매핑 생성
    /**
     * 관리자와 권한의 매핑 엔티티를 생성합니다.
     *
     * @param adminUser 권한을 부여할 관리자
     * @param adminRole 부여할 권한
     * @return 관리자 권한 매핑 엔티티
     */
    public static AdminUserRole create(
            AdminUser adminUser, // 권한을 부여할 관리자
            AdminRole adminRole // 부여할 권한
    ) {
        AdminUserRole adminUserRole = new AdminUserRole();
        adminUserRole.adminUser = adminUser;
        adminUserRole.adminRole = adminRole;
        return adminUserRole;
    }
}
