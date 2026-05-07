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

    // 권한 사용 상태, ACTIVE인 권한만 부여 가능
    @Column(name = "status", length = 50)
    private String status;

    /**
     * 권한이 부여 가능한 활성 상태인지 확인합니다.
     *
     * @return status가 비어 있거나 ACTIVE이면 true
     */
    public boolean isActive() {
        return status == null || status.isBlank() || "ACTIVE".equals(status);
    }
}
