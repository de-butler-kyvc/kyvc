package com.kyvc.backend.domain.core.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.infrastructure.dto.VerifyPresentationApiRequest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoreHttpAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
