package com.kyvc.backendadmin.domain.core.repository;

import com.kyvc.backendadmin.global.util.KyvcEnums;

import java.time.LocalDateTime;

/**
 * Core 요청 저장 Repository입니다.
 */
public interface CoreRequestRepository {

    /**
     * core_requests 테이블에 Core 요청 row를 생성합니다.
     *
     * @param coreRequestId Core 요청 ID
     * @param requestType Core 요청 유형
     * @param targetType Core 요청 대상 유형
     * @param targetId Core 요청 대상 ID
     * @param status Core 요청 상태
     * @param requestPayloadJson 요청 payload JSON
     * @param requestedAt 요청 시각
     * @return 생성된 row 수
     */
    int save(
            String coreRequestId,
            KyvcEnums.CoreRequestType requestType,
            KyvcEnums.CoreTargetType targetType,
            Long targetId,
            KyvcEnums.CoreRequestStatus status,
            String requestPayloadJson,
            LocalDateTime requestedAt
    );
}
