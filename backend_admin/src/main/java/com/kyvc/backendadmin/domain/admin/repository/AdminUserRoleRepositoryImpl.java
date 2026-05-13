package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminUserRole;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@link AdminUserRoleRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>admin_user_roles 테이블의 매핑 조회, 저장, 삭제와 admin_roles 조인 기반
 * 권한 코드별 보유 관리자 수 조회를 담당합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class AdminUserRoleRepositoryImpl implements AdminUserRoleRepository {

    // 테스트에서 JPA 자동 구성이 제외되어도 컨텍스트가 뜨도록 지연 조회
    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public Optional<AdminUserRole> findByAdminUserIdAndRoleId(Long adminUserId, Long roleId) {
        if (adminUserId == null || roleId == null) {
            return Optional.empty();
        }

        List<AdminUserRole> result = entityManager()
                .createQuery("""
                        select userRole
                        from AdminUserRole userRole
                        join fetch userRole.adminRole role
                        join fetch userRole.adminUser adminUser
                        where adminUser.adminId = :adminUserId
                          and role.roleId = :roleId
                        """, AdminUserRole.class)
                .setParameter("adminUserId", adminUserId)
                .setParameter("roleId", roleId)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public boolean existsByAdminUserIdAndRoleId(Long adminUserId, Long roleId) {
        if (adminUserId == null || roleId == null) {
            return false;
        }

        Long count = entityManager()
                .createQuery("""
                        select count(userRole)
                        from AdminUserRole userRole
                        where userRole.adminUser.adminId = :adminUserId
                          and userRole.adminRole.roleId = :roleId
                        """, Long.class)
                .setParameter("adminUserId", adminUserId)
                .setParameter("roleId", roleId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public AdminUserRole save(AdminUserRole adminUserRole) {
        entityManager().persist(adminUserRole);
        return adminUserRole;
    }

    @Override
    public void delete(AdminUserRole adminUserRole) {
        entityManager().remove(adminUserRole);
    }

    @Override
    public long countByRoleCode(KyvcEnums.RoleCode roleCode) {
        if (roleCode == null) {
            return 0;
        }

        return entityManager()
                .createQuery("""
                        select count(distinct userRole.adminUser.adminId)
                        from AdminUserRole userRole
                        where userRole.adminRole.roleCode = :roleCode
                        """, Long.class)
                .setParameter("roleCode", roleCode)
                .getSingleResult();
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
