package com.kyvc.backend.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// JWT 설정 속성
@Getter
@Setter
@ConfigurationProperties(prefix = "kyvc.jwt")
public class JwtProperties {

    private String secret = "local-development-secret-key-must-be-changed";
    private long accessTokenExpirationMinutes = 30;
    private long refreshTokenExpirationDays = 14;
    private String issuer = "kyvc-backend";
    private String accessCookieName = "KYVC_ACCESS_TOKEN";
    private String refreshCookieName = "KYVC_REFRESH_TOKEN";
    private boolean secureCookie;
    private String sameSite = "Lax";
    private String cookiePath = "/";
}
