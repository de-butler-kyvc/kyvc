package com.kyvc.backendadmin.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger 테스트 문서 설정
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth"; // JWT Bearer 인증 스키마명

    // OpenAPI 기본 정보와 JWT 인증 스키마
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
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
    }
}
