package com.kyvc.backend.domain.core.repository;

import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// CoreRequest Repository 구현체
@Repository
@RequiredArgsConstructor
public class CoreRequestRepositoryImpl implements CoreRequestRepository {

    private final CoreRequestJpaRepository coreRequestJpaRepository;

    // CoreRequest 저장
    @Override
    public CoreRequest save(
            CoreRequest coreRequest // 저장 대상 CoreRequest
    ) {
        return coreRequestJpaRepository.save(coreRequest);
    }

    // CoreRequest ID 기준 조회
    @Override
    public CoreRequest getById(
            String coreRequestId // Core 요청 ID
    ) {
        return coreRequestJpaRepository.findById(coreRequestId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // CoreRequest ID 기준 Optional 조회
    @Override
    public Optional<CoreRequest> findById(
            String coreRequestId // Core 요청 ID
    ) {
        return coreRequestJpaRepository.findById(coreRequestId);
    }

    // 대상 기준 최신 CoreRequest 조회
    @Override
    public Optional<CoreRequest> findLatestByTarget(
            KyvcEnums.CoreTargetType targetType, // Core 대상 유형
            Long targetId, // 대상 ID
            KyvcEnums.CoreRequestType requestType // Core 요청 유형
    ) {
        return coreRequestJpaRepository.findFirstByCoreTargetTypeAndTargetIdAndCoreRequestTypeOrderByRequestedAtDesc(
                targetType,
                targetId,
                requestType
        );
    }

    // Callback 완료 여부 조회
    @Override
    public boolean existsCompletedCallback(
            String coreRequestId // Core 요청 ID
    ) {
        return findById(coreRequestId)
                .map(CoreRequest::isCompleted)
                .orElse(false);
    }
}
