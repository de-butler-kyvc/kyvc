package com.kyvc.backendadmin.domain.kyc.repository;

import com.kyvc.backendadmin.domain.kyc.domain.DocumentRequirement;
import com.kyvc.backendadmin.domain.kyc.dto.AdminDocumentRequirementSearchRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link DocumentRequirementQueryRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>document_requirements 목록 조회 조건을 동적 JPQL로 조립하여 검색과 카운트를 처리합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class DocumentRequirementQueryRepositoryImpl implements DocumentRequirementQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<DocumentRequirement> search(AdminDocumentRequirementSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        TypedQuery<DocumentRequirement> query = entityManager()
                .createQuery("""
                        select requirement
                        from DocumentRequirement requirement
                        %s
                        order by requirement.corporateTypeCode asc,
                                 requirement.sortOrder asc,
                                 requirement.requirementId desc
                        """.formatted(queryParts.whereClause()), DocumentRequirement.class);
        bindParameters(query, queryParts.parameters());
        return query
                .setFirstResult(request.page() * request.size())
                .setMaxResults(request.size())
                .getResultList();
    }

    @Override
    public long count(AdminDocumentRequirementSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        TypedQuery<Long> query = entityManager()
                .createQuery("""
                        select count(requirement)
                        from DocumentRequirement requirement
                        %s
                        """.formatted(queryParts.whereClause()), Long.class);
        bindParameters(query, queryParts.parameters());
        return query.getSingleResult();
    }

    private QueryParts buildQueryParts(AdminDocumentRequirementSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();

        if (StringUtils.hasText(request.corporateType())) {
            where.append(" and requirement.corporateTypeCode = :corporateType");
            parameters.put("corporateType", request.corporateType());
        }
        if (StringUtils.hasText(request.documentType())) {
            where.append(" and requirement.documentTypeCode = :documentType");
            parameters.put("documentType", request.documentType());
        }
        if (StringUtils.hasText(request.requiredYn())) {
            where.append(" and requirement.requiredYn = :requiredYn");
            parameters.put("requiredYn", request.requiredYn());
        }
        if (StringUtils.hasText(request.enabledYn())) {
            where.append(" and requirement.enabledYn = :enabledYn");
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
