package com.kyvc.backendadmin.global.config;

import com.kyvc.backendadmin.global.security.AdminSecurityPatterns;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

// Swagger 테스트 문서 설정
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth"; // JWT Bearer 인증 스키마명

    private final String serverUrl;
    private final String serverDescription;

    public SwaggerConfig(@Value("${kyvc.openapi.server-url:}") String serverUrl,
                         @Value("${kyvc.openapi.server-description:KYvC backendadmin API}") String serverDescription) {
        this.serverUrl = serverUrl;
        this.serverDescription = serverDescription;
    }

    // OpenAPI 기본 정보와 JWT 인증 스키마
    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("KYvC backendadmin API")
                        .version("v1")
                        .description("KYvC backendadmin API 테스트용 Swagger 문서"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
        if (StringUtils.hasText(serverUrl)) {
            openAPI.setServers(List.of(new Server()
                    .url(serverUrl.trim())
                    .description(serverDescription)));
        }
        return openAPI;
    }

    @Bean
    public OpenApiCustomizer adminAuthSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperationsMap().forEach((method, operation) ->
                            applySecurityRequirement(method, path, operation)));
        };
    }

    private void applySecurityRequirement(PathItem.HttpMethod method, String path, Operation operation) {
        if (AdminSecurityPatterns.isPublicRequest(method.name(), path)) {
            operation.setSecurity(List.of());
            return;
        }
        operation.setSecurity(List.of(new SecurityRequirement().addList(BEARER_AUTH)));
    }
}
