package com.kyvc.backendadmin.domain.credential.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Backend Credential 위임 API 호출 설정입니다.
 *
 * <p>backend_admin은 core를 직접 호출하지 않고 backend API에만 요청을 위임하므로,
 * backend base URL과 VC 재발급/폐기 경로를 설정으로 관리합니다.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "kyvc.backend")
public class BackendCredentialProperties {

    private String baseUrl = "http://localhost:8080"; // 로컬 backend 기본 주소입니다.
    private String internalApiKey; // backend 내부 API 보호가 필요한 경우 전달할 키입니다.
    private String reissuePath = "/api/internal/dev/credentials/{credentialId}/reissue"; // Backend VC 재발급 위임 경로입니다.
    private String revokePath = "/api/internal/dev/credentials/{credentialId}/revoke"; // Backend VC 폐기 위임 경로입니다.
    private int connectTimeoutMillis = 3000; // Backend 연결 제한 시간입니다.
    private int readTimeoutMillis = 10000; // Backend 응답 제한 시간입니다.
}
