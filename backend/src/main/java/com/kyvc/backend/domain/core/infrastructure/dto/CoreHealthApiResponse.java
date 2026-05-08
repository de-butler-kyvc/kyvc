package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Core /health 응답 DTO
public record CoreHealthApiResponse(
        @JsonProperty("status")
        String status, // 상태 값
        @JsonProperty("service")
        String service, // 서비스 이름
        @JsonProperty("environment")
        String environment // 실행 환경
) {
}