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
    ));

    private List<String> allowedOriginPatterns = new ArrayList<>();
}
