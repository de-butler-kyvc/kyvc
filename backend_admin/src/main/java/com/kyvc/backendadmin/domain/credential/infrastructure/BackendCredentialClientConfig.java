package com.kyvc.backendadmin.domain.credential.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Backend Credential 위임 API용 HTTP Client 설정입니다.
 */
@Configuration
@EnableConfigurationProperties(BackendCredentialProperties.class)
public class BackendCredentialClientConfig {

    /**
     * Backend API 호출에 사용할 RestClient를 생성합니다.
     *
     * @param properties Backend 호출 설정
     * @return Backend Credential RestClient
     */
    @Bean
    public RestClient backendCredentialRestClient(BackendCredentialProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory);
        if (properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()) {
            builder.baseUrl(properties.getBaseUrl().trim());
        }
        return builder.build();
    }
}
