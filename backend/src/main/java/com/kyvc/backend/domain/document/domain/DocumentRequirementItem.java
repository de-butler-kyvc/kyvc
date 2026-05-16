package com.kyvc.backend.domain.document.domain;

// 제출 문서 요구 항목
public record DocumentRequirementItem(
        String documentTypeCode, // 문서 유형 코드
        String documentTypeName, // 문서 유형 표시명
        String description // 제출 안내 문구
) {
}
