package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.DocumentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 제출 문서 요구사항 JPA Repository
 */
public interface DocumentRequirementJpaRepository extends JpaRepository<DocumentRequirement, Long> {

    /**
     * 회사 유형 기준 활성 제출 문서 요구사항 목록 조회
     *
     * @param corporateTypeCode 회사 유형 코드
     * @return 활성 제출 문서 요구사항 목록
     */
    List<DocumentRequirement> findByCorporateTypeCodeAndEnabledTrueOrderBySortOrderAscDocumentTypeCodeAsc(
            String corporateTypeCode // 회사 유형 코드
    );
}
