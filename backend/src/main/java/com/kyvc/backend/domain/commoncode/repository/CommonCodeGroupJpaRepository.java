package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.domain.CommonCodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 공통코드 그룹 JPA Repository
 */
public interface CommonCodeGroupJpaRepository extends JpaRepository<CommonCodeGroup, Long> {

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
     * 그룹 코드 기준 활성 공통코드 그룹 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @return 활성 공통코드 그룹 조회 결과
     */
    Optional<CommonCodeGroup> findByCodeGroupAndEnabledTrue(
            String codeGroup // 공통코드 그룹 코드
    );
}
