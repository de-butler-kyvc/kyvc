package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.domain.CommonCodeGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 공통코드 그룹 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CommonCodeGroupRepositoryImpl implements CommonCodeGroupRepository {

    private final CommonCodeGroupJpaRepository commonCodeGroupJpaRepository;

    // 그룹 코드 기준 공통코드 그룹 조회
    @Override
    public Optional<CommonCodeGroup> findByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeGroupJpaRepository.findByCodeGroup(codeGroup);
    }

    // 활성 그룹 코드 기준 공통코드 그룹 조회
    @Override
    public Optional<CommonCodeGroup> findEnabledByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeGroupJpaRepository.findByCodeGroupAndEnabledTrue(codeGroup);
    }
}
