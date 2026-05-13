package com.kyvc.backend.domain.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 알림 페이지 정책 설정
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kyvc.notification")
public class NotificationProperties {

    private static final int DEFAULT_PAGE_SIZE_FALLBACK = 20; // 기본 페이지 크기 기본값
    private static final int MAX_PAGE_SIZE_FALLBACK = 100; // 최대 페이지 크기 기본값

    private int defaultPageSize = DEFAULT_PAGE_SIZE_FALLBACK;
    private int maxPageSize = MAX_PAGE_SIZE_FALLBACK;

    public int resolvedDefaultPageSize() {
        int resolvedMaxPageSize = resolvedMaxPageSize(); // 보정 최대 페이지 크기
        if (defaultPageSize < 1) {
            return Math.min(DEFAULT_PAGE_SIZE_FALLBACK, resolvedMaxPageSize);
        }
        return Math.min(defaultPageSize, resolvedMaxPageSize);
    }

    public int resolvedMaxPageSize() {
        if (maxPageSize < 1) {
            return MAX_PAGE_SIZE_FALLBACK;
        }
        return maxPageSize;
    }
}
