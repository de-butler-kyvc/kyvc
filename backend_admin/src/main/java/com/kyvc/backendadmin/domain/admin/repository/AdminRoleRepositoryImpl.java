package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminRole;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * {@link AdminRoleRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>admin_roles 테이블의 권한 목록과 권한 ID 기반 단건 조회를 담당합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class AdminRoleRepositoryImpl implements AdminRoleRepository {

    // 테스트에서 JPA 자동 구성이 제외되어도 컨텍스트가 뜨도록 지연 조회
    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AdminRole> findAll() {
        return entityManager()
                .createQuery("""
                        select role
                        from AdminRole role
                        order by role.roleId asc
                        """, AdminRole.class)
                .getResultList();
    }

    @Override
    public Optional<AdminRole> findById(Long roleId) {
        return Optional.ofNullable(entityManager().find(AdminRole.class, roleId));
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
