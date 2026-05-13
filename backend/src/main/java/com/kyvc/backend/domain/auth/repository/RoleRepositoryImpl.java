package com.kyvc.backend.domain.auth.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 사용자 역할 Repository 구현체
@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final EntityManager entityManager;

    // 사용자 ID 기준 활성 역할 목록 조회
    @Override
    public List<RoleRow> findEnabledRolesByUserId(
            Long userId // 사용자 ID
    ) {
        Query query = entityManager.createNativeQuery("""
                select role.role_id,
                       role.role_code,
                       role.role_name,
                       role.role_type_code
                from user_roles user_role
                join roles role on role.role_id = user_role.role_id
                where user_role.user_id = :userId
                  and user_role.enabled_yn = 'Y'
                  and role.enabled_yn = 'Y'
                order by role.sort_order asc, role.role_id asc
                """);
        query.setParameter("userId", userId);
        return query.getResultList().stream()
                .map(row -> toRoleRow((Object[]) row))
                .toList();
    }

    // 역할 코드 기준 활성 역할 조회
    @Override
    public Optional<RoleRow> findEnabledByRoleCode(
            String roleCode // 역할 코드
    ) {
        Query query = entityManager.createNativeQuery("""
                select role.role_id,
                       role.role_code,
                       role.role_name,
                       role.role_type_code
                from roles role
                where role.role_code = :roleCode
                  and role.enabled_yn = 'Y'
                """);
        query.setParameter("roleCode", roleCode);
        return query.getResultList().stream()
                .findFirst()
                .map(row -> toRoleRow((Object[]) row));
    }

    // 역할 조회 행 변환
    private RoleRow toRoleRow(Object[] row // 역할 조회 원본 행
    ) {
        return new RoleRow(
                toLong(row[0]),
                toString(row[1]),
                toString(row[2]),
                toString(row[3])
        );
    }

    // Long 변환
    private Long toLong(Object value // 원본 값
    ) {
        return value == null ? null : ((Number) value).longValue();
    }

    // 문자열 변환
    private String toString(Object value // 원본 값
    ) {
        return value == null ? null : value.toString();
    }
}
