package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.Corporate;

import java.util.Optional;

/**
 * 법인 Repository
 */
public interface CorporateRepository {

    /**
     * 사용자 ID 기준 법인 조회
     *
     * @param userId 사용자 ID
     * @return 법인 조회 결과
     */
    Optional<Corporate> findByUserId(
            Long userId // 사용자 ID
    );
}
