package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.document.domain.DocumentRequirementGroup;
import com.kyvc.backend.domain.document.domain.DocumentRequirementItem;
import com.kyvc.backend.domain.document.domain.DocumentRequirementPolicy;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.domain.corporate.application.CorporateTypeCodeNormalizer;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// KYC 필수서류 정책 Provider
@Component
@RequiredArgsConstructor
public class RequiredDocumentPolicyProvider {

    private static final String OWNERSHIP_DOC_GROUP = "OWNERSHIP_DOC"; // 소유구조 문서 그룹
    private static final String RULE_DOC_GROUP = "RULE_DOC"; // 규약 문서 그룹
    private static final int MIN_ONE = 1; // 최소 1개 기준

    private final DocumentStorageProperties documentStorageProperties;

    // 법인 유형 기준 필수서류 정책 목록 조회
    public List<RequiredDocumentPolicy> getRequiredDocuments(
            String corporateTypeCode // 법인 유형 코드
    ) {
        DocumentRequirementPolicy policy = getPolicy(corporateTypeCode); // 회사 유형별 문서 정책
        if (!policy.supported()) {
            return List.of();
        }
        Map<String, RequiredDocumentPolicy> policies = new LinkedHashMap<>(); // 문서 유형별 안내 정책
        policy.requiredItems().forEach(item -> addPolicy(policies, item, true, null));
        policy.requiredGroups().forEach(group -> group.items()
                .forEach(item -> addPolicy(policies, item, false, group)));
        policy.agentRequiredItems().forEach(item -> addPolicy(policies, item, false, null));
        return List.copyOf(policies.values());
    }

    // 회사 유형별 제출 문서 정책 조회
    public DocumentRequirementPolicy getPolicy(
            String corporateTypeCode // 회사 유형 코드
    ) {
        KyvcEnums.CorporateType corporateType = resolveCorporateType(corporateTypeCode); // 정규화 회사 유형
        return switch (corporateType) {
            case CORPORATION -> corporationPolicy();
            case LIMITED_COMPANY -> limitedCompanyPolicy(KyvcEnums.CorporateType.LIMITED_COMPANY);
            case LIMITED_PARTNERSHIP -> limitedCompanyPolicy(KyvcEnums.CorporateType.LIMITED_PARTNERSHIP);
            case GENERAL_PARTNERSHIP -> limitedCompanyPolicy(KyvcEnums.CorporateType.GENERAL_PARTNERSHIP);
            case NON_PROFIT -> nonProfitPolicy();
            case ASSOCIATION -> associationPolicy();
            case FOREIGN_COMPANY -> foreignCompanyPolicy();
            case SOLE_PROPRIETOR -> unsupportedPolicy(KyvcEnums.CorporateType.SOLE_PROPRIETOR);
        };
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
        return getRequiredDocumentTypeCodes(corporateTypeCode).contains(documentTypeCode);
    }

    // 필수서류 정책 생성
    private RequiredDocumentPolicy createPolicy(
            String documentTypeCode, // 문서 유형 코드
            String documentTypeName, // 문서 유형 표시명
            String description // 제출 안내 문구
    ) {
        return new RequiredDocumentPolicy(
                documentTypeCode,
                documentTypeName,
                true,
                description,
                documentStorageProperties.getAllowedExtensions(),
                documentStorageProperties.getMaxFileSizeMb()
        );
    }

    private void addPolicy(
            Map<String, RequiredDocumentPolicy> policies, // 문서 유형별 안내 정책
            DocumentRequirementItem item, // 정책 문서 항목
            boolean required, // 단일 필수 여부
            DocumentRequirementGroup group // 선택 필수 그룹
    ) {
        policies.putIfAbsent(
                item.documentTypeCode(),
                createPolicy(item.documentTypeCode(), item.documentTypeName(), item.description(), required, group)
        );
    }

    // 필수서류 정책 생성
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

    private DocumentRequirementPolicy corporationPolicy() {
        return new DocumentRequirementPolicy(
                KyvcEnums.CorporateType.CORPORATION.name(),
                true,
                List.of(
                        item(KyvcEnums.DocumentType.BUSINESS_REGISTRATION),
                        item(KyvcEnums.DocumentType.CORPORATE_REGISTRY)
                ),
                List.of(group(
                        OWNERSHIP_DOC_GROUP,
                        "소유구조 확인 문서",
                        KyvcEnums.DocumentType.SHAREHOLDER_REGISTRY,
                        KyvcEnums.DocumentType.STOCK_CHANGE_STATEMENT
                )),
                agentRequiredItems()
        );
    }

