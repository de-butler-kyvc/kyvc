package com.kyvc.backend.domain.core.repository;

import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.Optional;

/**
 * CoreRequest Repository
 */
public interface CoreRequestRepository {

    /**
     * CoreRequest 저장
     *
     * @param coreRequest 저장 대상 CoreRequest
     * @return 저장된 CoreRequest
     */
    CoreRequest save(
            CoreRequest coreRequest // 저장 대상 CoreRequest
    );

    /**
     * CoreRequest ID 기준 조회
     *
     * @param coreRequestId Core 요청 ID
     * @return 조회된 CoreRequest
     */
    CoreRequest getById(
            String coreRequestId // Core 요청 ID
    );

    /**
     * CoreRequest ID 기준 Optional 조회
     *
     * @param coreRequestId Core 요청 ID
     * @return 조회 결과 Optional
     */
    Optional<CoreRequest> findById(
            String coreRequestId // Core 요청 ID
    );

    /**
     * 대상 기준 최신 CoreRequest 조회
     *
     * @param targetType Core 대상 유형
     * @param targetId 대상 ID
     * @param requestType Core 요청 유형
     * @return 최신 CoreRequest 조회 결과
     */
    Optional<CoreRequest> findLatestByTarget(
            KyvcEnums.CoreTargetType targetType, // Core 대상 유형
            Long targetId, // 대상 ID
            KyvcEnums.CoreRequestType requestType // Core 요청 유형
    );

}
