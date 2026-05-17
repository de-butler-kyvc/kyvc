package com.kyvc.backend.domain.document.domain;

import java.util.List;

// 회사 유형별 제출 문서 정책
public record DocumentRequirementPolicy(
        String corporateTypeCode, // 회사 유형 코드
        boolean supported, // 정책 지원 여부
        List<DocumentRequirementItem> requiredItems, // 단일 필수 문서 목록
        List<DocumentRequirementGroup> requiredGroups, // 선택 필수 그룹 목록
        List<DocumentRequirementItem> agentRequiredItems // 대리인 조건부 필수 문서 목록
) {

    public DocumentRequirementPolicy {
        requiredItems = requiredItems == null ? List.of() : List.copyOf(requiredItems);
        requiredGroups = requiredGroups == null ? List.of() : List.copyOf(requiredGroups);
        agentRequiredItems = agentRequiredItems == null ? List.of() : List.copyOf(agentRequiredItems);
    }
}
