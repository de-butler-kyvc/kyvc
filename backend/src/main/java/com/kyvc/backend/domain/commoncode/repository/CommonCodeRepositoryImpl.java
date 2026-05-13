package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.domain.CommonCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 공통코드 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CommonCodeRepositoryImpl implements CommonCodeRepository {

    private final CommonCodeJpaRepository commonCodeJpaRepository;

    @Override
    public Optional<CommonCode> findByCodeGroupAndCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        return commonCodeJpaRepository.findByCodeGroup_CodeGroupAndCode(codeGroup, code);
    }

    @Override
    public Optional<CommonCode> findEnabledByCodeGroupAndCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        return commonCodeJpaRepository.findByCodeGroup_CodeGroupAndCodeAndEnabledTrue(codeGroup, code);
    }

    @Override
    public boolean existsEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        return commonCodeJpaRepository.existsByCodeGroup_CodeGroupAndCodeAndEnabledTrue(codeGroup, code);
    }

    @Override
    public List<CommonCode> findByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeJpaRepository.findByCodeGroup_CodeGroupOrderBySortOrderAscCodeAsc(codeGroup);
    }

    @Override
    public List<CommonCode> findEnabledByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeJpaRepository.findByCodeGroup_CodeGroupAndEnabledTrueOrderBySortOrderAscCodeAsc(codeGroup);
    }
}
