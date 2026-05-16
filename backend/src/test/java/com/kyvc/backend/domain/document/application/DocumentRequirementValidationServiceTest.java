package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.document.domain.DocumentRequirementValidationResult;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentRequirementValidationServiceTest {

    private DocumentRequirementValidationService service;

    @BeforeEach
    void setUp() {
        RequiredDocumentPolicyProvider provider = new RequiredDocumentPolicyProvider(
                new DocumentStorageProperties("./build/test-documents", 10, "pdf,jpg,jpeg,png", "application/pdf")
        );
        service = new DocumentRequirementValidationService(provider);
    }

    @Test
    void documentTypes_containExistingAndNewPolicyCodes() {
        Set<String> documentTypes = Arrays.stream(KyvcEnums.DocumentType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(documentTypes)
                .hasSize(19)
                .contains(
                        "BUSINESS_REGISTRATION",
                        "CORPORATE_REGISTRY",
                        "SHAREHOLDER_REGISTRY",
                        "STOCK_CHANGE_STATEMENT",
                        "INVESTOR_REGISTRY",
                        "MEMBER_REGISTRY",
                        "BOARD_REGISTRY",
                        "ARTICLES_OF_ASSOCIATION",
                        "OPERATING_RULES",
                        "REGULATIONS",
                        "MEETING_MINUTES",
                        "OFFICIAL_LETTER",
                        "PURPOSE_PROOF_DOCUMENT",
                        "ORGANIZATION_IDENTITY_CERTIFICATE",
                        "INVESTMENT_REGISTRATION_CERTIFICATE",
                        "BENEFICIAL_OWNER_PROOF_DOCUMENT",
                        "POWER_OF_ATTORNEY",
                        "SEAL_CERTIFICATE",
                        "REPRESENTATIVE_PROOF_DOCUMENT"
                );
    }

    @Test
    void corporateTypes_containCurrentPolicyCodes() {
        Set<String> corporateTypes = Arrays.stream(KyvcEnums.CorporateType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(corporateTypes)
                .contains(
                        "JOINT_STOCK_COMPANY",
                        "LIMITED_COMPANY",
                        "LIMITED_PARTNERSHIP",
                        "GENERAL_PARTNERSHIP",
                        "NON_PROFIT",
                        "ASSOCIATION",
                        "FOREIGN_COMPANY",
                        "SOLE_PROPRIETOR"
                );
    }

    @Test
    void validateJointStockCompany_acceptsShareholderRegistryOrStockChangeStatement() {
        assertValid(
                "JOINT_STOCK_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "SHAREHOLDER_REGISTRY"
        );
        assertValid(
                "JOINT_STOCK_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "STOCK_CHANGE_STATEMENT"
        );

        DocumentRequirementValidationResult result = validate(
                "JOINT_STOCK_COMPANY",
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
    void validateLimitedCompany_acceptsOneOwnershipDocument() {
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
    void validateLimitedPartnershipAndGeneralPartnership_useSeparatedCorporateTypes() {
        assertValid("LIMITED_PARTNERSHIP", false, "BUSINESS_REGISTRATION", "CORPORATE_REGISTRY", "MEMBER_REGISTRY");
        assertValid("GENERAL_PARTNERSHIP", false, "BUSINESS_REGISTRATION", "CORPORATE_REGISTRY", "MEMBER_REGISTRY");
    }

    @Test
    void validateNonProfit_requiresPurposeProofDocument() {
        assertValid("NON_PROFIT", false, "ARTICLES_OF_ASSOCIATION", "PURPOSE_PROOF_DOCUMENT", "CORPORATE_REGISTRY");

        DocumentRequirementValidationResult result = validate(
                "NON_PROFIT",
                false,
                "ARTICLES_OF_ASSOCIATION",
                "CORPORATE_REGISTRY"
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.missingRequiredItems())
                .extracting("documentTypeCode")
                .containsExactly("PURPOSE_PROOF_DOCUMENT");
    }

    @Test
    void validateAssociation_requiresRepresentativeProofAndOneRuleDocument() {
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

        DocumentRequirementValidationResult missingRule = validate(
                "ASSOCIATION",
                false,
                "ORGANIZATION_IDENTITY_CERTIFICATE",
                "REPRESENTATIVE_PROOF_DOCUMENT"
        );
        DocumentRequirementValidationResult missingRepresentative = validate(
                "ASSOCIATION",
                false,
                "ORGANIZATION_IDENTITY_CERTIFICATE",
                "OPERATING_RULES"
        );

        assertThat(missingRule.unsatisfiedGroups())
                .extracting("groupCode")
                .containsExactly("RULE_DOC");
        assertThat(missingRepresentative.missingRequiredItems())
                .extracting("documentTypeCode")
                .containsExactly("REPRESENTATIVE_PROOF_DOCUMENT");
    }

    @Test
    void validateForeignCompany_requiresInvestmentCertificateAndOwnershipDocument() {
        assertValid(
                "FOREIGN_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "PURPOSE_PROOF_DOCUMENT",
                "INVESTMENT_REGISTRATION_CERTIFICATE",
                "INVESTOR_REGISTRY"
        );

        DocumentRequirementValidationResult result = validate(
                "FOREIGN_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "PURPOSE_PROOF_DOCUMENT",
                "INVESTMENT_REGISTRATION_CERTIFICATE"
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.unsatisfiedGroups())
                .extracting("groupCode")
                .containsExactly("OWNERSHIP_DOC");
    }

    @Test
    void validateAgentApplication_requiresDelegationDocuments() {
        DocumentRequirementValidationResult agentResult = validate(
                "JOINT_STOCK_COMPANY",
                true,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "SHAREHOLDER_REGISTRY"
        );
        DocumentRequirementValidationResult representativeResult = validate(
                "JOINT_STOCK_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "SHAREHOLDER_REGISTRY"
        );

        assertThat(agentResult.valid()).isFalse();
        assertThat(agentResult.missingRequiredItems())
                .extracting("documentTypeCode")
                .containsExactly("POWER_OF_ATTORNEY", "SEAL_CERTIFICATE");
        assertThat(representativeResult.valid()).isTrue();
    }

    @Test
    void validateBeneficialOwnerProofDocument_isNotDefaultRequiredDocument() {
        assertValid(
                "JOINT_STOCK_COMPANY",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRY",
                "SHAREHOLDER_REGISTRY"
        );
    }

    @Test
    void validateLegacyCorporationAndDocumentCodes_remainCompatible() {
        assertValid(
                "CORPORATION",
                false,
                "BUSINESS_REGISTRATION",
                "CORPORATE_REGISTRATION",
                "SHAREHOLDER_LIST"
        );
    }

    @Test
    void validateSoleProprietor_isUnsupportedForCorporateKycPolicy() {
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
}
