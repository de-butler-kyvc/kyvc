package com.kyvc.backend.domain.core.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.infrastructure.dto.IssueKycCredentialApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.VerifyPresentationApiRequest;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoreHttpAdapterTest {

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
}
