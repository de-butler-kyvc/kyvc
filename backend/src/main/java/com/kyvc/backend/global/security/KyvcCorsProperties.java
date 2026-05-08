package com.kyvc.backend.global.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

// CORS 설정 속성
@Getter
@Setter
@ConfigurationProperties(prefix = "kyvc.cors")
public class KyvcCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
    )); // 허용 Origin 목록

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*"
    )); // 허용 Origin Pattern 목록

    private boolean allowCredentials = true; // 인증정보 포함 허용 여부

    private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    )); // 허용 HTTP Method 목록

    private List<String> allowedHeaders = new ArrayList<>(List.of("*")); // 허용 Header 목록

    private List<String> exposedHeaders = new ArrayList<>(List.of("X-Request-Id")); // 노출 Header 목록

    private long maxAge = 3600L; // Preflight 캐시 시간
}
