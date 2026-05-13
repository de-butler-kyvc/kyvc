package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminUserRole;
import com.kyvc.backendadmin.global.util.KyvcEnums;

import java.util.Optional;

/**
 * 관리자-권한 매핑 관리 Repository입니다.
 *
 * <p>admin_user_roles 테이블을 기준으로 매핑 존재 여부 확인, 권한 매핑 저장,
 * 권한 매핑 삭제와 SYSTEM_ADMIN 보유자 수 조회 책임을 가집니다.</p>
 */
public interface AdminUserRoleRepository {

    /**
     * 관리자 ID와 권한 ID로 매핑을 조회합니다.
     *
     * @param adminUserId 조회할 관리자 ID
     * @param roleId 조회할 권한 ID
     * @return 관리자 권한 매핑 Optional
     */
    Optional<AdminUserRole> findByAdminUserIdAndRoleId(Long adminUserId, Long roleId);

    /**
     * 관리자 ID와 권한 ID로 매핑 존재 여부를 확인합니다.
     *
     * @param adminUserId 확인할 관리자 ID
     * @param roleId 확인할 권한 ID
     * @return 매핑이 존재하면 true
     */
    boolean existsByAdminUserIdAndRoleId(Long adminUserId, Long roleId);

    /**
     * 관리자 권한 매핑을 저장합니다.
     *
     * @param adminUserRole 저장할 관리자 권한 매핑 엔티티
     * @return 저장된 관리자 권한 매핑 엔티티
     */
    AdminUserRole save(AdminUserRole adminUserRole);

    /**
     * 관리자 권한 매핑을 삭제합니다.
     *
     * @param adminUserRole 삭제할 관리자 권한 매핑 엔티티
     */
    void delete(AdminUserRole adminUserRole);

    /**
     * 특정 권한 코드를 보유한 관리자 수를 조회합니다.
     *
     * @param roleCode 조회할 권한 코드
     * @return 해당 권한을 보유한 관리자 수
     */
    long countByRoleCode(KyvcEnums.RoleCode roleCode);
}
