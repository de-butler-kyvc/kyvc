package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.domain.CommonCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 공통코드 JPA Repository
 */
public interface CommonCodeJpaRepository extends JpaRepository<CommonCode, Long> {

    /**
     * 그룹 코드와 코드 기준 공통코드 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @param code 공통코드 값
     * @return 공통코드 조회 결과
     */
    Optional<CommonCode> findByCodeGroup_CodeGroupAndCode(
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
    Optional<CommonCode> findByCodeGroup_CodeGroupAndCodeAndEnabledTrue(
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
    boolean existsByCodeGroup_CodeGroupAndCodeAndEnabledTrue(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    );
}
