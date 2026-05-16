package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.DocumentRequirement;

import java.util.List;

/**
 * 제출 문서 요구사항 Repository
 */
public interface DocumentRequirementRepository {

    /**
     * 회사 유형 기준 활성 제출 문서 요구사항 목록 조회
     *
     * @param corporateTypeCode 회사 유형 코드
     * @return 활성 제출 문서 요구사항 목록
     */
    List<DocumentRequirement> findEnabledByCorporateTypeCode(
            String corporateTypeCode // 회사 유형 코드
    );
}
