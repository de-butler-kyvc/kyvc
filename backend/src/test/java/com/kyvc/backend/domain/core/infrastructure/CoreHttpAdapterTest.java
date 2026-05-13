package com.kyvc.backend.domain.core.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CorePayloadSanitizer;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.IssueKycCredentialApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.IssueKycCredentialApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.VerifyPresentationApiRequest;
import com.kyvc.backend.global.logging.LogEventLogger;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

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
                "did:xrpl:1:rHolder#holder-key-1",
                "KYC_CREDENTIAL"
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
        assertThat(rootNode.get("holder_key_id").asText()).isEqualTo("did:xrpl:1:rHolder#holder-key-1");
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
}
