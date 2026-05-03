package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 법인 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CorporateRepositoryImpl implements CorporateRepository {

    private final CorporateJpaRepository corporateJpaRepository;

    // 사용자 ID 기준 법인 조회
    @Override
    public Optional<Corporate> findByUserId(
            Long userId // 사용자 ID
    ) {
        return corporateJpaRepository.findByUserId(userId);
    }
}
