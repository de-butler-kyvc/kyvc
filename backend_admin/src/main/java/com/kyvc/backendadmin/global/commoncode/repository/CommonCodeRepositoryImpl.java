package com.kyvc.backendadmin.global.commoncode.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.global.commoncode.domain.CommonCode;
import com.kyvc.backendadmin.global.commoncode.domain.CommonCodeGroup;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupSearchRequest;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeSearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommonCodeRepositoryImpl implements CommonCodeRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<CommonCodeGroup> searchGroups(CommonCodeGroupSearchRequest request) {
        QueryParts queryParts = buildGroupQueryParts(request);
        TypedQuery<CommonCodeGroup> query = entityManager().createQuery("""
                select codeGroup
                from CommonCodeGroup codeGroup
                %s
                order by codeGroup.sortOrder asc, codeGroup.codeGroup asc
                """.formatted(queryParts.whereClause()), CommonCodeGroup.class);
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        return query.getResultList();
    }

    @Override
    public long countGroups(CommonCodeGroupSearchRequest request) {
        QueryParts queryParts = buildGroupQueryParts(request);
        TypedQuery<Long> query = entityManager().createQuery("""
                select count(codeGroup)
                from CommonCodeGroup codeGroup
                %s
                """.formatted(queryParts.whereClause()), Long.class);
        bindParameters(query, queryParts.parameters());
        return query.getSingleResult();
    }

    @Override
    public Optional<CommonCodeGroup> findGroupById(Long codeGroupId) {
        return Optional.ofNullable(entityManager().find(CommonCodeGroup.class, codeGroupId));
    }

    @Override
    public Optional<CommonCodeGroup> findGroupByCodeGroup(String codeGroup) {
        List<CommonCodeGroup> result = entityManager()
                .createQuery("""
                        select commonCodeGroup
                        from CommonCodeGroup commonCodeGroup
                        where commonCodeGroup.codeGroup = :codeGroup
                        """, CommonCodeGroup.class)
                .setParameter("codeGroup", codeGroup)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public List<CommonCode> searchCodes(CommonCodeSearchRequest request) {
        QueryParts queryParts = buildCodeQueryParts(request);
        TypedQuery<CommonCode> query = entityManager().createQuery("""
                select code
                from CommonCode code
                join fetch code.codeGroup codeGroup
                %s
                order by codeGroup.codeGroup asc, code.sortOrder asc, code.code asc
                """.formatted(queryParts.whereClause()), CommonCode.class);
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        return query.getResultList();
    }

    @Override
    public long countCodes(CommonCodeSearchRequest request) {
        QueryParts queryParts = buildCodeQueryParts(request);
        TypedQuery<Long> query = entityManager().createQuery("""
                select count(code)
                from CommonCode code
                join code.codeGroup codeGroup
                %s
                """.formatted(queryParts.whereClause()), Long.class);
        bindParameters(query, queryParts.parameters());
        return query.getSingleResult();
    }

    @Override
    public Optional<CommonCode> findCodeById(Long codeId) {
        List<CommonCode> result = entityManager()
                .createQuery("""
                        select code
                        from CommonCode code
                        join fetch code.codeGroup codeGroup
                        where code.codeId = :codeId
                        """, CommonCode.class)
                .setParameter("codeId", codeId)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    @Override
    public boolean existsCodeByGroupIdAndCode(Long codeGroupId, String code) {
        Long count = entityManager()
                .createQuery("""
                        select count(commonCode)
                        from CommonCode commonCode
                        where commonCode.codeGroup.codeGroupId = :codeGroupId
                          and commonCode.code = :code
                        """, Long.class)
                .setParameter("codeGroupId", codeGroupId)
                .setParameter("code", code)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsEnabledCode(String codeGroup, String code) {
        Long count = entityManager()
                .createQuery("""
                        select count(commonCode)
                        from CommonCode commonCode
                        join commonCode.codeGroup commonCodeGroup
                        where commonCodeGroup.codeGroup = :codeGroup
                          and commonCodeGroup.enabledYn = 'Y'
                          and commonCode.code = :code
                          and commonCode.enabledYn = 'Y'
                        """, Long.class)
                .setParameter("codeGroup", codeGroup)
                .setParameter("code", code)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public CommonCode saveCode(CommonCode commonCode) {
        entityManager().persist(commonCode);
        return commonCode;
    }

    @Override
    public void deleteCode(CommonCode commonCode) {
        entityManager().remove(commonCode);
    }

    @Override
    public AuditLog saveAuditLog(AuditLog auditLog) {
        entityManager().persist(auditLog);
        return auditLog;
    }

    private QueryParts buildGroupQueryParts(CommonCodeGroupSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();
        if (StringUtils.hasText(request.codeGroup())) {
            where.append(" and codeGroup.codeGroup = :codeGroup");
            parameters.put("codeGroup", request.codeGroup());
        }
        if (StringUtils.hasText(request.keyword())) {
            where.append("""
                     and (
                         lower(codeGroup.codeGroup) like :keyword
                         or lower(codeGroup.codeGroupName) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.enabledYn())) {
            where.append(" and codeGroup.enabledYn = :enabledYn");
            parameters.put("enabledYn", request.enabledYn());
        }
        return new QueryParts(where.toString(), parameters);
    }

    private QueryParts buildCodeQueryParts(CommonCodeSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();
        if (StringUtils.hasText(request.codeGroup())) {
            where.append(" and codeGroup.codeGroup = :codeGroup");
            parameters.put("codeGroup", request.codeGroup());
        }
        if (StringUtils.hasText(request.keyword())) {
            where.append("""
                     and (
                         lower(code.code) like :keyword
                         or lower(code.codeName) like :keyword
                     )
                    """);
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (StringUtils.hasText(request.enabledYn())) {
            where.append(" and code.enabledYn = :enabledYn");
            parameters.put("enabledYn", request.enabledYn());
        }
        return new QueryParts(where.toString(), parameters);
    }

    private void bindParameters(TypedQuery<?> query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }

    private record QueryParts(
            String whereClause,
            Map<String, Object> parameters
    ) {
    }
}
