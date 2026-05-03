package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.domain.CommonCode;

import java.util.Optional;

/**
 * 공통코드 Repository
 */
public interface CommonCodeRepository {

    /**
     * 그룹 코드와 코드 기준 공통코드 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @param code 공통코드 값
     * @return 공통코드 조회 결과
     */
    Optional<CommonCode> findByCodeGroupAndCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    );

    /**
     * 그룹 코드와 코드 기준 활성 공통코드 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @param code 공통코드 값
     * @return 활성 공통코드 조회 결과
     */
    Optional<CommonCode> findEnabledByCodeGroupAndCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    );

    /**
     * 그룹 코드와 코드 기준 활성 공통코드 존재 여부
     *
     * @param codeGroup 공통코드 그룹 코드
     * @param code 공통코드 값
     * @return 활성 공통코드 존재 여부
     */
    boolean existsEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    );
}
