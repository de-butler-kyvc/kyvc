package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.document.domain.DocumentRequirementGroup;
import com.kyvc.backend.domain.document.domain.DocumentRequirementItem;
import com.kyvc.backend.domain.document.domain.DocumentRequirementPolicy;
import com.kyvc.backend.domain.document.domain.DocumentRequirementValidationResult;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 제출 문서 요구사항 검증 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentRequirementValidationService {

    private final RequiredDocumentPolicyProvider requiredDocumentPolicyProvider;

    // 제출 문서 요구사항 검증
    public DocumentRequirementValidationResult validate(
            String corporateTypeCode, // 회사 유형 코드
            Collection<String> submittedDocumentTypeCodes, // 제출 문서 유형 코드 목록
            boolean agentApplication // 대리인 신청 여부
    ) {
        DocumentRequirementPolicy policy = requiredDocumentPolicyProvider.getPolicy(corporateTypeCode); // 회사 유형별 문서 정책
        if (!policy.supported()) {
            return new DocumentRequirementValidationResult(false, List.of(), List.of());
        }

        Set<String> submittedTypes = normalizeDocumentTypes(submittedDocumentTypeCodes); // 정규화 제출 문서 유형 목록
        List<DocumentRequirementItem> requiredItems = agentApplication
                ? merge(policy.requiredItems(), policy.agentRequiredItems())
                : policy.requiredItems(); // 조건 반영 단일 필수 문서 목록
        List<DocumentRequirementItem> missingItems = requiredItems.stream()
                .filter(item -> !submittedTypes.contains(item.documentTypeCode()))
                .toList();
        List<DocumentRequirementGroup> unsatisfiedGroups = policy.requiredGroups().stream()
                .filter(group -> submittedCount(group, submittedTypes) < group.minRequiredCount())
                .toList();

        return new DocumentRequirementValidationResult(true, missingItems, unsatisfiedGroups);
    }

    // 제출 문서 요구사항 검증 및 예외 처리
    public void validateOrThrow(
            String corporateTypeCode, // 회사 유형 코드
            Collection<String> submittedDocumentTypeCodes, // 제출 문서 유형 코드 목록
            boolean agentApplication // 대리인 신청 여부
    ) {
        DocumentRequirementValidationResult result = validate(
                corporateTypeCode,
                submittedDocumentTypeCodes,
                agentApplication
        );
        if (!result.supported()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!result.valid()) {
            throw new ApiException(ErrorCode.DOCUMENT_REQUIRED_MISSING);
        }
    }

    private int submittedCount(
            DocumentRequirementGroup group, // 선택 필수 그룹
            Set<String> submittedTypes // 제출 문서 유형 코드 목록
    ) {
        return (int) group.items().stream()
                .filter(item -> submittedTypes.contains(item.documentTypeCode()))
                .count();
    }

    private List<DocumentRequirementItem> merge(
            List<DocumentRequirementItem> baseItems, // 기본 필수 문서 목록
            List<DocumentRequirementItem> conditionalItems // 조건부 필수 문서 목록
    ) {
        return java.util.stream.Stream.concat(baseItems.stream(), conditionalItems.stream())
                .toList();
    }

    private Set<String> normalizeDocumentTypes(
            Collection<String> documentTypeCodes // 원본 문서 유형 코드 목록
    ) {
        if (documentTypeCodes == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>(); // 정규화 문서 유형 코드 목록
        for (String documentTypeCode : documentTypeCodes) {
            if (StringUtils.hasText(documentTypeCode)) {
                normalized.add(DocumentTypeCodeNormalizer.normalize(documentTypeCode));
            }
        }
        return normalized;
    }
}
