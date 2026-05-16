package com.kyvc.backend.domain.document.domain;

import java.util.List;

// 제출 문서 정책 검증 결과
public record DocumentRequirementValidationResult(
        boolean supported, // 정책 지원 여부
        List<DocumentRequirementItem> missingRequiredItems, // 누락 단일 필수 문서 목록
        List<DocumentRequirementGroup> unsatisfiedGroups // 미충족 선택 필수 그룹 목록
) {

    public DocumentRequirementValidationResult {
        missingRequiredItems = missingRequiredItems == null ? List.of() : List.copyOf(missingRequiredItems);
        unsatisfiedGroups = unsatisfiedGroups == null ? List.of() : List.copyOf(unsatisfiedGroups);
    }

    public boolean valid() {
        return supported && missingRequiredItems.isEmpty() && unsatisfiedGroups.isEmpty();
    }
}
