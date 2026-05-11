package com.kyvc.backend.domain.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;

// Core 연동 설정
@Getter
@Setter
@ConfigurationProperties(prefix = "kyvc.core")
public class CoreProperties {

    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_AI_REVIEW_READ_TIMEOUT_SECONDS = 300;
    private static final int MAX_AI_REVIEW_READ_TIMEOUT_SECONDS = 300;

    private String mode = "http"; // Core 연동 모드
    private String baseUrl; // Core Base URL
    private Integer connectTimeoutMillis = 5000; // 연결 타임아웃 밀리초
    private Integer readTimeoutMillis = 180000; // 응답 타임아웃 밀리초
    private String connectTimeoutSeconds = String.valueOf(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    private String readTimeoutSeconds = String.valueOf(DEFAULT_READ_TIMEOUT_SECONDS);
    private String aiReviewReadTimeoutSeconds = String.valueOf(DEFAULT_AI_REVIEW_READ_TIMEOUT_SECONDS);
    private boolean devSeedEnabled = false; // 개발 seed 허용 여부
    private boolean failureFallbackEnabled = false; // 장애 fallback 허용 여부
    private String apiKey; // Core API Key
    private String internalApiKey; // 내부 API Key 호환 필드

    // 정규화 모드 조회
    public String normalizedMode() {
        if (!StringUtils.hasText(mode)) {
            return "http";
        }
        return mode.trim().toLowerCase(Locale.ROOT);
    }

    // Mock 모드 여부
    public boolean isMockMode() {
        return "mock".equals(normalizedMode());
    }

    // HTTP 모드 여부
    public boolean isHttpMode() {
        return "http".equals(normalizedMode());
    }

    // Hybrid 모드 여부
    public boolean isHybridMode() {
        return "hybrid".equals(normalizedMode());
    }

    // HTTP 호출 모드 여부
    public boolean isHttpEnabledMode() {
        return isHttpMode() || isHybridMode();
    }

    // API Key 조회
    public String resolveApiKey() {
        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }
        if (StringUtils.hasText(internalApiKey)) {
            return internalApiKey.trim();
        }
        return null;
    }

    public int resolvedConnectTimeoutSeconds() {
        return normalizePositiveSeconds(connectTimeoutSeconds, DEFAULT_CONNECT_TIMEOUT_SECONDS);
    }

    public int resolvedReadTimeoutSeconds() {
        return normalizePositiveSeconds(readTimeoutSeconds, DEFAULT_READ_TIMEOUT_SECONDS);
    }

    public int resolvedAiReviewReadTimeoutSeconds() {
        int normalized = normalizePositiveSeconds(aiReviewReadTimeoutSeconds, DEFAULT_AI_REVIEW_READ_TIMEOUT_SECONDS);
        return Math.min(normalized, MAX_AI_REVIEW_READ_TIMEOUT_SECONDS);
    }

    public int resolvedConnectTimeoutMillis() {
        return toMillis(resolvedConnectTimeoutSeconds());
    }

    public int resolvedReadTimeoutMillis() {
        return toMillis(resolvedReadTimeoutSeconds());
    }

    public int resolvedAiReviewReadTimeoutMillis() {
        return toMillis(resolvedAiReviewReadTimeoutSeconds());
    }

    private int normalizePositiveSeconds(
            String value, // 타임아웃 초 단위 값
            int defaultValue // 기본 초 단위 값
    ) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed <= 0 ? defaultValue : parsed;
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private int toMillis(
            int seconds // 초 단위 값
    ) {
        return Math.toIntExact(Duration.ofSeconds(seconds).toMillis());
    }
}
