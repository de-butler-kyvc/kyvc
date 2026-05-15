package com.kyvc.backend.domain.core.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorePayloadSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CorePayloadSanitizer sanitizer = new CorePayloadSanitizer(objectMapper);

    @Test
    void sanitizePayload_masksCredentialAndPresentationRawValues() throws Exception {
        String sanitized = sanitizer.sanitizePayload("""
                {
                  "credentialId": 100,
                  "status": "VALID",
                  "format": "vp+jwt",
                  "credentialJwt": "dev.vc.jwt.100",
                  "vcPayloadJson": "{\\"id\\":\\"vc-001\\"}",
                  "presentation": "compact-vp-jwt",
                  "vpJwt": "compact-vp-jwt",
                  "sdJwtKb": "sd-jwt-kb"
                }
                """);

        JsonNode rootNode = objectMapper.readTree(sanitized);

        assertThat(rootNode.get("credentialId").asLong()).isEqualTo(100L);
        assertThat(rootNode.get("status").asText()).isEqualTo("VALID");
        assertThat(rootNode.get("format").asText()).isEqualTo("vp+jwt");
        assertThat(rootNode.get("credentialJwt").asText()).isEqualTo("[MASKED]");
        assertThat(rootNode.get("vcPayloadJson").asText()).isEqualTo("[MASKED]");
        assertThat(rootNode.get("presentation").asText()).isEqualTo("[MASKED]");
        assertThat(rootNode.get("vpJwt").asText()).isEqualTo("[MASKED]");
        assertThat(rootNode.get("sdJwtKb").asText()).isEqualTo("[MASKED]");
    }

    @Test
    void sanitizeAiReviewResponsePayload_preservesClaimsAndMasksRawSensitiveValues() throws Exception {
        String sanitized = sanitizer.sanitizeAiReviewResponsePayload("""
                {
                  "assessment": {
                    "claims": {
                      "legalEntity": {
                        "name": "KYVC Corp"
                      }
                    },
                    "documentContent": "raw-document-text",
                    "jwt": "header.payload.signature"
                  }
                }
                """);

        JsonNode assessmentNode = objectMapper.readTree(sanitized).get("assessment");

        assertThat(assessmentNode.get("claims").get("legalEntity").get("name").asText()).isEqualTo("KYVC Corp");
        assertThat(assessmentNode.get("documentContent").asText()).isEqualTo("[MASKED]");
        assertThat(assessmentNode.get("jwt").asText()).isEqualTo("[MASKED]");
    }
}
