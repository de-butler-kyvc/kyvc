package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.domain.CorporateAgent;
import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;
import com.kyvc.backend.domain.corporate.repository.CorporateAgentRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepresentativeRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialClaimsAssemblerTest {

    @Mock
    private CorporateRepository corporateRepository;

    @Mock
    private CorporateRepresentativeRepository corporateRepresentativeRepository;

    @Mock
    private CorporateAgentRepository corporateAgentRepository;

    @Mock
    private KycDocumentRepository kycDocumentRepository;

    @Mock
    private LogEventLogger logEventLogger;

    private CredentialClaimsAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new CredentialClaimsAssembler(
                corporateRepository,
                corporateRepresentativeRepository,
                corporateAgentRepository,
                kycDocumentRepository,
                new CoreProperties(),
                logEventLogger
        );
    }

    @Test
    void assemble_returnsClaimsFromPersistedEntityFieldsOnly() {
        Corporate corporate = createCorporate();
        KycApplication kycApplication = createApprovedKyc();
        CorporateRepresentative representative = CorporateRepresentative.create(
                20L,
                "홍길동",
                LocalDate.of(1990, 1, 1),
                "KR",
                "010-0000-0000",
                "rep@example.com",
                500L
        );
        CorporateAgent agent = CorporateAgent.create(
                20L,
                "김대리",
                LocalDate.of(1992, 3, 1),
                "010-1111-2222",
                "agent@example.com",
                "KYC_APPLICATION",
                600L
        );
        agent.updateAuthority(
                "KYC_APPLICATION",
                KyvcEnums.AgentAuthorityStatus.ACTIVE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
        KycDocument document = createDocument();

        when(corporateRepository.findById(20L)).thenReturn(Optional.of(corporate));
        when(corporateRepresentativeRepository.findByCorporateId(20L)).thenReturn(Optional.of(representative));
        when(corporateAgentRepository.findByCorporateId(20L)).thenReturn(List.of(agent));
        when(kycDocumentRepository.findByKycId(10L)).thenReturn(List.of(document));

        Map<String, Object> claims = assembler.assemble(kycApplication);

        Map<String, Object> legalEntity = map(claims.get("legalEntity"));
        assertThat(legalEntity.get("name")).isEqualTo("KYVC Corp");
        assertThat(legalEntity.get("registrationNumber")).isEqualTo("123-45-67890");
        assertThat(legalEntity).doesNotContainKey("businessRegistrationNumber");
        assertThat(legalEntity.get("corporateRegistrationNumber")).isEqualTo("110111-1234567");
        assertThat(legalEntity.get("type")).isEqualTo("CORPORATION");
        assertThat(legalEntity.get("establishedDate")).isEqualTo("2024-01-01");
        assertThat(legalEntity.get("address")).isEqualTo("서울시 중구");
        assertThat(legalEntity.get("businessType")).isEqualTo("IT");

        Map<String, Object> kyc = map(claims.get("kyc"));
        assertThat(kyc.get("status")).isEqualTo("APPROVED");
        assertThat(kyc.get("jurisdiction")).isEqualTo("KR");
        assertThat(kyc.get("assuranceLevel")).isEqualTo("STANDARD");
        assertThat(kyc.get("applicationChannel")).isEqualTo("ONLINE");

        Map<String, Object> representativeClaims = map(claims.get("representative"));
        assertThat(representativeClaims.get("name")).isEqualTo("홍길동");
        assertThat(representativeClaims.get("birthDate")).isEqualTo("1990-01-01");
        assertThat(representativeClaims.get("nationality")).isEqualTo("KR");
        assertThat(representativeClaims).doesNotContainKey("englishName");

        Map<String, Object> delegate = map(claims.get("delegate"));
        assertThat(delegate.get("name")).isEqualTo("김대리");
        assertThat(delegate.get("birthDate")).isEqualTo("1992-03-01");
        assertThat(delegate).doesNotContainKey("nationality");

        Map<String, Object> delegation = map(claims.get("delegation"));
        assertThat(delegation.get("authorityScope")).isEqualTo("KYC_APPLICATION");
        assertThat(delegation.get("status")).isEqualTo("ACTIVE");
        assertThat(delegation.get("validFrom")).isEqualTo("2026-01-01");
        assertThat(delegation.get("validUntil")).isEqualTo("2026-12-31");

        Map<String, Object> extra = map(claims.get("extra"));
        assertThat(extra.get("corporateRegistrationNumber")).isEqualTo("110111-1234567");
        assertThat(extra.get("aiReviewStatus")).isEqualTo("SUCCESS");
        assertThat(extra.get("aiReviewResult")).isEqualTo("PASS");
        assertThat(extra.get("aiConfidenceScore")).isEqualTo(new BigDecimal("0.95"));
        assertThat(extra.get("aiReviewSummary")).isEqualTo("AI 심사 요약");
        assertThat(extra).doesNotContainKey("aiReviewDetailJson");
        assertThat(extra).doesNotContainKey("assessmentId");

        assertThat(claims).doesNotContainKey("documents");
        List<Map<String, Object>> documentEvidence = documentEvidence(claims);
        assertThat(documentEvidence).hasSize(1);
        assertThat(documentEvidence.get(0).get("documentId")).isEqualTo("urn:kyvc:doc:1");
        assertThat(documentEvidence.get(0).get("documentType")).isEqualTo("BUSINESS_REGISTRATION");
        assertThat(documentEvidence.get(0).get("documentClass")).isEqualTo("BUSINESS_REGISTRATION");
        assertThat(documentEvidence.get(0).get("digestSRI")).isEqualTo("sha256-document");
        assertThat(documentEvidence.get(0).get("mediaType")).isEqualTo("application/pdf");
        assertThat(documentEvidence.get(0).get("byteSize")).isEqualTo(123456L);
        assertThat(documentEvidence.get(0).get("uploadedAt")).isEqualTo("2026-05-13T10:00");
        assertThat(documentEvidence.get(0)).doesNotContainKeys("fileName", "filePath", "originalName", "hashInput", "evidenceFor");

        assertThat(claims).doesNotContainKeys("additionalProp1", "beneficialOwners", "establishmentPurpose");
    }

    private Corporate createCorporate() {
        Corporate corporate = Corporate.create(
                1L,
                "KYVC Corp",
                "123-45-67890",
                "110111-1234567",
                "CORPORATION",
                LocalDate.of(2024, 1, 1),
                "02-0000-0000",
                "기본 대표자",
                "010-9999-0000",
                "fallback-rep@example.com",
                "서울시 중구",
                null,
                "IT",
                KyvcEnums.CorporateStatus.ACTIVE
        );
        corporate.updateAgent("기본 대리인", "010-9999-1111", "fallback-agent@example.com", "FALLBACK_SCOPE");
        ReflectionTestUtils.setField(corporate, "corporateId", 20L);
        return corporate;
    }

    private KycApplication createApprovedKyc() {
        KycApplication kycApplication = KycApplication.createDraft(20L, 1L, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", 10L);
        kycApplication.completeAiReviewAsApproved(
                new BigDecimal("0.95"),
                "AI 심사 요약",
                "{\"raw\":true}",
                LocalDateTime.of(2026, 5, 13, 10, 0)
        );
        return kycApplication;
    }

    private KycDocument createDocument() {
        KycDocument document = KycDocument.createUploaded(
                10L,
                "BUSINESS_REGISTRATION",
                "business.pdf",
                "/storage/kyc/business.pdf",
                "application/pdf",
                123456L,
                "sha256-document",
                KyvcEnums.UploadActorType.USER,
                1L
        );
        ReflectionTestUtils.setField(document, "documentId", 1L);
        ReflectionTestUtils.setField(document, "uploadedAt", LocalDateTime.of(2026, 5, 13, 10, 0));
        return document;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> documentEvidence(Map<String, Object> claims) {
        return (List<Map<String, Object>>) claims.get("documentEvidence");
    }
}
