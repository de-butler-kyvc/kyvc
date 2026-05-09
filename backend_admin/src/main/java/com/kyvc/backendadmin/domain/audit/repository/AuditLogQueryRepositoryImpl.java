package com.kyvc.backendadmin.domain.audit.repository;

import com.kyvc.backendadmin.domain.audit.domain.AuditLog;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogSearchRequest;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
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

@Repository
@RequiredArgsConstructor
public class AuditLogQueryRepositoryImpl implements AuditLogQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public List<AuditLog> search(AdminAuditLogSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        TypedQuery<AuditLog> query = entityManager().createQuery("""
                select auditLog
                from AuditLogForAudit auditLog
                %s
                order by auditLog.createdAt desc, auditLog.auditLogId desc
                """.formatted(queryParts.whereClause()), AuditLog.class);
        bindParameters(query, queryParts.parameters());
        query.setFirstResult(request.page() * request.size());
        query.setMaxResults(request.size());
        return query.getResultList();
    }

    @Override
    public long count(AdminAuditLogSearchRequest request) {
        QueryParts queryParts = buildQueryParts(request);
        TypedQuery<Long> query = entityManager().createQuery("""
                select count(auditLog)
                from AuditLogForAudit auditLog
                %s
                """.formatted(queryParts.whereClause()), Long.class);
        bindParameters(query, queryParts.parameters());
        return query.getSingleResult();
    }

    private QueryParts buildQueryParts(AdminAuditLogSearchRequest request) {
        StringBuilder where = new StringBuilder("where 1 = 1");
        Map<String, Object> parameters = new HashMap<>();

        if (StringUtils.hasText(request.actorType())) {
            where.append(" and auditLog.actorType = :actorType");
            parameters.put("actorType", parseActorType(request.actorType()));
        }
        if (request.actorId() != null) {
            where.append(" and auditLog.actorId = :actorId");
            parameters.put("actorId", request.actorId());
        }
        if (StringUtils.hasText(request.actionType())) {
            where.append(" and auditLog.actionType = :actionType");
            parameters.put("actionType", request.actionType());
        }
        if (StringUtils.hasText(request.targetType())) {
            where.append(" and auditLog.targetType = :targetType");
            parameters.put("targetType", parseTargetType(request.targetType()));
        }
        if (request.targetId() != null) {
            where.append(" and auditLog.targetId = :targetId");
            parameters.put("targetId", request.targetId());
        }
        if (StringUtils.hasText(request.keyword())) {
            where.append(" and lower(coalesce(auditLog.requestSummary, '')) like :keyword");
            parameters.put("keyword", "%" + request.keyword().toLowerCase() + "%");
        }
        if (request.from() != null) {
            where.append(" and auditLog.createdAt >= :from");
            parameters.put("from", request.from());
        }
        if (request.to() != null) {
            where.append(" and auditLog.createdAt < :to");
            parameters.put("to", request.to());
        }
        return new QueryParts(where.toString(), parameters);
    }

    private KyvcEnums.ActorType parseActorType(String actorType) {
        try {
            return KyvcEnums.ActorType.valueOf(actorType);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 actorType입니다.");
        }
    }

    private KyvcEnums.AuditTargetType parseTargetType(String targetType) {
        try {
            return KyvcEnums.AuditTargetType.valueOf(targetType);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 targetType입니다.");
        }
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
