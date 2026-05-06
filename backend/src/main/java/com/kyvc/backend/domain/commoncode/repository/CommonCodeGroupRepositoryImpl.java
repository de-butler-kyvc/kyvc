package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.domain.CommonCodeGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 공통코드 그룹 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CommonCodeGroupRepositoryImpl implements CommonCodeGroupRepository {

    private final CommonCodeGroupJpaRepository commonCodeGroupJpaRepository;

    @Override
    public Optional<CommonCodeGroup> findByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeGroupJpaRepository.findByCodeGroup(codeGroup);
    }

    @Override
    public Optional<CommonCodeGroup> findEnabledByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeGroupJpaRepository.findByCodeGroupAndEnabledTrue(codeGroup);
    }

    @Override
    public List<CommonCodeGroup> findAllOrderBySortOrderAscCodeGroupAsc() {
        return commonCodeGroupJpaRepository.findAllByOrderBySortOrderAscCodeGroupAsc();
    }

    @Override
    public List<CommonCodeGroup> findEnabledAllOrderBySortOrderAscCodeGroupAsc() {
        return commonCodeGroupJpaRepository.findByEnabledTrueOrderBySortOrderAscCodeGroupAsc();
    }
}