    private DocumentRequirementPolicy limitedCompanyPolicy(
            KyvcEnums.CorporateType corporateType // 회사 유형
    ) {
        return new DocumentRequirementPolicy(
                corporateType.name(),
                true,
                List.of(
                        item(KyvcEnums.DocumentType.BUSINESS_REGISTRATION),
                        item(KyvcEnums.DocumentType.CORPORATE_REGISTRY)
                ),
                List.of(group(
                        OWNERSHIP_DOC_GROUP,
                        "소유구조 확인 문서",
                        KyvcEnums.DocumentType.INVESTOR_REGISTRY,
                        KyvcEnums.DocumentType.MEMBER_REGISTRY,
                        KyvcEnums.DocumentType.ARTICLES_OF_ASSOCIATION
                )),
                agentRequiredItems()
        );
    }

    private DocumentRequirementPolicy nonProfitPolicy() {
        return new DocumentRequirementPolicy(
                KyvcEnums.CorporateType.NON_PROFIT.name(),
                true,
                List.of(
                        item(KyvcEnums.DocumentType.ARTICLES_OF_ASSOCIATION),
                        item(KyvcEnums.DocumentType.PURPOSE_PROOF_DOCUMENT),
                        item(KyvcEnums.DocumentType.CORPORATE_REGISTRY)
                ),
                List.of(),
                agentRequiredItems()
        );
    }

    private DocumentRequirementPolicy associationPolicy() {
        return new DocumentRequirementPolicy(
                KyvcEnums.CorporateType.ASSOCIATION.name(),
                true,
                List.of(
                        item(KyvcEnums.DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE),
                        item(KyvcEnums.DocumentType.REPRESENTATIVE_PROOF_DOCUMENT)
                ),
                List.of(group(
                        RULE_DOC_GROUP,
                        "규약 문서",
                        KyvcEnums.DocumentType.OPERATING_RULES,
                        KyvcEnums.DocumentType.REGULATIONS,
                        KyvcEnums.DocumentType.ARTICLES_OF_ASSOCIATION
                )),
                agentRequiredItems()
        );
    }

    private DocumentRequirementPolicy foreignCompanyPolicy() {
        return new DocumentRequirementPolicy(
                KyvcEnums.CorporateType.FOREIGN_COMPANY.name(),
                true,
                List.of(
                        item(KyvcEnums.DocumentType.BUSINESS_REGISTRATION),
                        item(KyvcEnums.DocumentType.CORPORATE_REGISTRY),
                        item(KyvcEnums.DocumentType.PURPOSE_PROOF_DOCUMENT),
                        item(KyvcEnums.DocumentType.INVESTMENT_REGISTRATION_CERTIFICATE)
                ),
                List.of(group(
                        OWNERSHIP_DOC_GROUP,
                        "소유구조 확인 문서",
                        KyvcEnums.DocumentType.SHAREHOLDER_REGISTRY,
                        KyvcEnums.DocumentType.STOCK_CHANGE_STATEMENT,
                        KyvcEnums.DocumentType.INVESTOR_REGISTRY
                )),
                agentRequiredItems()
        );
    }

    private DocumentRequirementPolicy unsupportedPolicy(
            KyvcEnums.CorporateType corporateType // 회사 유형
    ) {
        return new DocumentRequirementPolicy(
                corporateType.name(),
                false,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private List<DocumentRequirementItem> agentRequiredItems() {
        return List.of(
                item(KyvcEnums.DocumentType.POWER_OF_ATTORNEY),
                item(KyvcEnums.DocumentType.SEAL_CERTIFICATE)
        );
    }

    private DocumentRequirementGroup group(
            String groupCode, // 그룹 코드
            String groupName, // 그룹 표시명
            KyvcEnums.DocumentType... documentTypes // 그룹 문서 유형
    ) {
        return new DocumentRequirementGroup(
                groupCode,
                groupName,
                MIN_ONE,
                List.of(documentTypes).stream()
                        .map(this::item)
                        .toList()
        );
    }

    private DocumentRequirementItem item(
            KyvcEnums.DocumentType documentType // 문서 유형
    ) {
        return new DocumentRequirementItem(
                documentType.name(),
                documentType.displayName(),
                documentType.displayName() + "를 제출해 주세요."
        );
    }

    private KyvcEnums.CorporateType resolveCorporateType(
            String corporateTypeCode // 회사 유형 코드
    ) {
        if (!StringUtils.hasText(corporateTypeCode)) {
            return KyvcEnums.CorporateType.SOLE_PROPRIETOR;
        }
        String normalized = CorporateTypeCodeNormalizer.normalize(corporateTypeCode); // 정규화 회사 유형 코드
        try {
            return KyvcEnums.CorporateType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return KyvcEnums.CorporateType.SOLE_PROPRIETOR;
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
