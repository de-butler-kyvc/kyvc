package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminRole;
import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.domain.AdminUserRole;
import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.kyvc.backendadmin.global.util.KyvcEnums;

import java.util.List;
import java.util.Optional;

// EntityManager 기반 관리자 Repository 구현체
/**
 * {@link AdminUserRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>admin_users, admin_roles, admin_user_roles, audit_logs 테이블의 기본 저장과
 * 단건 조회를 담당합니다. 목록 검색과 조인 기반 페이징은
 * {@link AdminUserQueryRepository} 구현체로 분리합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class AdminUserRepositoryImpl implements AdminUserRepository {

    // 테스트에서 JPA 자동 구성이 제외되어도 컨텍스트가 뜨도록 지연 조회
    private final ObjectProvider<EntityManager> entityManagerProvider;

    // 관리자 ID로 admin_users 조회
    @Override
    public Optional<AdminUser> findById(Long adminId) {
        return Optional.ofNullable(entityManager().find(AdminUser.class, adminId));
    }

    // 신규 관리자 저장
    @Override
    public AdminUser save(AdminUser adminUser) {
        entityManager().persist(adminUser);
        return adminUser;
    }

    // 관리자 권한 매핑 저장
    @Override
    public AdminUserRole saveUserRole(AdminUserRole adminUserRole) {
        entityManager().persist(adminUserRole);
        return adminUserRole;
    }

    // 감사 로그 저장
    @Override
    public AuditLog saveAuditLog(AuditLog auditLog) {
        entityManager().persist(auditLog);
        return auditLog;
    }

    // 이메일 대소문자를 구분하지 않고 관리자 조회
    @Override
    public Optional<AdminUser> findByEmail(String email) {
        List<AdminUser> result = entityManager()
                .createQuery("""
                        select adminUser
                        from AdminUser adminUser
                        where lower(adminUser.email) = lower(:email)
                        """, AdminUser.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    // 이메일 존재 여부 확인
    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager()
                .createQuery("""
                        select count(adminUser)
                        from AdminUser adminUser
                        where lower(adminUser.email) = lower(:email)
                        """, Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    // 권한 ID 목록으로 admin_roles 조회
    @Override
    public List<AdminRole> findRolesByIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        return entityManager()
                .createQuery("""
                        select role
                        from AdminRole role
                        where role.roleId in :roleIds
                        """, AdminRole.class)
                .setParameter("roleIds", roleIds)
                .getResultList();
    }

    // admin_user_roles와 admin_roles를 조인해 Spring Security 권한 문자열로 변환
    @Override
    public List<String> findRoleCodesByAdminId(Long adminId) {
        return entityManager()
                .createQuery("""
                        select role.roleCode
                        from AdminUserRole userRole
                        join userRole.adminRole role
                        where userRole.adminUser.adminId = :adminId
                        """, KyvcEnums.RoleCode.class)
                .setParameter("adminId", adminId)
                .getResultList()
                .stream()
                .map(roleCode -> "ROLE_" + roleCode.name())
                .toList();
    }

    // 실제 DB 접근 시점에 EntityManager를 가져옴
    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
