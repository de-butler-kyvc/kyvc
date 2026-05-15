package com.kyvc.backend.global.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskingUtilTest {

    @Test
    void maskBodyMasksPasswordFields() {
        String body = """
                {
                  "email": "user@example.com",
                  "password": "plain-password",
                  "currentPassword": "old-password",
                  "newPassword": "new-password",
                  "newPasswordConfirm": "new-password"
                }
                """;

        String maskedBody = LogMaskingUtil.maskBody(body, 2000);

        assertThat(maskedBody)
                .contains("\"password\":\"***\"")
                .contains("\"currentPassword\":\"***\"")
                .contains("\"newPassword\":\"***\"")
                .contains("\"newPasswordConfirm\":\"***\"")
                .contains("u***@example.com")
                .doesNotContain("plain-password")
                .doesNotContain("old-password")
                .doesNotContain("new-password")
                .doesNotContain("user@example.com");
    }

    @Test
    void maskBodyMasksTokenAndVerificationFields() {
        String body = """
                {
                  "accessToken": "access-token-raw",
                  "refreshToken": "refresh-token-raw",
                  "verificationCode": "123456",
                  "mfaToken": "mfa-token-raw"
                }
                """;

        String maskedBody = LogMaskingUtil.maskBody(body, 2000);

        assertThat(maskedBody)
                .contains("\"accessToken\":\"***\"")
                .contains("\"refreshToken\":\"***\"")
                .contains("\"verificationCode\":\"***\"")
                .contains("\"mfaToken\":\"***\"")
                .doesNotContain("access-token-raw")
                .doesNotContain("refresh-token-raw")
                .doesNotContain("123456")
                .doesNotContain("mfa-token-raw");
    }

    @Test
    void maskBodyMasksVpJwtAndNestedCredentialFields() {
        String body = """
                {
                  "vpJwt": "vp-jwt-raw",
                  "payload": {
                    "credentialJwt": "credential-jwt-raw",
                    "credential": "credential-raw",
                    "presentation": "presentation-raw",
                    "documentContent": "document-content-raw"
                  }
                }
                """;

        String maskedBody = LogMaskingUtil.maskBody(body, 2000);

        assertThat(maskedBody)
                .contains("\"vpJwt\":\"***\"")
                .contains("\"credentialJwt\":\"***\"")
                .contains("\"credential\":\"***\"")
                .contains("\"presentation\":\"***\"")
                .contains("\"documentContent\":\"***\"")
                .doesNotContain("vp-jwt-raw")
                .doesNotContain("credential-jwt-raw")
                .doesNotContain("credential-raw")
                .doesNotContain("presentation-raw")
                .doesNotContain("document-content-raw");
    }

    @Test
    void maskBodyMasksDocumentAttachmentBase64Fields() {
        String body = """
                {
                  "data": {
                    "documentAttachments": [
                      {
                        "fileName": "business_registration.pdf",
                        "contentBase64": "JVBERi0xLjQKraw-pdf"
                      }
                    ]
                  }
                }
                """;

        String maskedBody = LogMaskingUtil.maskBody(body, 2000);

        assertThat(maskedBody)
                .contains("\"contentBase64\":\"***\"")
                .doesNotContain("JVBERi0xLjQKraw-pdf");
    }

    @Test
    void maskTextMasksFormStyleSensitiveValues() {
        String text = "email=user@example.com&password=plain-password&refreshToken=refresh-token-raw&X-API-Key=api-key-raw";

        String maskedText = LogMaskingUtil.maskText(text, 2000);

        assertThat(maskedText)
                .contains("email=u***@example.com")
                .contains("password=***")
                .contains("refreshToken=***")
                .contains("X-API-Key=***")
                .doesNotContain("plain-password")
                .doesNotContain("refresh-token-raw")
                .doesNotContain("api-key-raw")
                .doesNotContain("user@example.com");
    }

    @Test
    void maskBodyTruncatesLongBody() {
        String body = "{\"message\":\"" + "a".repeat(2100) + "\"}";

        String maskedBody = LogMaskingUtil.maskBody(body, 2000);

        assertThat(maskedBody).endsWith("...[truncated]");
    }
}
