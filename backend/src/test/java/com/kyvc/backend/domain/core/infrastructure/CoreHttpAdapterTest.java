package com.kyvc.backend.domain.core.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CorePayloadSanitizer;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.dto.CorePresentationVerifyRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.IssueKycCredentialApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.IssueKycCredentialApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.LlmPrimaryAssessmentApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.VerifyPresentationApiRequest;
import com.kyvc.backend.global.logging.LogEventLogger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CoreHttpAdapterTest {

    private static final String VALID_ISSUER_ACCOUNT = "rf7J73nMdQq3WRh8dPDDRmJtruXk15hnfd";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void issueKycCredentialApiRequest_omitsIssuerOptionalFields() throws Exception {
        IssueKycCredentialApiRequest request = new IssueKycCredentialApiRequest(
                "rHolder",
                "did:xrpl:1:rHolder",
                Map.of("corporateName", "KYVC Corp"),
                OffsetDateTime.parse("2026-05-12T12:00:00Z"),
                OffsetDateTime.parse("2027-05-12T12:00:00Z"),
                true,
                true,
                false,
                null,
                null,
                false,
                "xrpl",
                "jwt",
                "vc+jwt",
                "holder-key-1",
                "https://kyvc.example/vct/legal-entity-kyc-v1"
        );

        JsonNode rootNode = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(rootNode.has("issuer_account")).isFalse();
        assertThat(rootNode.has("issuer_seed")).isFalse();
        assertThat(rootNode.has("issuer_private_key_pem")).isFalse();
        assertThat(rootNode.has("issuer_did")).isFalse();
        assertThat(rootNode.has("key_id")).isFalse();
        assertThat(rootNode.has("store_issuer_did_document")).isFalse();
        assertThat(rootNode.get("holder_account").asText()).isEqualTo("rHolder");
        assertThat(rootNode.get("holder_did").asText()).isEqualTo("did:xrpl:1:rHolder");
        assertThat(rootNode.get("holder_key_id").asText()).isEqualTo("holder-key-1");
        assertThat(rootNode.get("vct").asText()).isEqualTo("https://kyvc.example/vct/legal-entity-kyc-v1");
    }

    @Test
    void mapVcIssuanceResponse_parsesIssuerAccountFromCredentialCreateTransactionAccount() {
        CoreVcIssuanceResponse response = mapIssueResponse(issueResponse(
                Map.of("Account", VALID_ISSUER_ACCOUNT),
                Map.of(),
                Map.of("issuer_did", "did:xrpl:1:rIssuer"),
                null,
                null,
                null
        ), "did:xrpl:1:rIssuer");

        assertThat(response.issuerAccount()).isEqualTo(VALID_ISSUER_ACCOUNT);
        assertThat(response.issuerDid()).isEqualTo("did:xrpl:1:" + VALID_ISSUER_ACCOUNT);
    }

    @Test
    void mapVcIssuanceResponse_parsesIssuerAccountFromCredentialCreateTransactionTxJsonAccount() {
        CoreVcIssuanceResponse response = mapIssueResponse(issueResponse(
                Map.of("tx_json", Map.of("Account", VALID_ISSUER_ACCOUNT)),
                Map.of(),
                Map.of("issuer_did", "did:xrpl:1:rIssuer"),
                null,
                null,
                null
        ), "did:xrpl:1:rIssuer");

        assertThat(response.issuerAccount()).isEqualTo(VALID_ISSUER_ACCOUNT);
    }

    @Test
    void mapVcIssuanceResponse_doesNotFallbackToPlaceholderIssuerDid() {
        CoreVcIssuanceResponse response = mapIssueResponse(issueResponse(
                Map.of(),
                Map.of(),
                Map.of("issuer_did", "did:xrpl:1:rIssuer"),
                null,
                null,
                null
        ), "did:xrpl:1:rIssuer");

        assertThat(response.issuerAccount()).isNull();
    }

    @Test
    void mapVcIssuanceResponse_allowsValidIssuerDidFallback() {
        CoreVcIssuanceResponse response = mapIssueResponse(issueResponse(
                Map.of(),
                Map.of(),
                Map.of("issuer_did", "did:xrpl:1:" + VALID_ISSUER_ACCOUNT),
                null,
                null,
                null
        ), "did:xrpl:1:rIssuer");

        assertThat(response.issuerAccount()).isEqualTo(VALID_ISSUER_ACCOUNT);
    }

    @Test
    void mapVcIssuanceResponse_prefersValidCredentialStatusIdOverGenericId() {
        String holderAccount = "rHolder111111111111111111111111";
        String credentialStatusId = "xrpl:credential:" + VALID_ISSUER_ACCOUNT + ":" + holderAccount + ":KYC_CREDENTIAL";

        CoreVcIssuanceResponse response = mapIssueResponse(issueResponse(
                Map.of(),
                Map.of(),
                Map.of(
                        "id", "generic-status-row-id",
                        "status_id", credentialStatusId,
                        "issuer_did", "did:xrpl:1:rIssuer"
                ),
                null,
                null,
                null
        ), "did:xrpl:1:rIssuer");

        assertThat(response.credentialStatusId()).isEqualTo(credentialStatusId);
        assertThat(response.issuerAccount()).isEqualTo(VALID_ISSUER_ACCOUNT);
        assertThat(response.issuerDid()).isEqualTo("did:xrpl:1:" + VALID_ISSUER_ACCOUNT);
    }

    @Test
    void verifyPresentationApiRequest_serializesVpJwtPayload() throws Exception {
        VerifyPresentationApiRequest request = new VerifyPresentationApiRequest(
                "vp+jwt",
                "compact-vp-jwt",
                null,
                Map.of("requiredClaims", List.of("kycLevel", "jurisdiction")),
                true,
                null,
                false,
                "xrpl"
        );

        JsonNode rootNode = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(rootNode.get("format").asText()).isEqualTo("vp+jwt");
        assertThat(rootNode.get("presentation").asText()).isEqualTo("compact-vp-jwt");
        assertThat(rootNode.get("policy").get("requiredClaims").size()).isEqualTo(2);
        assertThat(rootNode.get("require_status").asBoolean()).isTrue();
        assertThat(rootNode.get("status_mode").asText()).isEqualTo("xrpl");
        assertThat(rootNode.has("sdJwtKb")).isFalse();
        assertThat(rootNode.has("vpJwt")).isFalse();
        assertThat(rootNode.has("aud")).isFalse();
        assertThat(rootNode.has("nonce")).isFalse();
    }

    @Test
    void verifyPresentationApiRequest_serializesSdJwtPayloadWithoutTopLevelSdJwtKb() throws Exception {
        Map<String, Object> presentation = new LinkedHashMap<>();
        presentation.put("format", "kyvc-sd-jwt-presentation-v1");
        presentation.put("aud", "https://dev-api-kyvc.khuoo.synology.me");
        presentation.put("nonce", "nonce-001");
        presentation.put("sdJwtKb", "sd-jwt-kb");
        VerifyPresentationApiRequest request = new VerifyPresentationApiRequest(
                "kyvc-sd-jwt-presentation-v1",
                presentation,
                null,
                Map.of("requiredClaims", List.of("kycLevel", "jurisdiction")),
                true,
                null,
                false,
                "xrpl"
        );

        JsonNode rootNode = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(rootNode.get("format").asText()).isEqualTo("kyvc-sd-jwt-presentation-v1");
        assertThat(rootNode.get("presentation").get("format").asText()).isEqualTo("kyvc-sd-jwt-presentation-v1");
        assertThat(rootNode.get("presentation").get("aud").asText()).isEqualTo("https://dev-api-kyvc.khuoo.synology.me");
        assertThat(rootNode.get("presentation").get("nonce").asText()).isEqualTo("nonce-001");
        assertThat(rootNode.get("presentation").get("sdJwtKb").asText()).isEqualTo("sd-jwt-kb");
        assertThat(rootNode.get("policy").get("requiredClaims").size()).isEqualTo(2);
        assertThat(rootNode.has("sdJwtKb")).isFalse();
        assertThat(rootNode.has("requiredClaims")).isFalse();
        assertThat(rootNode.has("challenge")).isFalse();
    }

    @Test
    void corePresentationVerifyRequest_serializesDidDocumentsForWebVpLogin() throws Exception {
        String holderDid = "did:xrpl:1:rHolder";
        Map<String, Object> didDocument = new LinkedHashMap<>();
        didDocument.put("id", holderDid);
        didDocument.put("verificationMethod", List.of());
        didDocument.put("authentication", List.of());
        Map<String, Map<String, Object>> didDocuments = Map.of(holderDid, didDocument);

        CorePresentationVerifyRequest request = new CorePresentationVerifyRequest(
                "kyvc-sd-jwt-presentation-v1",
                Map.of("format", "kyvc-sd-jwt-presentation-v1", "sdJwtKb", "sd-jwt-kb"),
                didDocuments,
                null,
                true,
                null,
                false,
                "xrpl"
        );

        JsonNode rootNode = objectMapper.readTree(objectMapper.writeValueAsString(request));

        assertThat(rootNode.has("did_documents")).isTrue();
        assertThat(rootNode.get("did_documents").has(holderDid)).isTrue();
        assertThat(rootNode.get("did_documents").get(holderDid).get("id").asText()).isEqualTo(holderDid);
        assertThat(rootNode.get("presentation").get("sdJwtKb").asText()).isEqualTo("sd-jwt-kb");
        assertThat(rootNode.get("status_mode").asText()).isEqualTo("xrpl");
    }

    @Test
    void mapAiReviewResponse_buildsClaimsFromExtractedFields() {
        CoreHttpAdapter adapter = new CoreHttpAdapter(
                mock(RestClient.class),
                mock(RestClient.class),
                mock(CoreProperties.class),
                mock(LogEventLogger.class),
                objectMapper,
                mock(CorePayloadSanitizer.class)
        );
        Map<String, Object> corporateProfile = new LinkedHashMap<>();
        corporateProfile.put("legalName", "테스트 법인");
        corporateProfile.put("corporateRegistrationNumber", "110111-1234567");
        corporateProfile.put("representative", Map.of(
                "name", "대표자명",
                "birthDate", "1990-01-01",
                "nationality", "KR"
        ));
        Map<String, Object> extractedFields = new LinkedHashMap<>();
        extractedFields.put("corporateProfile", corporateProfile);
        extractedFields.put("delegate", Map.of(
                "name", "대리인명",
                "contact", "010-1111-2222"
        ));
        extractedFields.put("delegation", Map.of(
                "kycApplication", true,
                "documentSubmission", true,
                "vcReceipt", true,
                "validFrom", "2026-01-01",
                "validUntil", "2026-12-31"
        ));
        LlmPrimaryAssessmentApiResponse.KycAssessmentApiResponse assessment =
                new LlmPrimaryAssessmentApiResponse.KycAssessmentApiResponse(
                        "assessment-001",
                        "10",
                        "STOCK_COMPANY",
                        "DELEGATE",
                        "NORMAL",
                        new BigDecimal("0.95"),
                        "AI 심사 요약",
                        List.of(),
                        extractedFields,
                        List.of(),
                        Map.of("owners", List.of(Map.of(
                                "name", "실소유자명",
                                "ownershipPercent", 75
                        ))),
                        Map.of("status", "VALID"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of(),
                        "2026-05-13T10:00:00"
                );
        LlmPrimaryAssessmentApiResponse body = new LlmPrimaryAssessmentApiResponse(
                "llm-primary",
                "provider",
                assessment,
                List.of()
        );

        CoreAiReviewResponse response = ReflectionTestUtils.invokeMethod(
                adapter,
                "mapAiReviewResponse",
                aiReviewRequest(),
                body
        );

        Map<String, Object> claims = response.claims();
        assertThat(map(claims.get("legalEntity")).get("name")).isEqualTo("테스트 법인");
        assertThat(map(claims.get("legalEntity")).get("registrationNumber")).isEqualTo("110111-1234567");
        assertThat(map(claims.get("representative")).get("name")).isEqualTo("대표자명");
        assertThat(map(claims.get("delegate")).get("name")).isEqualTo("대리인명");
        assertThat(map(claims.get("delegation")).get("kycApplication")).isEqualTo(true);
        assertThat(map(claims.get("delegation")).get("status")).isEqualTo("VALID");
        assertThat(claims).doesNotContainKey("aiReview");
    }

    private CoreVcIssuanceResponse mapIssueResponse(
            IssueKycCredentialApiResponse body, // Core 발급 응답
            String requestIssuerDid // 요청 Issuer DID
    ) {
        CoreHttpAdapter adapter = new CoreHttpAdapter(
                mock(RestClient.class),
                mock(RestClient.class),
                mock(CoreProperties.class),
                mock(LogEventLogger.class),
                objectMapper,
                mock(CorePayloadSanitizer.class)
        );
        return ReflectionTestUtils.invokeMethod(
                adapter,
                "mapVcIssuanceResponse",
                issueRequest(requestIssuerDid),
                body
        );
    }

    private CoreVcIssuanceRequest issueRequest(
            String issuerDid // 요청 Issuer DID
    ) {
        return new CoreVcIssuanceRequest(
                "core-request-id",
                1L,
                10L,
                20L,
                null,
                null,
                null,
                issuerDid,
                issuerDid + "#issuer-key-1",
                null,
                "rHolder111111111111111111111111",
                "did:xrpl:1:rHolder111111111111111111111111",
                "KYC_CREDENTIAL",
                "STANDARD",
                "KR",
                Map.of("corporateName", "KYVC Corp"),
                OffsetDateTime.parse("2026-05-12T12:00:00Z"),
                OffsetDateTime.parse("2027-05-12T12:00:00Z"),
                true,
                true,
                false,
                true,
                null,
                null,
                false,
                "xrpl",
                "jwt",
                "dc+sd-jwt",
                "did:xrpl:1:rHolder111111111111111111111111#holder-key-1",
                "KYC_CREDENTIAL",
                OffsetDateTime.parse("2026-05-12T12:00:00Z")
        );
    }

    private CoreAiReviewRequest aiReviewRequest() {
        return new CoreAiReviewRequest(
                "core-request-id",
                10L,
                20L,
                "123-45-67890",
                "110111-1234567",
                "테스트 법인",
                "대표자명",
                null,
                null,
                "대리인명",
                "CORPORATION",
                List.of(),
                null
        );
    }

    private IssueKycCredentialApiResponse issueResponse(
            Map<String, Object> credentialCreateTransaction, // CredentialCreate 트랜잭션
            Map<String, Object> ledgerEntry, // Ledger entry
            Map<String, Object> status, // 상태 객체
            String issuerAccount, // issuerAccount 필드
            String issuerAccountSnake, // issuer_account 필드
            String issuer // issuer 필드
    ) {
        return new IssueKycCredentialApiResponse(
                "dc+sd-jwt",
                "header.payload.signature~disclosure-001~",
                "credential-external-id",
                "KYC_CREDENTIAL",
                null,
                null,
                null,
                issuerAccount,
                issuerAccountSnake,
                issuer,
                "vc-hash",
                status,
                credentialCreateTransaction,
                ledgerEntry,
                null,
                "xrpl"
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
