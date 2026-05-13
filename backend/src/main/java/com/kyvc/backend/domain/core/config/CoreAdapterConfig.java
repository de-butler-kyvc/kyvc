package com.kyvc.backend.domain.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Core Adapter 설정
@Slf4j
@Configuration
@EnableConfigurationProperties(CoreProperties.class)
public class CoreAdapterConfig {

    // Core HTTP Client
    @Bean(name = "coreRestClient")
    public RestClient coreRestClient(
            CoreProperties coreProperties // Core 설정
    ) {
        return buildRestClient(coreProperties, coreProperties.resolvedReadTimeoutMillis());
    }

    // Core AI 심사 HTTP Client
    @Bean(name = "coreAiReviewRestClient")
    public RestClient coreAiReviewRestClient(
            CoreProperties coreProperties // Core 설정
    ) {
        return buildRestClient(coreProperties, coreProperties.resolvedAiReviewReadTimeoutMillis());
    }

    @Bean
    public ApplicationRunner coreTimeoutLogger(
            CoreProperties coreProperties // Core 설정
    ) {
        return arguments -> log.info(
                "Core timeout configured: connectTimeoutSeconds={}, readTimeoutSeconds={}, aiReviewReadTimeoutSeconds={}",
                coreProperties.resolvedConnectTimeoutSeconds(),
                coreProperties.resolvedReadTimeoutSeconds(),
                coreProperties.resolvedAiReviewReadTimeoutSeconds()
        );
    }

    private RestClient buildRestClient(
            CoreProperties coreProperties, // Core 설정
            int readTimeoutMillis // 응답 타임아웃 밀리초
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory(); // HTTP 요청 팩토리
        requestFactory.setConnectTimeout(coreProperties.resolvedConnectTimeoutMillis());
        requestFactory.setReadTimeout(readTimeoutMillis);
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory);

        if (coreProperties.getBaseUrl() != null && !coreProperties.getBaseUrl().isBlank()) {
            builder.baseUrl(coreProperties.getBaseUrl().trim());
        }

        return builder.build();
    }
}
