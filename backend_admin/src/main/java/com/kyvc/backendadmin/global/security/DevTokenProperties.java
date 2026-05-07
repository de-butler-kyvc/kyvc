package com.kyvc.backendadmin.global.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Dev Token 설정 속성
@Getter
@Setter
@ConfigurationProperties(prefix = "kyvc.dev-token")
public class DevTokenProperties {

    private boolean enabled;
    private boolean autoCreateUser = true;
    private String defaultEmail = "dev-corporate-user@kyvc.local";
}
