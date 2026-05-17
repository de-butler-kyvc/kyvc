package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;
import com.kyvc.backend.domain.document.domain.DocumentRequirement;
import com.kyvc.backend.domain.document.domain.DocumentRequirementValidationResult;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.domain.document.repository.DocumentRequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentRequirementValidationServiceTest {

    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE";

    @Mock
    private DocumentRequirementRepository documentRequirementRepository;
    @Mock
    private CommonCodeProvider commonCodeProvider;

    private DocumentRequirementValidationService service;

    @BeforeEach
    void setUp() {
        RequiredDocumentPolicyProvider provider = new RequiredDocumentPolicyProvider(
                documentRequirementRepository,
                commonCodeProvider,
                new DocumentStorageProperties("./build/test-documents", 10, "pdf,jpg,jpeg,png", "application/pdf")
        );
        service = new DocumentRequirementValidationService(provider);
        when(commonCodeProvider.getEnabledCodes(DOCUMENT_TYPE_GROUP)).thenReturn(documentTypeCodes());
    }

    @Test
    void validateCorporation_acceptsShareholderRegistryOrStockChangeStatementFromRepository() {
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("CORPORATION"))
                .thenReturn(corporationRequirements());

        assertValid(
                "CORPORATION",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "SHAREHOLDER_REGISTRY"
        );
        assertValid(
                "CORPORATION",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "STOCK_CHANGE_STATEMENT"
        );

        DocumentRequirementValidationResult result = validate(
                "CORPORATION",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY"
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.unsatisfiedGroups())
                .extracting("groupCode")
                .containsExactly("OWNERSHIP_DOC");
    }

    @Test
    void validateLimitedCompany_acceptsOneOwnershipDocumentFromRepository() {
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("LIMITED_COMPANY"))
                .thenReturn(limitedCompanyRequirements());

        assertValid("LIMITED_COMPANY", false, "BUSINESS_REGISTRATION", "CORPORATE_REGISTRY", "INVESTOR_REGISTRY");
        assertValid("LIMITED_COMPANY", false, "BUSINESS_REGISTRATION", "CORPORATE_REGISTRY", "MEMBER_REGISTRY");
        assertValid("LIMITED_COMPANY", false, "BUSINESS_REGISTRATION", "CORPORATE_REGISTRY", "ARTICLES_OF_ASSOCIATION");

        DocumentRequirementValidationResult result = validate(
                "LIMITED_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY"
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.unsatisfiedGroups())
                .extracting("groupCode")
                .containsExactly("OWNERSHIP_DOC");
    }

    @Test
    void validateAssociation_acceptsOneRuleDocumentFromRepository() {
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("ASSOCIATION"))
                .thenReturn(associationRequirements());

        assertValid(
                "ASSOCIATION",
                false,
                "ORGANIZATION_IDENTITY_CERTIFICATE",
                "REPRESENTATIVE_PROOF_DOCUMENT",
                "OPERATING_RULES"
        );
        assertValid(
                "ASSOCIATION",
                false,
                "ORGANIZATION_IDENTITY_CERTIFICATE",
                "REPRESENTATIVE_PROOF_DOCUMENT",
                "REGULATIONS"
        );
        assertValid(
                "ASSOCIATION",
                false,
                "ORGANIZATION_IDENTITY_CERTIFICATE",
                "REPRESENTATIVE_PROOF_DOCUMENT",
                "ARTICLES_OF_ASSOCIATION"
        );

        DocumentRequirementValidationResult result = validate(
                "ASSOCIATION",
                false,
                "ORGANIZATION_IDENTITY_CERTIFICATE",
                "REPRESENTATIVE_PROOF_DOCUMENT"
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.unsatisfiedGroups())
                .extracting("groupCode")
                .containsExactly("RULE_DOC");
    }

    @Test
    void validateJointStockCompanyAlias_normalizesToCorporationRepositoryLookup() {
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("CORPORATION"))
                .thenReturn(corporationRequirements());

        assertValid(
                "JOINT_STOCK_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "SHAREHOLDER_REGISTRY"
        );

        verify(documentRequirementRepository).findEnabledByCorporateTypeCode("CORPORATION");
    }

    @Test
    void validateLegacyDocumentCodes_normalizesSubmittedDocuments() {
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("CORPORATION"))
                .thenReturn(corporationRequirements());

        assertValid(
                "CORPORATION",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRATION",
                "SHAREHOLDER_LIST"
        );
    }

    @Test
    void validateAgentApplication_requiresPowerOfAttorneyAndSealCertificate() {
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("CORPORATION"))
                .thenReturn(corporationRequirements());

        DocumentRequirementValidationResult result = validate(
                "CORPORATION",
                true,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "SHAREHOLDER_REGISTRY"
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.missingRequiredItems())
                .extracting("documentTypeCode")
                .containsExactly("POWER_OF_ATTORNEY", "SEAL_CERTIFICATE");
    }

    @Test
    void validateEmptyDocumentRequirements_returnsUnsupportedPolicy() {
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("SOLE_PROPRIETOR"))
                .thenReturn(List.of());

        DocumentRequirementValidationResult result = validate(
                "SOLE_PROPRIETOR",
                false,
                "BUSINESS_REGISTRATION"
        );

        assertThat(result.supported()).isFalse();
        assertThat(result.valid()).isFalse();
    }

    private void assertValid(
            String corporateTypeCode, // 회사 유형 코드
            boolean agentApplication, // 대리인 신청 여부
            String... documentTypeCodes // 제출 문서 유형 코드 목록
    ) {
        assertThat(validate(corporateTypeCode, agentApplication, documentTypeCodes).valid()).isTrue();
    }

    private DocumentRequirementValidationResult validate(
            String corporateTypeCode, // 회사 유형 코드
            boolean agentApplication, // 대리인 신청 여부
            String... documentTypeCodes // 제출 문서 유형 코드 목록
    ) {
        return service.validate(corporateTypeCode, Set.of(documentTypeCodes), agentApplication);
    }

    private List<DocumentRequirement> corporationRequirements() {
        return List.of(
                requirement("CORPORATION", "BUSINESS_REGISTRATION", true, 1, "사업자등록증을 제출해 주세요.", null, null, null),
                requirement("CORPORATION", "CORPORATE_REGISTRY", true, 2, "등기사항전부증명서를 제출해 주세요.", null, null, null),
                requirement("CORPORATION", "SHAREHOLDER_REGISTRY", false, 3, "소유구조 확인 문서 중 1개로 제출할 수 있습니다.", "OWNERSHIP_DOC", "소유구조 확인 문서", 1),
                requirement("CORPORATION", "STOCK_CHANGE_STATEMENT", false, 4, "소유구조 확인 문서 중 1개로 제출할 수 있습니다.", "OWNERSHIP_DOC", "소유구조 확인 문서", 1),
                requirement("CORPORATION", "POWER_OF_ATTORNEY", false, 5, "대리 신청 시 위임장을 제출해 주세요.", null, null, null),
                requirement("CORPORATION", "SEAL_CERTIFICATE", false, 6, "대리 신청 시 인감증명서를 제출해 주세요.", null, null, null)
        );
    }

    private List<DocumentRequirement> limitedCompanyRequirements() {
        return List.of(
                requirement("LIMITED_COMPANY", "BUSINESS_REGISTRATION", true, 1, "사업자등록증을 제출해 주세요.", null, null, null),
                requirement("LIMITED_COMPANY", "CORPORATE_REGISTRY", true, 2, "등기사항전부증명서를 제출해 주세요.", null, null, null),
                requirement("LIMITED_COMPANY", "INVESTOR_REGISTRY", false, 3, "소유구조 확인 문서 중 1개로 제출할 수 있습니다.", "OWNERSHIP_DOC", "소유구조 확인 문서", 1),
                requirement("LIMITED_COMPANY", "MEMBER_REGISTRY", false, 4, "소유구조 확인 문서 중 1개로 제출할 수 있습니다.", "OWNERSHIP_DOC", "소유구조 확인 문서", 1),
                requirement("LIMITED_COMPANY", "ARTICLES_OF_ASSOCIATION", false, 5, "소유구조 확인 문서 중 1개로 제출할 수 있습니다.", "OWNERSHIP_DOC", "소유구조 확인 문서", 1)
        );
    }

    private List<DocumentRequirement> associationRequirements() {
        return List.of(
                requirement("ASSOCIATION", "ORGANIZATION_IDENTITY_CERTIFICATE", true, 1, "고유번호증을 제출해 주세요.", null, null, null),
                requirement("ASSOCIATION", "REPRESENTATIVE_PROOF_DOCUMENT", true, 2, "대표자 확인서류를 제출해 주세요.", null, null, null),
                requirement("ASSOCIATION", "OPERATING_RULES", false, 3, "규약 문서 중 1개로 제출할 수 있습니다.", "RULE_DOC", "규약 문서", 1),
                requirement("ASSOCIATION", "REGULATIONS", false, 4, "규약 문서 중 1개로 제출할 수 있습니다.", "RULE_DOC", "규약 문서", 1),
                requirement("ASSOCIATION", "ARTICLES_OF_ASSOCIATION", false, 5, "규약 문서 중 1개로 제출할 수 있습니다.", "RULE_DOC", "규약 문서", 1)
        );
    }

    private DocumentRequirement requirement(
            String corporateTypeCode, // 회사 유형 코드
            String documentTypeCode, // 문서 유형 코드
            boolean required, // 단일 필수 여부
            int sortOrder, // 정렬 순서
            String guideMessage, // 제출 안내 문구
            String groupCode, // 선택 필수 그룹 코드
            String groupName, // 선택 필수 그룹 표시명
            Integer minRequiredCount // 그룹 최소 제출 개수
    ) {
        DocumentRequirement requirement = newDocumentRequirement();
        ReflectionTestUtils.setField(requirement, "corporateTypeCode", corporateTypeCode);
        ReflectionTestUtils.setField(requirement, "documentTypeCode", documentTypeCode);
        ReflectionTestUtils.setField(requirement, "required", required);
        ReflectionTestUtils.setField(requirement, "enabled", true);
        ReflectionTestUtils.setField(requirement, "sortOrder", sortOrder);
        ReflectionTestUtils.setField(requirement, "guideMessage", guideMessage);
        ReflectionTestUtils.setField(requirement, "requirementGroupCode", groupCode);
        ReflectionTestUtils.setField(requirement, "requirementGroupName", groupName);
        ReflectionTestUtils.setField(requirement, "minRequiredCount", minRequiredCount);
        return requirement;
    }

    private DocumentRequirement newDocumentRequirement() {
        try {
            java.lang.reflect.Constructor<DocumentRequirement> constructor = DocumentRequirement.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("DocumentRequirement 생성 실패", exception);
        }
    }

    private List<CommonCodeItem> documentTypeCodes() {
        return List.of(
                code("BUSINESS_REGISTRATION", "사업자등록증"),
                code("CORPORATE_REGISTRY", "등기사항전부증명서"),
                code("SHAREHOLDER_REGISTRY", "주주명부"),
                code("STOCK_CHANGE_STATEMENT", "주식변동상황명세서"),
                code("INVESTOR_REGISTRY", "투자자명부"),
                code("MEMBER_REGISTRY", "사원명부"),
                code("ARTICLES_OF_ASSOCIATION", "정관"),
                code("OPERATING_RULES", "운영규정"),
                code("REGULATIONS", "규정"),
                code("ORGANIZATION_IDENTITY_CERTIFICATE", "고유번호증"),
                code("REPRESENTATIVE_PROOF_DOCUMENT", "대표자 확인서류"),
                code("POWER_OF_ATTORNEY", "위임장"),
                code("SEAL_CERTIFICATE", "인감증명서")
        );
    }

    private CommonCodeItem code(
            String code, // 문서 유형 코드
            String codeName // 문서 유형 표시명
    ) {
        return new CommonCodeItem(DOCUMENT_TYPE_GROUP, code, codeName, null, 1);
    }
}
