package com.kyvc.backendadmin.global.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Backend Admin CORS 설정 속성입니다.
 *
 * <p>frontend_admin 브라우저 요청과 Swagger 테스트 요청에서 사용할 Origin, Method, Header를
 * 환경별 프로퍼티로 조정할 수 있도록 제공합니다.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "kyvc.cors")
public class KyvcCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:5173",
            "https://dev-admin-kyvc.khuoo.synology.me",
            "https://admin-kyvc.khuoo.synology.me"
    )); // 로컬 frontend_admin과 개발/운영 관리자 도메인을 허용합니다.

    private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
    )); // 로컬 개발 서버 포트가 바뀌어도 preflight가 통과하도록 허용합니다.

    private boolean allowCredentials = true; // HttpOnly Cookie와 인증 쿠키 전달을 허용합니다.

    private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    )); // 관리자 API에서 사용하는 HTTP Method 목록입니다.

    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Request-Id"
    )); // JWT와 요청 추적에 필요한 Header 목록입니다.

    private List<String> exposedHeaders = new ArrayList<>(List.of(
            "X-Request-Id",
            "Content-Disposition",
            "Content-Length"
    )); // 프론트에서 읽을 수 있는 응답 Header입니다.

    private long maxAge = 3600L; // Preflight 결과 캐시 시간입니다.
}
