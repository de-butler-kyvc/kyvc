package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.corporate.repository.CorporateAgentRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepresentativeRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private CoreProperties coreProperties;

    @BeforeEach
    void setUp() {
        coreProperties = new CoreProperties();
        assembler = new CredentialClaimsAssembler(
                coreProperties,
                logEventLogger,
                new ObjectMapper()
        );
    }

    @Test
    void assemble_returnsOnlyAiReviewResultClaims() {
        KycApplication kycApplication = createApprovedKycWithAiReview();

        Map<String, Object> claims = assembler.assemble(kycApplication);

        assertThat(claims).containsOnlyKeys("aiReview");
        Map<String, Object> aiReview = map(claims.get("aiReview"));
        assertThat(aiReview.get("coreRequestId")).isEqualTo("core-request-001");
        assertThat(aiReview.get("aiReviewStatus")).isEqualTo("SUCCESS");
        assertThat(aiReview.get("aiReviewResult")).isEqualTo("PASS");
        assertThat(aiReview.get("assessmentStatus")).isEqualTo("NORMAL");
        assertThat(aiReview.get("assessmentId")).isEqualTo("assessment-001");
        assertThat(aiReview.get("confidenceScore")).isEqualTo(new BigDecimal("0.95"));
        assertThat(aiReview.get("summary")).isEqualTo("AI 심사 요약");
        assertThat(aiReview.get("requestedAt")).isEqualTo("2026-05-13T10:00:00");

        assertThat(claims).doesNotContainKeys(
                "kyc",
                "legalEntity",
                "representative",
                "delegate",
                "delegation",
                "extra",
                "documentEvidence",
                "documents",
                "beneficialOwners",
                "establishmentPurpose",
                "additionalProp1"
        );
        verifyNoInteractions(
                corporateRepository,
                corporateRepresentativeRepository,
                corporateAgentRepository,
                kycDocumentRepository
        );
    }

    @Test
    void assemble_rejectsWhenAiReviewResultMissingEvenIfDevSeedEnabled() {
        coreProperties.setDevSeedEnabled(true);
        KycApplication kycApplication = KycApplication.createDraft(20L, 1L, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", 10L);
        kycApplication.approveForDevTest(LocalDateTime.of(2026, 5, 13, 10, 0));

        ApiException exception = assertThrows(ApiException.class, () -> assembler.assemble(kycApplication));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CREDENTIAL_CLAIMS_REQUIRED_DATA_MISSING);
    }

    private KycApplication createApprovedKycWithAiReview() {
        KycApplication kycApplication = KycApplication.createDraft(20L, 1L, "CORPORATION");
        ReflectionTestUtils.setField(kycApplication, "kycId", 10L);
        kycApplication.completeAiReviewAsApproved(
                new BigDecimal("0.95"),
                "AI 심사 요약",
                """
                        {
                          "coreRequestId": "core-request-001",
                          "status": "SUCCESS",
                          "assessmentStatus": "NORMAL",
                          "assessmentId": "assessment-001",
                          "confidenceScore": 0.91,
                          "message": "AI 상세 메시지",
                          "requestedAt": "2026-05-13T10:00:00"
                        }
                        """,
                LocalDateTime.of(2026, 5, 13, 10, 0)
        );
        return kycApplication;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
