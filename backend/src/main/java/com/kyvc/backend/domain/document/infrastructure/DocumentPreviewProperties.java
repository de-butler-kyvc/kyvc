package com.kyvc.backend.domain.document.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 문서 미리보기 정책 설정
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kyvc.document.preview")
public class DocumentPreviewProperties {

    private static final int EXPIRATION_MINUTES_FALLBACK = 10; // 미리보기 만료시간 기본값 분

    private int expirationMinutes = EXPIRATION_MINUTES_FALLBACK;

    public int resolvedExpirationMinutes() {
        return expirationMinutes < 1 ? EXPIRATION_MINUTES_FALLBACK : expirationMinutes;
    }
}
