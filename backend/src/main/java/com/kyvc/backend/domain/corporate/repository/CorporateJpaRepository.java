package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 법인 JPA Repository
 */
public interface CorporateJpaRepository extends JpaRepository<Corporate, Long> {

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
