package com.kyvc.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

// Swagger 테스트 문서 설정
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth"; // JWT Bearer 인증 스키마명

    // OpenAPI 기본 정보와 JWT 인증 스키마
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KYvC Backend API")
                        .version("v1")
                        .description("KYvC Backend API 테스트용 Swagger 문서"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OpenApiCustomizer publicApiOpenApiCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (isPublicApi(method, path)) {
                        operation.setSecurity(List.of());
                    }
                }));
    }

    @Bean
    public ServerBaseUrlCustomizer swaggerServerBaseUrlCustomizer(
            Environment environment // 환경 설정
    ) {
        return (serverBaseUrl, request) -> {
            if (!isHttpsSwaggerProfile(environment)) {
                return serverBaseUrl;
            }

            return toHttpsServerBaseUrl(serverBaseUrl);
        };
    }

    private boolean isHttpsSwaggerProfile(
            Environment environment // 환경 설정
    ) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "prod".equals(profile));
    }

    private String toHttpsServerBaseUrl(
            String serverBaseUrl // Swagger 서버 기본 URL
    ) {
        if (serverBaseUrl == null || !serverBaseUrl.startsWith("http://")) {
            return serverBaseUrl;
        }

        return "https://" + serverBaseUrl.substring("http://".length());
    }

    private boolean isPublicApi(
            PathItem.HttpMethod method, // HTTP 메서드
            String path // API 경로
    ) {
        return (PathItem.HttpMethod.POST.equals(method) && isPublicPostApi(path))
                || (PathItem.HttpMethod.GET.equals(method) && isPublicGetApi(path));
    }

    private boolean isPublicPostApi(
            String path // API 경로
    ) {
        return switch (path) {
            case "/api/auth/signup/corporate",
                 "/api/auth/login",
                 "/api/auth/token/refresh",
                 "/api/auth/password-reset/request",
                 "/api/auth/password-reset/confirm",
                 "/api/auth/dev/token",
                 "/api/mobile/auth/login",
                 "/api/mobile/auth/auto-login" -> true;
            default -> false;
        };
    }

    private boolean isPublicGetApi(
            String path // API 경로
    ) {
        return "/api/common/session".equals(path)
                || "/health".equals(path)
                || "/actuator/health".equals(path)
                || path.startsWith("/actuator/health/")
                || "/actuator/info".equals(path);
    }
}
