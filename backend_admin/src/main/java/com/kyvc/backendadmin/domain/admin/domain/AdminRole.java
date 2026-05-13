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

// Backend Admin 권한 마스터 엔티티
/**
 * admin_roles 테이블과 매핑되는 Backend Admin 권한 마스터 엔티티입니다.
 *
 * <p>관리자 권한 코드와 표시 이름을 저장합니다.</p>
 */
@Entity
@Table(name = "admin_roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminRole {

    // admin_roles 기본 키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    // BACKEND_ADMIN, AUDITOR 등 권한 코드
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private KyvcEnums.RoleCode roleCode;

    // 관리자 화면에 표시할 권한 이름
    @Column(name = "role_name", length = 100)
    private String roleName;

    // 권한 그룹 설명
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 권한 사용 상태, ACTIVE인 권한만 부여 가능
    @Column(name = "status", length = 50)
    private String status;

    // 권한 그룹 생성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 권한 그룹 수정 시각
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 신규 관리자 권한 그룹 엔티티를 생성합니다.
     *
     * @param roleCode 권한 그룹 코드
     * @param roleName 권한 그룹명
     * @param description 권한 설명
     * @param status 권한 상태
     * @return 신규 관리자 권한 그룹 엔티티
     */
    public static AdminRole create(
            KyvcEnums.RoleCode roleCode,
            String roleName,
            String description,
            String status
    ) {
        AdminRole adminRole = new AdminRole();
        adminRole.roleCode = roleCode;
        adminRole.roleName = roleName;
        adminRole.description = description;
        adminRole.status = status;
        return adminRole;
    }

    /**
     * 관리자 권한 그룹 정보를 수정합니다.
     *
     * @param roleName 권한 그룹명
     * @param description 권한 설명
     * @param status 권한 상태
     */
    public void update(
            String roleName,
            String description,
            String status
    ) {
        this.roleName = roleName;
        this.description = description;
        this.status = status;
    }

    /**
     * 권한이 부여 가능한 활성 상태인지 확인합니다.
     *
     * @return status가 비어 있거나 ACTIVE이면 true
     */
    public boolean isActive() {
        return status == null || status.isBlank() || "ACTIVE".equals(status);
    }
}
