package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.domain.AdminRole;
import com.kyvc.backendadmin.domain.admin.domain.AdminUserRole;
import com.kyvc.backendadmin.domain.admin.domain.AuditLog;

import java.util.List;
import java.util.Optional;

// 관리자 계정과 권한 조회 Repository 계약
/**
 * 관리자 계정 관리의 기본 저장 책임을 가지는 Repository입니다.
 *
 * <p>admin_users, admin_roles, admin_user_roles, audit_logs 테이블에 대한
 * 단건 조회, 저장, 권한 조회, 감사 로그 저장을 담당합니다.</p>
 */
public interface AdminUserRepository {

    // 관리자 ID로 단건 조회
    /**
     * 관리자 ID로 관리자 계정을 조회합니다.
     *
     * @param adminId 조회할 관리자 ID
     * @return 관리자 계정 Optional
     */
    Optional<AdminUser> findById(Long adminId);

    // 신규 관리자 저장
    /**
     * 신규 관리자 계정을 저장합니다.
     *
     * @param adminUser 저장할 관리자 계정 엔티티
     * @return 저장된 관리자 계정 엔티티
     */
    AdminUser save(AdminUser adminUser);

    // 관리자 권한 매핑 저장
    /**
     * 관리자와 권한의 매핑 정보를 저장합니다.
     *
     * @param adminUserRole 저장할 관리자 권한 매핑 엔티티
     * @return 저장된 관리자 권한 매핑 엔티티
     */
    AdminUserRole saveUserRole(AdminUserRole adminUserRole);

    // 감사 로그 저장
    /**
     * 관리자 계정 변경 감사 로그를 저장합니다.
     *
     * @param auditLog 저장할 감사 로그 엔티티
     * @return 저장된 감사 로그 엔티티
     */
    AuditLog saveAuditLog(AuditLog auditLog);

    // 이메일로 관리자 단건 조회
    /**
     * 이메일로 관리자 계정을 조회합니다.
     *
     * @param email 조회할 관리자 이메일
     * @return 관리자 계정 Optional
     */
    Optional<AdminUser> findByEmail(String email);

    // 이메일 존재 여부 확인
    /**
     * 이메일 중복 여부를 확인합니다.
     *
     * @param email 확인할 관리자 이메일
     * @return 이미 존재하면 true
     */
    boolean existsByEmail(String email);

    // 권한 ID 목록으로 권한 엔티티 조회
    /**
     * 권한 ID 목록으로 권한 엔티티를 조회합니다.
     *
     * @param roleIds 조회할 권한 ID 목록
     * @return 조회된 권한 엔티티 목록
     */
    List<AdminRole> findRolesByIds(List<Long> roleIds);

    // 관리자에게 할당된 ROLE_ 접두사 포함 권한 문자열 목록 조회
    /**
     * 관리자에게 할당된 권한 코드를 Spring Security 권한 문자열로 조회합니다.
     *
     * @param adminId 권한을 조회할 관리자 ID
     * @return ROLE_ 접두사가 포함된 권한 문자열 목록
     */
    List<String> findRoleCodesByAdminId(Long adminId);
}
