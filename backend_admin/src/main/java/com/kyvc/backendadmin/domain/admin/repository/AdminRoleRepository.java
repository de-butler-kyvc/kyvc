package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminRole;
import com.kyvc.backendadmin.global.util.KyvcEnums;

import java.util.List;
import java.util.Optional;

/**
 * 관리자 권한 조회 Repository입니다.
 *
 * <p>admin_roles 테이블을 기준으로 권한 목록과 단건 권한 조회 책임을 가집니다.</p>
 */
public interface AdminRoleRepository {

    /**
     * admin_roles 테이블의 전체 권한 목록을 조회합니다.
     *
     * @return 권한 엔티티 목록
     */
    List<AdminRole> findAll();

    /**
     * 권한 ID로 권한을 조회합니다.
     *
     * @param roleId 조회할 권한 ID
     * @return 권한 엔티티 Optional
     */
    Optional<AdminRole> findById(Long roleId);

    /**
     * 신규 관리자 권한 그룹을 저장합니다.
     *
     * @param adminRole 저장할 관리자 권한 그룹 엔티티
     * @return 저장된 관리자 권한 그룹 엔티티
     */
    AdminRole save(AdminRole adminRole);

    /**
     * 관리자 권한 코드 중복 여부를 확인합니다.
     *
     * @param roleCode 확인할 관리자 권한 코드
     * @return 이미 존재하면 true
     */
    boolean existsByRoleCode(KyvcEnums.RoleCode roleCode);
}
