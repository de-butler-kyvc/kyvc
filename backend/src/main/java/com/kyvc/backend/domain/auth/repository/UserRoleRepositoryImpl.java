package com.kyvc.backend.domain.auth.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 사용자 보유 역할 Repository 구현체
@Repository
@RequiredArgsConstructor
public class UserRoleRepositoryImpl implements UserRoleRepository {

    private final EntityManager entityManager;

    // 사용자 보유 활성 역할 존재 여부
    @Override
    public boolean existsEnabledUserRole(
            Long userId, // 사용자 ID
            String roleCode // 역할 코드
    ) {
        Number count = (Number) entityManager.createNativeQuery("""
                        select count(*)
                        from user_roles user_role
                        join roles role on role.role_id = user_role.role_id
                        where user_role.user_id = :userId
                          and role.role_code = :roleCode
                          and user_role.enabled_yn = 'Y'
                          and role.enabled_yn = 'Y'
                        """)
                .setParameter("userId", userId)
                .setParameter("roleCode", roleCode)
                .getSingleResult();
        return count.longValue() > 0;
    }
}
