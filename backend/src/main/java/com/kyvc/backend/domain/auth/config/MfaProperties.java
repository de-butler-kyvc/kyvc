package com.kyvc.backend.domain.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// MFA 정책 설정
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kyvc.auth.mfa")
public class MfaProperties {

    private static final int EXPIRATION_MINUTES_FALLBACK = 5; // 인증코드 만료시간 기본값 분
    private static final int SESSION_EXPIRATION_MINUTES_FALLBACK = 10; // 세션 토큰 만료시간 기본값 분
    private static final int CODE_LENGTH_FALLBACK = 6; // 인증코드 길이 기본값
    private static final int CODE_LENGTH_MIN = 4; // 인증코드 길이 최소값
    private static final int CODE_LENGTH_MAX = 10; // 인증코드 길이 최대값
    private static final int MAX_FAILED_ATTEMPTS_FALLBACK = 5; // 인증 실패 횟수 기본값

    private int expirationMinutes = EXPIRATION_MINUTES_FALLBACK;
    private int sessionExpirationMinutes = SESSION_EXPIRATION_MINUTES_FALLBACK;
    private int codeLength = CODE_LENGTH_FALLBACK;
    private int maxFailedAttempts = MAX_FAILED_ATTEMPTS_FALLBACK;

    public int resolvedExpirationMinutes() {
        return expirationMinutes < 1 ? EXPIRATION_MINUTES_FALLBACK : expirationMinutes;
    }

    public int resolvedSessionExpirationMinutes() {
        return sessionExpirationMinutes < 1 ? SESSION_EXPIRATION_MINUTES_FALLBACK : sessionExpirationMinutes;
    }

    public int resolvedCodeLength() {
        if (codeLength < CODE_LENGTH_MIN || codeLength > CODE_LENGTH_MAX) {
            return CODE_LENGTH_FALLBACK;
        }
        return codeLength;
    }

    public int resolvedMaxFailedAttempts() {
        return maxFailedAttempts < 1 ? MAX_FAILED_ATTEMPTS_FALLBACK : maxFailedAttempts;
    }
}
