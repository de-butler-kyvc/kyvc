package com.kyvc.backend.domain.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Locale;

// Core 연동 설정
@Getter
@Setter
@ConfigurationProperties(prefix = "kyvc.core")
public class CoreProperties {

    private String mode = "mock"; // Core 연동 모드
    private String baseUrl; // Core Base URL
    private Integer connectTimeoutMillis = 3000; // 연결 타임아웃 밀리초
    private Integer readTimeoutMillis = 10000; // 응답 타임아웃 밀리초
    private boolean devSeedEnabled = false; // 개발 seed 허용 여부
    private boolean failureFallbackEnabled = false; // 장애 fallback 허용 여부
    private String apiKey; // Core API Key
    private String internalApiKey; // 내부 API Key 호환 필드

    // 정규화 모드 조회
    public String normalizedMode() {
        if (!StringUtils.hasText(mode)) {
            return "mock";
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
}
