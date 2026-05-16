package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.domain.DocumentRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// 제출 문서 요구사항 Repository 구현체
@Repository
@RequiredArgsConstructor
public class DocumentRequirementRepositoryImpl implements DocumentRequirementRepository {

    private final DocumentRequirementJpaRepository documentRequirementJpaRepository;

    // 회사 유형 기준 활성 제출 문서 요구사항 목록 조회
    @Override
    public List<DocumentRequirement> findEnabledByCorporateTypeCode(
            String corporateTypeCode // 회사 유형 코드
    ) {
        return documentRequirementJpaRepository.findByCorporateTypeCodeAndEnabledTrueOrderBySortOrderAscDocumentTypeCodeAsc(
                corporateTypeCode
        );
    }
}
