package com.kyvc.backend.domain.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// 비밀번호 재설정 정책 설정
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kyvc.auth.password-reset")
public class PasswordResetProperties {

    private static final int EXPIRATION_MINUTES_FALLBACK = 30; // 토큰 만료시간 기본값 분

    private int expirationMinutes = EXPIRATION_MINUTES_FALLBACK;
    private String baseUrl = "";

    public int resolvedExpirationMinutes() {
        return expirationMinutes < 1 ? EXPIRATION_MINUTES_FALLBACK : expirationMinutes;
    }

    public boolean hasBaseUrl() {
        return StringUtils.hasText(baseUrl);
    }
}
