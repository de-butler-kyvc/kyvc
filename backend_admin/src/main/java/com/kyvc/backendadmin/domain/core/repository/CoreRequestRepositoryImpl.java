package com.kyvc.backendadmin.domain.core.repository;

import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * {@link CoreRequestRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class CoreRequestRepositoryImpl implements CoreRequestRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public int save(
            String coreRequestId,
            KyvcEnums.CoreRequestType requestType,
            KyvcEnums.CoreTargetType targetType,
            Long targetId,
            KyvcEnums.CoreRequestStatus status,
            String requestPayloadJson,
            LocalDateTime requestedAt
    ) {
        return entityManager().createNativeQuery("""
                        insert into core_requests (
                            core_request_id,
                            core_request_type_code,
                            core_target_type_code,
                            target_id,
                            core_request_status_code,
                            request_payload_json,
                            requested_at,
                            created_at,
                            updated_at
                        ) values (
                            :coreRequestId,
                            :requestType,
                            :targetType,
                            :targetId,
                            :status,
                            :requestPayloadJson,
                            :requestedAt,
                            :requestedAt,
                            :requestedAt
                        )
                        """)
                .setParameter("coreRequestId", coreRequestId)
                .setParameter("requestType", requestType.name())
                .setParameter("targetType", targetType.name())
                .setParameter("targetId", targetId)
                .setParameter("status", status.name())
                .setParameter("requestPayloadJson", requestPayloadJson)
                .setParameter("requestedAt", requestedAt)
                .executeUpdate();
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
