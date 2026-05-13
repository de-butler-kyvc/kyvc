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
    void assemble_returnsAiReviewExtractedClaims() {
        KycApplication kycApplication = createApprovedKycWithAiReviewClaims();

        Map<String, Object> claims = assembler.assemble(kycApplication);

        assertThat(claims).containsKeys(
                "kyc",
                "legalEntity",
                "representative",
                "delegate",
                "delegation",
                "beneficialOwners",
                "extra"
        );
        assertThat(claims).doesNotContainKeys("aiReview", "documents", "additionalProp1");

        Map<String, Object> legalEntity = map(claims.get("legalEntity"));
        assertThat(legalEntity.get("name")).isEqualTo("테스트 법인");
        assertThat(legalEntity.get("registrationNumber")).isEqualTo("110111-1234567");

        Map<String, Object> representative = map(claims.get("representative"));
        assertThat(representative.get("name")).isEqualTo("대표자명");
        assertThat(representative.get("birthDate")).isEqualTo("1990-01-01");
        assertThat(representative.get("nationality")).isEqualTo("KR");

        Map<String, Object> delegate = map(claims.get("delegate"));
        assertThat(delegate.get("name")).isEqualTo("대리인명");
        assertThat(delegate.get("contact")).isEqualTo("010-1111-2222");

        Map<String, Object> delegation = map(claims.get("delegation"));
        assertThat(delegation.get("kycApplication")).isEqualTo(true);
        assertThat(delegation.get("documentSubmission")).isEqualTo(true);
        assertThat(delegation.get("vcReceipt")).isEqualTo(true);

        Map<String, Object> extra = map(claims.get("extra"));
        Map<String, Object> aiAssessmentRef = map(extra.get("aiAssessmentRef"));
        assertThat(aiAssessmentRef.get("assessmentId")).isEqualTo("assessment-001");
        assertThat(aiAssessmentRef.get("status")).isEqualTo("NORMAL");

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

    private KycApplication createApprovedKycWithAiReviewClaims() {
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
                          "requestedAt": "2026-05-13T10:00:00",
                          "claims": {
                            "kyc": {
                              "jurisdiction": "KR",
                              "assuranceLevel": "STANDARD"
                            },
                            "legalEntity": {
                              "type": "STOCK_COMPANY",
                              "name": "테스트 법인",
                              "registrationNumber": "110111-1234567"
                            },
                            "representative": {
                              "name": "대표자명",
                              "birthDate": "1990-01-01",
                              "nationality": "KR"
                            },
                            "beneficialOwners": [
                              {
                                "name": "실소유자명",
                                "ownershipPercentage": 75
                              }
                            ],
                            "delegate": {
                              "name": "대리인명",
                              "contact": "010-1111-2222"
                            },
                            "delegation": {
                              "kycApplication": true,
                              "documentSubmission": true,
                              "vcReceipt": true,
                              "validFrom": "2026-01-01",
                              "validUntil": "2026-12-31"
                            },
                            "extra": {
                              "aiAssessmentRef": {
                                "assessmentId": "assessment-001",
                                "applicationId": "10",
                                "status": "NORMAL"
                              }
                            }
                          }
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
