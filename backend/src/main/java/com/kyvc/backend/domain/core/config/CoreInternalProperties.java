package com.kyvc.backend.domain.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Core 내부 연동 설정
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kyvc.core")
public class CoreInternalProperties {

    private String mode = "STUB"; // Core 연동 모드
    private String internalApiKey; // 내부 API Key
    private String callbackBaseUrl; // Callback 기준 URL
}
