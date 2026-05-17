package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;
import com.kyvc.backend.domain.document.domain.DocumentRequirement;
import com.kyvc.backend.domain.document.dto.RequiredDocumentResponse;
import com.kyvc.backend.domain.document.infrastructure.DocumentStorageProperties;
import com.kyvc.backend.domain.document.repository.DocumentRequirementRepository;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequiredDocumentServiceTest {

    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE";

    @Mock
    private KycApplicationRepository kycApplicationRepository;
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    @Mock
    private CommonCodeProvider commonCodeProvider;
    @Mock
    private DocumentRequirementRepository documentRequirementRepository;

    private RequiredDocumentService service;

    @BeforeEach
    void setUp() {
        RequiredDocumentPolicyProvider provider = new RequiredDocumentPolicyProvider(
                documentRequirementRepository,
                commonCodeProvider,
                new DocumentStorageProperties("./build/test-documents", 10, "pdf,jpg,jpeg,png", "application/pdf")
        );
        service = new RequiredDocumentService(
                kycApplicationRepository,
                kycDocumentRepository,
                commonCodeProvider,
                provider
        );
    }

    @Test
    void buildRequiredDocumentResponses_usesDocumentRequirementsAndCommonCodeNames() {
        when(commonCodeProvider.getEnabledCodes(DOCUMENT_TYPE_GROUP)).thenReturn(documentTypeCodes());
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("CORPORATION"))
                .thenReturn(corporationRequirements());

        List<RequiredDocumentResponse> responses = service.buildRequiredDocumentResponses(
                "CORPORATION",
                Set.of("CORPORATE_REGISTRATION", "SHAREHOLDER_LIST")
        );

        assertThat(responses)
                .extracting(
                        RequiredDocumentResponse::documentTypeCode,
                        RequiredDocumentResponse::documentTypeName,
                        RequiredDocumentResponse::required,
                        RequiredDocumentResponse::uploaded,
                        RequiredDocumentResponse::groupCode,
                        RequiredDocumentResponse::groupName,
                        RequiredDocumentResponse::minRequiredCount,
                        RequiredDocumentResponse::groupCandidate
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("BUSINESS_REGISTRATION", "사업자등록증", true, false, null, null, null, false),
                        org.assertj.core.groups.Tuple.tuple("CORPORATE_REGISTRY", "등기사항전부증명서(DB)", true, true, null, null, null, false),
                        org.assertj.core.groups.Tuple.tuple("SHAREHOLDER_REGISTRY", "주주명부(DB)", false, true, "OWNERSHIP_DOC", "소유구조 확인 문서", 1, true),
                        org.assertj.core.groups.Tuple.tuple("STOCK_CHANGE_STATEMENT", "주식변동상황명세서", false, false, "OWNERSHIP_DOC", "소유구조 확인 문서", 1, true),
                        org.assertj.core.groups.Tuple.tuple("POWER_OF_ATTORNEY", "위임장", false, false, null, null, null, false),
                        org.assertj.core.groups.Tuple.tuple("SEAL_CERTIFICATE", "인감증명서", false, false, null, null, null, false)
                );
    }

    @Test
    void buildRequiredDocumentResponses_normalizesJointStockCompanyToCorporation() {
        when(commonCodeProvider.getEnabledCodes(DOCUMENT_TYPE_GROUP)).thenReturn(documentTypeCodes());
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("CORPORATION"))
                .thenReturn(corporationRequirements());

        List<RequiredDocumentResponse> responses = service.buildRequiredDocumentResponses(
                "JOINT_STOCK_COMPANY",
                Set.of()
        );

        assertThat(responses)
                .extracting(RequiredDocumentResponse::documentTypeCode)
                .contains("BUSINESS_REGISTRATION", "CORPORATE_REGISTRY");
    }

    @Test
    void buildRequiredDocumentResponses_hidesInactiveDocumentTypesFromCommonCodes() {
        when(commonCodeProvider.getEnabledCodes(DOCUMENT_TYPE_GROUP)).thenReturn(List.of(
                code("BUSINESS_REGISTRATION", "사업자등록증")
        ));
        when(commonCodeProvider.existsCode(DOCUMENT_TYPE_GROUP, "CORPORATE_REGISTRY")).thenReturn(true);
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("CORPORATION"))
                .thenReturn(List.of(
                        requirement("CORPORATION", "BUSINESS_REGISTRATION", true, 1, "사업자등록증을 제출해 주세요.", null, null, null),
                        requirement("CORPORATION", "CORPORATE_REGISTRY", true, 2, "등기사항전부증명서를 제출해 주세요.", null, null, null)
                ));

        List<RequiredDocumentResponse> responses = service.buildRequiredDocumentResponses(
                "CORPORATION",
                Set.of()
        );

        assertThat(responses)
                .extracting(RequiredDocumentResponse::documentTypeCode)
                .containsExactly("BUSINESS_REGISTRATION");
    }

    @Test
    void buildRequiredDocumentResponses_normalizesLegacyRepresentativeRequirementCode() {
        when(commonCodeProvider.getEnabledCodes(DOCUMENT_TYPE_GROUP)).thenReturn(List.of(
                code("REPRESENTATIVE_PROOF_DOCUMENT", "대표자 확인서류")
        ));
        when(documentRequirementRepository.findEnabledByCorporateTypeCode("ASSOCIATION"))
                .thenReturn(List.of(
                        requirement("ASSOCIATION", "REPRESENTATIVE_ID", true, 1, "대표자 확인서류 제출", null, null, null)
                ));

        List<RequiredDocumentResponse> responses = service.buildRequiredDocumentResponses(
                "ASSOCIATION",
                Set.of("REPRESENTATIVE_ID")
        );

        assertThat(responses)
                .extracting(
                        RequiredDocumentResponse::documentTypeCode,
                        RequiredDocumentResponse::documentTypeName,
                        RequiredDocumentResponse::uploaded
                )
                .containsExactly(org.assertj.core.groups.Tuple.tuple(
                        "REPRESENTATIVE_PROOF_DOCUMENT",
                        "대표자 확인서류",
                        true
                ));
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
                code("CORPORATE_REGISTRY", "등기사항전부증명서(DB)"),
                code("SHAREHOLDER_REGISTRY", "주주명부(DB)"),
                code("STOCK_CHANGE_STATEMENT", "주식변동상황명세서"),
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
