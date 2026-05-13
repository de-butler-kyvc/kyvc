package com.kyvc.backend.global.filter;

import com.kyvc.backend.global.jwt.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ApiLoggingFilterTest {

    @Test
    void loggingFilterMasksRequestAndResponseBodyAndKeepsResponse(CapturedOutput output) throws Exception {
        JwtProperties jwtProperties = new JwtProperties();
        ApiLoggingFilter filter = new ApiLoggingFilter(new MockEnvironment().withProperty("spring.profiles.active", "dev"), jwtProperties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:3000");
        request.addHeader(HttpHeaders.USER_AGENT, "JUnit");
        request.setCookies(new Cookie(jwtProperties.getRefreshCookieName(), "refresh-token-raw"));
        request.setContent("""
                {"email":"user@example.com","password":"plain-password"}
                """.getBytes(StandardCharsets.UTF_8));
        FilterChain filterChain = (wrappedRequest, wrappedResponse) -> {
            ServletInputStream inputStream = wrappedRequest.getInputStream(); // 요청 본문 소비
            inputStream.readAllBytes();
            wrappedResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            wrappedResponse.getWriter().write("""
                    {"success":true,"code":"SUCCESS","data":{"accessToken":"access-token-raw"}}
                    """);
        };

        MDC.put(RequestIdFilter.MDC_KEY, "test-request-id");
        try {
            filter.doFilter(request, response, filterChain);
        } finally {
            MDC.remove(RequestIdFilter.MDC_KEY);
        }

        assertThat(response.getContentAsString())
                .contains("access-token-raw");
        assertThat(output)
                .contains("\"event\":\"api.request.start\"")
                .contains("\"event\":\"api.request.complete\"")
                .contains("\"requestId\":\"test-request-id\"")
                .contains("\"hasRefreshCookie\":true")
                .contains("\\\"password\\\":\\\"***\\\"")
                .contains("\\\"accessToken\\\":\\\"***\\\"")
                .contains("u***@example.com")
                .doesNotContain("plain-password")
                .doesNotContain("access-token-raw")
                .doesNotContain("refresh-token-raw")
                .doesNotContain("user@example.com");
    }

    @Test
    void loggingFilterDoesNotLogMultipartBody(CapturedOutput output) throws Exception {
        ApiLoggingFilter filter = new ApiLoggingFilter(new MockEnvironment().withProperty("spring.profiles.active", "dev"), new JwtProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/documents");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        request.setContent("file=document-raw-content".getBytes(StandardCharsets.UTF_8));
        FilterChain filterChain = (wrappedRequest, wrappedResponse) -> {
            wrappedResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            wrappedResponse.getWriter().write("{\"success\":true,\"code\":\"SUCCESS\"}");
        };

        filter.doFilter(request, response, filterChain);

        assertThat(output)
                .contains("\"event\":\"api.request.complete\"")
                .doesNotContain("document-raw-content");
    }
}
