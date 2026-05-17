package com.kyvc.backend.domain.document.domain;

import java.util.List;

// 선택 필수 문서 그룹
public record DocumentRequirementGroup(
        String groupCode, // 그룹 코드
        String groupName, // 그룹 표시명
        int minRequiredCount, // 최소 제출 개수
        List<DocumentRequirementItem> items // 그룹 문서 목록
) {

    public DocumentRequirementGroup {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
