package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;
import com.kyvc.backend.domain.corporate.application.CorporateTypeCodeNormalizer;
import com.kyvc.backend.domain.document.domain.DocumentRequirement;
import com.kyvc.backend.domain.document.domain.DocumentRequirementGroup;
import com.kyvc.backend.domain.document.domain.DocumentRequirementItem;
import com.kyvc.backend.domain.document.domain.DocumentRequirementPolicy;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.domain.document.repository.DocumentRequirementRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

// KYC 필수서류 정책 Provider
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequiredDocumentPolicyProvider {

    private static final String CORPORATE_TYPE_GROUP = "CORPORATE_TYPE"; // 회사 유형 공통코드 그룹
    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE"; // 문서 유형 공통코드 그룹
    private static final int DEFAULT_MIN_REQUIRED_COUNT = 1; // 기본 그룹 최소 제출 개수
    private static final Set<String> AGENT_REQUIRED_DOCUMENT_TYPES = Set.of(
            "POWER_OF_ATTORNEY",
            "SEAL_CERTIFICATE"
    ); // 대리 신청 조건부 필수 문서 유형 목록

    private final DocumentRequirementRepository documentRequirementRepository;
    private final CommonCodeProvider commonCodeProvider;
    private final DocumentStorageProperties documentStorageProperties;

    // 법인 유형 기준 필수서류 정책 목록 조회
    public List<RequiredDocumentPolicy> getRequiredDocuments(
            String corporateTypeCode // 법인 유형 코드
    ) {
        return getPolicyRequirements(corporateTypeCode).stream()
                .map(this::toRequiredDocumentPolicy)
                .toList();
    }

    // 회사 유형별 제출 문서 정책 조회
    public DocumentRequirementPolicy getPolicy(
            String corporateTypeCode // 회사 유형 코드
    ) {
        String normalizedCorporateTypeCode = normalizeCorporateTypeCode(corporateTypeCode); // 정규화 회사 유형 코드
        if (!StringUtils.hasText(normalizedCorporateTypeCode)) {
            return unsupportedPolicy(null);
        }

        List<ResolvedDocumentRequirement> requirements = getPolicyRequirements(normalizedCorporateTypeCode);
        if (requirements.isEmpty()) {
            return unsupportedPolicy(normalizedCorporateTypeCode);
        }

        List<DocumentRequirementItem> requiredItems = requirements.stream()
                .filter(requirement -> isSingleRequired(requirement.requirement()))
                .map(this::toItem)
                .toList();
        List<DocumentRequirementGroup> requiredGroups = buildGroups(requirements);
        List<DocumentRequirementItem> agentRequiredItems = requirements.stream()
                .filter(requirement -> isAgentConditionalRequirement(requirement.requirement()))
                .map(this::toItem)
                .toList();

        return new DocumentRequirementPolicy(
                normalizedCorporateTypeCode,
                true,
                requiredItems,
                requiredGroups,
                agentRequiredItems
        );
    }

    // 법인 유형 기준 필수 문서 유형 코드 목록 조회
    public List<String> getRequiredDocumentTypeCodes(
            String corporateTypeCode // 법인 유형 코드
    ) {
        return getRequiredDocuments(corporateTypeCode).stream()
                .map(RequiredDocumentPolicy::documentTypeCode)
                .toList();
    }

    // 법인 유형 기준 필수 문서 여부 조회
    public boolean isRequiredDocument(
            String corporateTypeCode, // 법인 유형 코드
            String documentTypeCode // 문서 유형 코드
    ) {
        String normalizedDocumentTypeCode = DocumentTypeCodeNormalizer.normalize(documentTypeCode); // 정규화 문서 유형 코드
        return getRequiredDocumentTypeCodes(corporateTypeCode).contains(normalizedDocumentTypeCode);
    }

    private List<ResolvedDocumentRequirement> getPolicyRequirements(
            String corporateTypeCode // 회사 유형 코드
    ) {
        String normalizedCorporateTypeCode = normalizeCorporateTypeCode(corporateTypeCode); // 정규화 회사 유형 코드
        if (!StringUtils.hasText(normalizedCorporateTypeCode)) {
            return List.of();
        }

        commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, normalizedCorporateTypeCode);
        Map<String, CommonCodeItem> enabledDocumentTypeMap = getEnabledDocumentTypeMap(); // 활성 문서 유형 공통코드 맵
        return documentRequirementRepository.findEnabledByCorporateTypeCode(normalizedCorporateTypeCode).stream()
                .map(requirement -> resolveRequirement(requirement, enabledDocumentTypeMap))
                .filter(resolved -> resolved != null && isPolicyRequirement(resolved.requirement()))
                .toList();
    }

    private ResolvedDocumentRequirement resolveRequirement(
            DocumentRequirement requirement, // 제출 문서 요구사항
            Map<String, CommonCodeItem> enabledDocumentTypeMap // 활성 문서 유형 공통코드 맵
    ) {
        CommonCodeItem documentType = enabledDocumentTypeMap.get(requirement.getDocumentTypeCode());
        if (documentType != null) {
            return new ResolvedDocumentRequirement(requirement, documentType);
        }
        if (commonCodeProvider.existsCode(DOCUMENT_TYPE_GROUP, requirement.getDocumentTypeCode())) {
            return null;
        }
        throw new ApiException(
                ErrorCode.COMMON_CODE_NOT_FOUND,
                DOCUMENT_TYPE_GROUP + ":" + requirement.getDocumentTypeCode() + " 공통 코드를 찾을 수 없습니다."
        );
    }

    private Map<String, CommonCodeItem> getEnabledDocumentTypeMap() {
        return commonCodeProvider.getEnabledCodes(DOCUMENT_TYPE_GROUP).stream()
                .collect(Collectors.toMap(
                        CommonCodeItem::code,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private RequiredDocumentPolicy toRequiredDocumentPolicy(
            ResolvedDocumentRequirement resolvedRequirement // 공통코드가 반영된 제출 문서 요구사항
    ) {
        DocumentRequirement requirement = resolvedRequirement.requirement(); // 제출 문서 요구사항
        DocumentRequirementGroup group = isGroupCandidate(requirement)
                ? toGroup(requirement, List.of(toItem(resolvedRequirement)))
                : null; // 선택 필수 그룹
        return createPolicy(
                requirement.getDocumentTypeCode(),
                resolvedRequirement.documentType().codeName(),
                resolveGuideMessage(resolvedRequirement),
                isSingleRequired(requirement),
                group
        );
    }

    private List<DocumentRequirementGroup> buildGroups(
            List<ResolvedDocumentRequirement> requirements // 공통코드가 반영된 제출 문서 요구사항 목록
    ) {
        Map<String, GroupBuilder> groups = new LinkedHashMap<>(); // 선택 필수 그룹 조립 맵
        requirements.stream()
                .filter(requirement -> isGroupCandidate(requirement.requirement()))
                .forEach(requirement -> groups.computeIfAbsent(
                        requirement.requirement().getRequirementGroupCode(),
                        groupCode -> new GroupBuilder(
                                groupCode,
                                resolveGroupName(requirement.requirement()),
                                resolveMinRequiredCount(requirement.requirement())
                        )
                ).add(toItem(requirement)));

        return groups.values().stream()
                .map(GroupBuilder::toGroup)
                .toList();
    }

    private DocumentRequirementGroup toGroup(
            DocumentRequirement requirement, // 제출 문서 요구사항
            List<DocumentRequirementItem> items // 그룹 문서 목록
    ) {
        return new DocumentRequirementGroup(
                requirement.getRequirementGroupCode(),
                resolveGroupName(requirement),
                resolveMinRequiredCount(requirement),
                items
        );
    }

    private DocumentRequirementItem toItem(
            ResolvedDocumentRequirement resolvedRequirement // 공통코드가 반영된 제출 문서 요구사항
    ) {
        return new DocumentRequirementItem(
                resolvedRequirement.requirement().getDocumentTypeCode(),
                resolvedRequirement.documentType().codeName(),
                resolveGuideMessage(resolvedRequirement)
        );
    }

    private RequiredDocumentPolicy createPolicy(
            String documentTypeCode, // 문서 유형 코드
            String documentTypeName, // 문서 유형 표시명
            String description, // 제출 안내 문구
            boolean required, // 필수 여부
            DocumentRequirementGroup group // 선택 필수 그룹
    ) {
        return new RequiredDocumentPolicy(
                documentTypeCode,
                documentTypeName,
                required,
                description,
                documentStorageProperties.getAllowedExtensions(),
                documentStorageProperties.getMaxFileSizeMb(),
                group == null ? null : group.groupCode(),
                group == null ? null : group.groupName(),
                group == null ? null : group.minRequiredCount(),
                group != null
        );
    }

    private String resolveGuideMessage(
            ResolvedDocumentRequirement resolvedRequirement // 공통코드가 반영된 제출 문서 요구사항
    ) {
        if (StringUtils.hasText(resolvedRequirement.requirement().getGuideMessage())) {
            return resolvedRequirement.requirement().getGuideMessage();
        }
        return resolvedRequirement.documentType().codeName() + "을 제출해 주세요.";
    }

    private String resolveGroupName(
            DocumentRequirement requirement // 제출 문서 요구사항
    ) {
        if (StringUtils.hasText(requirement.getRequirementGroupName())) {
            return requirement.getRequirementGroupName();
        }
        return requirement.getRequirementGroupCode();
    }

    private int resolveMinRequiredCount(
            DocumentRequirement requirement // 제출 문서 요구사항
    ) {
        return requirement.getMinRequiredCount() == null
                ? DEFAULT_MIN_REQUIRED_COUNT
                : requirement.getMinRequiredCount();
    }

    private boolean isPolicyRequirement(
            DocumentRequirement requirement // 제출 문서 요구사항
    ) {
        return isSingleRequired(requirement)
                || isGroupCandidate(requirement)
                || isAgentConditionalRequirement(requirement);
    }

    private boolean isSingleRequired(
            DocumentRequirement requirement // 제출 문서 요구사항
    ) {
        return requirement.isRequired() && !hasGroup(requirement);
    }

    private boolean isGroupCandidate(
            DocumentRequirement requirement // 제출 문서 요구사항
    ) {
        return hasGroup(requirement);
    }

    private boolean isAgentConditionalRequirement(
            DocumentRequirement requirement // 제출 문서 요구사항
    ) {
        return !requirement.isRequired()
                && !hasGroup(requirement)
                && AGENT_REQUIRED_DOCUMENT_TYPES.contains(requirement.getDocumentTypeCode());
    }

    private boolean hasGroup(
            DocumentRequirement requirement // 제출 문서 요구사항
    ) {
        return StringUtils.hasText(requirement.getRequirementGroupCode());
    }

    private DocumentRequirementPolicy unsupportedPolicy(
            String corporateTypeCode // 회사 유형 코드
    ) {
        return new DocumentRequirementPolicy(
                corporateTypeCode,
                false,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private String normalizeCorporateTypeCode(
            String corporateTypeCode // 원본 회사 유형 코드
    ) {
        return CorporateTypeCodeNormalizer.normalize(corporateTypeCode);
    }

    private record ResolvedDocumentRequirement(
            DocumentRequirement requirement, // 제출 문서 요구사항
            CommonCodeItem documentType // 문서 유형 공통코드
    ) {
    }

    private record GroupBuilder(
            String groupCode, // 선택 필수 그룹 코드
            String groupName, // 선택 필수 그룹 표시명
            int minRequiredCount, // 그룹 최소 제출 개수
            List<DocumentRequirementItem> items // 그룹 문서 목록
    ) {

        private GroupBuilder(
                String groupCode, // 선택 필수 그룹 코드
                String groupName, // 선택 필수 그룹 표시명
                int minRequiredCount // 그룹 최소 제출 개수
        ) {
            this(groupCode, groupName, minRequiredCount, new java.util.ArrayList<>());
        }

        private void add(
                DocumentRequirementItem item // 그룹 문서 항목
        ) {
            items.add(item);
        }

        private DocumentRequirementGroup toGroup() {
            return new DocumentRequirementGroup(groupCode, groupName, minRequiredCount, items);
        }
    }

    // 필수서류 정책 데이터
    public record RequiredDocumentPolicy(
            String documentTypeCode, // 문서 유형 코드
            String documentTypeName, // 문서 유형 표시명
            boolean required, // 필수 여부
            String description, // 제출 안내 문구
            List<String> allowedExtensions, // 허용 확장자 목록
            int maxFileSizeMb, // 최대 파일 크기 MB
            String groupCode, // 선택 필수 그룹 코드
            String groupName, // 선택 필수 그룹 표시명
            Integer minRequiredCount, // 그룹 최소 제출 개수
            boolean groupCandidate // 선택 필수 그룹 후보 여부
    ) {

        public RequiredDocumentPolicy {
            allowedExtensions = allowedExtensions == null ? List.of() : List.copyOf(allowedExtensions);
        }

        public RequiredDocumentPolicy(
                String documentTypeCode, // 문서 유형 코드
                String documentTypeName, // 문서 유형 표시명
                boolean required, // 필수 여부
                String description, // 제출 안내 문구
                List<String> allowedExtensions, // 허용 확장자 목록
                int maxFileSizeMb // 최대 파일 크기 MB
        ) {
            this(
                    documentTypeCode,
                    documentTypeName,
                    required,
                    description,
                    allowedExtensions,
                    maxFileSizeMb,
                    null,
                    null,
                    null,
                    false
            );
        }
    }
}
