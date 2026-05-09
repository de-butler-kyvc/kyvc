package com.kyvc.backend.domain.core.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Core Adapter 설정
@Configuration
@EnableConfigurationProperties(CoreProperties.class)
public class CoreAdapterConfig {

    // Core HTTP Client
    @Bean(name = "coreRestClient")
    public RestClient coreRestClient(
            CoreProperties coreProperties // Core 설정
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory(); // HTTP 요청 팩토리
        requestFactory.setConnectTimeout(coreProperties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(coreProperties.getReadTimeoutMillis());

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory);

        if (coreProperties.getBaseUrl() != null && !coreProperties.getBaseUrl().isBlank()) {
            builder.baseUrl(coreProperties.getBaseUrl().trim());
        }

        return builder.build();
    }
}
