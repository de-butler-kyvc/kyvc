package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.domain.CommonCodeGroup;

import java.util.Optional;

/**
 * 공통코드 그룹 Repository
 */
public interface CommonCodeGroupRepository {

    /**
     * 그룹 코드 기준 공통코드 그룹 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @return 공통코드 그룹 조회 결과
     */
    Optional<CommonCodeGroup> findByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    );

    /**
     * 활성 그룹 코드 기준 공통코드 그룹 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @return 활성 공통코드 그룹 조회 결과
     */
    Optional<CommonCodeGroup> findEnabledByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    );
}
