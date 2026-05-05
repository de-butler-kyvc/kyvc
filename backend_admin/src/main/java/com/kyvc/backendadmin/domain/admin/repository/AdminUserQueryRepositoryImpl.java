package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserSearchRequest;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// EntityManager 기반 관리자 검색 QueryRepository 구현체
/**
 * {@link AdminUserQueryRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>admin_users 목록 조회에서 keyword, status, roleCode 조건을 동적 JPQL로 구성하고,
 * 권한 조건이 있을 때 admin_user_roles와 admin_roles 조인을 수행합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class AdminUserQueryRepositoryImpl implements AdminUserQueryRepository {

    // 테스트에서 JPA 자동 구성이 제외되어도 컨텍스트가 뜨도록 지연 조회
    private final ObjectProvider<EntityManager> entityManagerProvider;

    // 검색 조건과 권한 조건을 적용해 관리자 목록 조회
    @Override
    public List<AdminUser> search(AdminUserSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        TypedQuery<AdminUser> query = entityManager()
                .createQuery("""
                        select distinct adminUser
                        from AdminUser adminUser
                        %s
                        %s
                        order by adminUser.createdAt desc, adminUser.adminId desc
                        """.formatted(queryParts.joinClause(), queryParts.whereClause()), AdminUser.class);
        bindParameters(query, queryParts.parameters());
        return query
                .setFirstResult(request.page() * request.size())
                .setMaxResults(request.size())
                .getResultList();
    }

    // 검색 조건과 권한 조건을 적용해 전체 건수 조회
    @Override
    public long count(AdminUserSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        TypedQuery<Long> query = entityManager()
                .createQuery("""
                        select count(distinct adminUser.adminId)
                        from AdminUser adminUser
                        %s
                        %s
                        """.formatted(queryParts.joinClause(), queryParts.whereClause()), Long.class);
        bindParameters(query, queryParts.parameters());
        return query.getSingleResult();
    }

    // 동적 JPQL 구성 요소 생성
    private QueryParts buildQueryParts(AdminUserSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();
        boolean joinRole = StringUtils.hasText(request.roleCode());

        if (StringUtils.hasText(request.keyword())) {
            where.append(" and (lower(adminUser.email) like :keyword or lower(adminUser.displayName) like :keyword)");
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.status())) {
            where.append(" and adminUser.status = :status");
            parameters.put("status", KyvcEnums.AdminUserStatus.valueOf(request.status()));
        }
        if (joinRole) {
            where.append(" and role.roleCode = :roleCode");
            parameters.put("roleCode", KyvcEnums.RoleCode.valueOf(request.roleCode()));
        }

        String join = joinRole
                ? "join AdminUserRole userRole on userRole.adminUser = adminUser join userRole.adminRole role"
                : "";
        return new QueryParts(join, where.toString(), parameters);
    }

    // JPQL 파라미터 바인딩
    private void bindParameters(TypedQuery<?> query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    // 실제 DB 접근 시점에 EntityManager를 가져옴
    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }

    // 동적 JPQL 구성 결과
    private record QueryParts(
            String joinClause, // join 절
            String whereClause, // where 절
            Map<String, Object> parameters // 바인딩 파라미터
    ) {
    }
}
