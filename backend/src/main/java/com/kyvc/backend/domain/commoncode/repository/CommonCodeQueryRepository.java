package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;

import java.util.List;

/**
 * 공통코드 조회용 Repository
 */
public interface CommonCodeQueryRepository {

    /**
     * 그룹 코드 기준 활성 공통코드 목록 조회
     *
     * @param codeGroup 공통코드 그룹 코드
     * @return 활성 공통코드 목록
     */
    List<CommonCodeItem> findEnabledCodeItemsByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    );
}
