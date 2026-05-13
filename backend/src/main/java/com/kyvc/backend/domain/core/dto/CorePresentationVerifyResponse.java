package com.kyvc.backend.domain.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Core Presentation 검증 응답
 *
 * @param ok ok 기준 검증 성공 여부
 * @param valid valid 기준 검증 성공 여부
 * @param verified verified 기준 검증 성공 여부
 * @param errors 오류 목록
 * @param details 상세 데이터
 * @param message 응답 메시지
 */
@Schema(description = "Core Presentation 검증 응답")
public record CorePresentationVerifyResponse(
        @JsonProperty("ok")
        @Schema(description = "ok 기준 검증 성공 여부", example = "true")
        Boolean ok, // ok 기준 검증 성공 여부
        @JsonProperty("valid")
        @Schema(description = "valid 기준 검증 성공 여부", example = "true")
        Boolean valid, // valid 기준 검증 성공 여부
        @JsonProperty("verified")
        @Schema(description = "verified 기준 검증 성공 여부", example = "true")
        Boolean verified, // verified 기준 검증 성공 여부
        @JsonProperty("errors")
        @Schema(description = "오류 목록")
        List<String> errors, // 오류 목록
        @JsonProperty("details")
        @Schema(description = "상세 데이터")
        Map<String, Object> details, // 상세 데이터
        @JsonProperty("message")
        @Schema(description = "응답 메시지")
        String message // 응답 메시지
) {

    /**
     * @return Core 검증 성공 여부
     */
    @JsonIgnore
    public boolean isVerified() {
        return Boolean.TRUE.equals(ok)
                || Boolean.TRUE.equals(valid)
                || Boolean.TRUE.equals(verified);
    }
}
