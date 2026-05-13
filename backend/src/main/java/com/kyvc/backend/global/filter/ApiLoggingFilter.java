package com.kyvc.backend.global.filter;

import com.kyvc.backend.global.jwt.JwtProperties;
import com.kyvc.backend.global.logging.LogMaskingUtil;
import com.kyvc.backend.global.logging.LogMessageBuilder;
import com.kyvc.backend.global.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// API 요청 로그 필터
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LENGTH = 2000;
    private static final int REQUEST_CACHE_LIMIT = MAX_BODY_LENGTH + 1;
    private static final String DISABLED_IN_PROD = "[disabled-in-prod]";
    private static final String VERIFIER_API_KEY_HEADER = "X-API-Key";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final Environment environment;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            FilterChain filterChain // 다음 필터 체인
    ) throws ServletException, IOException {
        long startTime = System.nanoTime(); // 요청 처리 시간 기준점
        String path = request.getRequestURI(); // 쿼리 문자열 제외 경로
        String method = request.getMethod(); // HTTP 메서드
        String requestId = MDC.get(RequestIdFilter.MDC_KEY); // 요청 추적 ID
        boolean bodyLoggingEnabled = isBodyLoggingEnabled(); // 본문 로그 활성 여부
        HttpServletRequest requestToUse = wrapRequestIfNecessary(request, bodyLoggingEnabled); // 로그용 요청 객체
        HttpServletResponse responseToUse = wrapResponseIfNecessary(request, response, bodyLoggingEnabled); // 로그용 응답 객체

        logStart(requestToUse, path, method, requestId, bodyLoggingEnabled);

        Exception caughtException = null; // 필터 체인 예외
        try {
            filterChain.doFilter(requestToUse, responseToUse);
        } catch (Exception exception) {
            caughtException = exception;
            logError(requestToUse, responseToUse, path, method, requestId, startTime, bodyLoggingEnabled, exception);
            throw exception;
        } finally {
            try {
                if (caughtException == null) {
                    logComplete(requestToUse, responseToUse, path, method, requestId, startTime, bodyLoggingEnabled);
                }
            } finally {
                copyBodyToResponse(responseToUse);
            }
        }
    }

    private void logStart(
            HttpServletRequest request, // 요청 정보
            String path, // 요청 경로
            String method, // HTTP 메서드
            String requestId, // 요청 추적 ID
            boolean bodyLoggingEnabled // 본문 로그 활성 여부
    ) {
        LogMessageBuilder builder = LogMessageBuilder.create()
                .level("INFO")
                .env(resolveEnv())
                .requestId(requestId)
                .event("api.request.start")
                .message("API request started")
                .path(path)
                .method(method)
                .fields(buildRequestFields(request))
                .field("requestBodyMasked", resolveRequestBody(request, bodyLoggingEnabled));
        log.info(builder.build());
    }

    private void logComplete(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            String path, // 요청 경로
            String method, // HTTP 메서드
            String requestId, // 요청 추적 ID
            long startTime, // 시작 시각 나노초
            boolean bodyLoggingEnabled // 본문 로그 활성 여부
    ) {
        int status = response.getStatus(); // HTTP 상태
        String responseBody = resolveRawResponseBody(response); // 응답 본문 원문
        String level = resolveLogLevel(status); // 완료 로그 레벨
        LogMessageBuilder builder = LogMessageBuilder.create()
                .level(level)
                .env(resolveEnv())
                .requestId(requestId)
                .event("api.request.complete")
                .message("API request completed")
                .path(path)
                .method(method)
                .status(status)
                .durationMs(resolveDurationMs(startTime))
                .fields(buildRequestFields(request))
                .field("requestBodyMasked", resolveRequestBody(request, bodyLoggingEnabled))
                .field("responseBodyMasked", resolveResponseBody(response, responseBody, bodyLoggingEnabled))
                .field("errorCode", resolveErrorCode(status, responseBody));
        builder.field("authFailureReason", resolveAuthFailureReason(status, responseBody));
        writeLog(level, builder.build(), null);
    }

    private void logError(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            String path, // 요청 경로
            String method, // HTTP 메서드
            String requestId, // 요청 추적 ID
            long startTime, // 시작 시각 나노초
            boolean bodyLoggingEnabled, // 본문 로그 활성 여부
            Exception exception // 필터 체인 예외
    ) {
        String responseBody = resolveRawResponseBody(response); // 응답 본문 원문
        log.error(LogMessageBuilder.create()
                .level("ERROR")
                .env(resolveEnv())
                .requestId(requestId)
                .event("api.request.error")
                .message("API request failed")
                .path(path)
                .method(method)
                .status(resolveErrorStatus(response))
                .durationMs(resolveDurationMs(startTime))
                .exception(exception.getClass().getName())
                .fields(buildRequestFields(request))
                .field("requestBodyMasked", resolveRequestBody(request, bodyLoggingEnabled))
                .field("responseBodyMasked", resolveResponseBody(response, responseBody, bodyLoggingEnabled))
                .field("errorCode", resolveErrorCode(response.getStatus(), responseBody))
                .build(), exception);
    }

    private Map<String, Object> buildRequestFields(HttpServletRequest request // 요청 정보
    ) {
        Map<String, Object> fields = new LinkedHashMap<>(); // 요청 로그 필드
        fields.put("queryString", LogMaskingUtil.maskText(request.getQueryString(), MAX_BODY_LENGTH));
        fields.put("clientIp", resolveClientIp(request));
        fields.put("userAgent", request.getHeader(HttpHeaders.USER_AGENT));
        fields.put("origin", request.getHeader(HttpHeaders.ORIGIN));
        fields.put("contentType", request.getContentType());
        fields.put("hasAccessCookie", hasCookie(request, jwtProperties.getAccessCookieName()));
        fields.put("hasRefreshCookie", hasCookie(request, jwtProperties.getRefreshCookieName()));
        fields.put("hasAuthorizationHeader", StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION)));
        fields.put("hasVerifierApiKey", StringUtils.hasText(request.getHeader(VERIFIER_API_KEY_HEADER)));
        fields.put("hasInternalApiKey", StringUtils.hasText(request.getHeader(INTERNAL_API_KEY_HEADER)));
        fields.put("authenticatedUserId", resolveAuthenticatedUserId());
        return fields;
    }

    private HttpServletRequest wrapRequestIfNecessary(
            HttpServletRequest request, // 요청 정보
            boolean bodyLoggingEnabled // 본문 로그 활성 여부
    ) {
        if (!bodyLoggingEnabled || request instanceof ContentCachingRequestWrapper || !shouldCaptureRequestBody(request)) {
            return request;
        }
        return new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
    }

    private HttpServletResponse wrapResponseIfNecessary(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            boolean bodyLoggingEnabled // 본문 로그 활성 여부
    ) {
        if (!bodyLoggingEnabled || response instanceof ContentCachingResponseWrapper || !shouldCaptureResponseBody(request)) {
            return response;
        }
        return new ContentCachingResponseWrapper(response);
    }

    // 예외 로그용 HTTP 상태 결정
    private int resolveErrorStatus(HttpServletResponse response // 응답 정보
    ) {
        if (response.isCommitted()) {
            return response.getStatus();
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private String resolveRequestBody(
            HttpServletRequest request, // 요청 정보
            boolean bodyLoggingEnabled // 본문 로그 활성 여부
    ) {
        if (!bodyLoggingEnabled && mayHaveRequestBody(request)) {
            return DISABLED_IN_PROD;
        }
        if (!(request instanceof ContentCachingRequestWrapper wrapper) || !isLoggableContentType(request.getContentType())) {
            return null;
        }

        byte[] body = wrapper.getContentAsByteArray(); // 캐시된 요청 본문
        if (body.length == 0) {
            return null;
        }
        return LogMaskingUtil.maskBody(toString(body, request.getCharacterEncoding()), MAX_BODY_LENGTH);
    }

    private String resolveResponseBody(
            HttpServletResponse response, // 응답 정보
            String responseBody, // 응답 본문 원문
            boolean bodyLoggingEnabled // 본문 로그 활성 여부
    ) {
        if (!bodyLoggingEnabled) {
            return DISABLED_IN_PROD;
        }
        if (!(response instanceof ContentCachingResponseWrapper) || !StringUtils.hasText(responseBody)) {
            return null;
        }
        if (!isLoggableResponseContentType(response.getContentType(), responseBody)) {
            return null;
        }
        return LogMaskingUtil.maskBody(responseBody, MAX_BODY_LENGTH);
    }

    private String resolveRawResponseBody(HttpServletResponse response // 응답 정보
    ) {
        if (!(response instanceof ContentCachingResponseWrapper wrapper)) {
            return null;
        }

        byte[] body = wrapper.getContentAsByteArray(); // 캐시된 응답 본문
        if (body.length == 0) {
            return null;
        }
        return toString(body, response.getCharacterEncoding());
    }

    private String resolveErrorCode(
            int status, // HTTP 상태
            String responseBody // 응답 본문 원문
    ) {
        if (status < HttpServletResponse.SC_BAD_REQUEST) {
            return null;
        }
        return LogMaskingUtil.extractCode(responseBody);
    }

    private String resolveAuthFailureReason(
            int status, // HTTP 상태
            String responseBody // 응답 본문 원문
    ) {
        if (status != HttpServletResponse.SC_UNAUTHORIZED && status != HttpServletResponse.SC_FORBIDDEN) {
            return null;
        }
        return LogMaskingUtil.extractCode(responseBody);
    }

    private String resolveClientIp(HttpServletRequest request // 요청 정보
    ) {
        String forwardedFor = request.getHeader("X-Forwarded-For"); // 프록시 전달 IP 목록
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP"); // 프록시 전달 단일 IP
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private Long resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 현재 인증 정보
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal(); // 인증 주체
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        return null;
    }

    private boolean hasCookie(
            HttpServletRequest request, // 요청 정보
            String cookieName // Cookie 이름
    ) {
        Cookie[] cookies = request.getCookies(); // 요청 Cookie 목록
        if (cookies == null || !StringUtils.hasText(cookieName)) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldCaptureRequestBody(HttpServletRequest request // 요청 정보
    ) {
        return mayHaveRequestBody(request)
                && isLoggableContentType(request.getContentType())
                && !isMultipartContentType(request.getContentType());
    }

    private boolean shouldCaptureResponseBody(HttpServletRequest request // 요청 정보
    ) {
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI(); // 요청 경로
        if (path != null) {
            String normalizedPath = path.toLowerCase(Locale.ROOT); // 비교용 요청 경로
            if (normalizedPath.contains("/preview") || normalizedPath.contains("/download")) {
                return false;
            }
        }

        String accept = request.getHeader(HttpHeaders.ACCEPT); // 요청 Accept 헤더
        if (!StringUtils.hasText(accept)) {
            return true;
        }
        String normalizedAccept = accept.toLowerCase(Locale.ROOT); // 비교용 Accept 헤더
        return normalizedAccept.contains("application/json")
                || normalizedAccept.contains("text/")
                || normalizedAccept.contains("*/*");
    }

    private boolean mayHaveRequestBody(HttpServletRequest request // 요청 정보
    ) {
        String method = request.getMethod(); // HTTP 메서드
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean isLoggableResponseContentType(
            String contentType, // 응답 Content-Type
            String responseBody // 응답 본문 원문
    ) {
        if (isLoggableContentType(contentType)) {
            return true;
        }
        String trimmedBody = responseBody == null ? "" : responseBody.trim(); // 공백 제거 본문
        return trimmedBody.startsWith("{") || trimmedBody.startsWith("[");
    }

    private boolean isLoggableContentType(String contentType // Content-Type
    ) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT); // 비교용 Content-Type
        return normalizedContentType.contains("application/json")
                || normalizedContentType.contains("+json")
                || normalizedContentType.startsWith("text/")
                || normalizedContentType.contains("application/x-www-form-urlencoded")
                || normalizedContentType.contains("application/xml")
                || normalizedContentType.contains("+xml");
    }

    private boolean isMultipartContentType(String contentType // Content-Type
    ) {
        return StringUtils.hasText(contentType)
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private String toString(
            byte[] body, // 본문 바이트
            String encoding // 문자 인코딩
    ) {
        return new String(body, resolveCharset(encoding));
    }

    private Charset resolveCharset(String encoding // 문자 인코딩
    ) {
        if (!StringUtils.hasText(encoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException exception) {
            return StandardCharsets.UTF_8;
        }
    }

    private String resolveLogLevel(int status // HTTP 상태
    ) {
        if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            return "ERROR";
        }
        if (status >= HttpServletResponse.SC_BAD_REQUEST) {
            return "WARN";
        }
        return "INFO";
    }

    private void writeLog(
            String level, // 로그 레벨
            String message, // 로그 메시지
            Exception exception // 원인 예외
    ) {
        if ("ERROR".equals(level)) {
            if (exception == null) {
                log.error(message);
                return;
            }
            log.error(message, exception);
            return;
        }
        if ("WARN".equals(level)) {
            log.warn(message);
            return;
        }
        log.info(message);
    }

    private void copyBodyToResponse(HttpServletResponse response // 응답 정보
    ) throws IOException {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            wrapper.copyBodyToResponse();
        }
    }

    private boolean isBodyLoggingEnabled() {
        return !resolveEnv().toLowerCase(Locale.ROOT).contains("prod");
    }

    // 요청 처리 시간 계산
    private long resolveDurationMs(long startTime // 시작 시각 나노초
    ) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    // 현재 실행 환경명 조회
    private String resolveEnv() {
        String[] activeProfiles = environment.getActiveProfiles(); // 활성 Spring 프로필
        if (activeProfiles.length > 0) {
            return String.join(",", activeProfiles);
        }
        return environment.getProperty("spring.profiles.active", "default");
    }
}
