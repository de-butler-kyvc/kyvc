package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminRole;

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
}
