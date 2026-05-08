package com.kyvc.backend.domain.core.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// Core 검증 API 공통 응답 DTO
public record VerificationApiResponse(
        @JsonProperty("ok")
        boolean ok, // 검증 성공 여부
        @JsonProperty("errors")
        List<String> errors, // 오류 목록
        @JsonProperty("details")
        Map<String, Object> details // 상세 데이터
) {
}
