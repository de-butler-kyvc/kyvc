package com.kyvc.backend.domain.core.repository;

import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CoreRequest JPA Repository
 */
public interface CoreRequestJpaRepository extends JpaRepository<CoreRequest, String> {

    /**
     * 대상 기준 최신 Core 요청 조회
     *
     * @param coreTargetType Core 대상 유형
     * @param targetId 대상 ID
     * @param coreRequestType Core 요청 유형
     * @return 최신 Core 요청 조회 결과
     */
    Optional<CoreRequest> findFirstByCoreTargetTypeAndTargetIdAndCoreRequestTypeOrderByRequestedAtDesc(
            KyvcEnums.CoreTargetType coreTargetType, // Core 대상 유형
            Long targetId, // 대상 ID
            KyvcEnums.CoreRequestType coreRequestType // Core 요청 유형
    );
}
