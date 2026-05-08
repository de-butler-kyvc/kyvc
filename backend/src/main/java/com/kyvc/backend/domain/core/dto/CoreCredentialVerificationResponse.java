package com.kyvc.backend.domain.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Core Credential 검증 응답
 *
 * @param ok 검증 성공 여부
 * @param errors 오류 목록
 * @param details 상세 데이터
 */
@Schema(description = "Core Credential 검증 응답")
public record CoreCredentialVerificationResponse(
        @Schema(description = "검증 성공 여부", example = "true")
        boolean ok, // 검증 성공 여부
        @Schema(description = "오류 목록")
        List<String> errors, // 오류 목록
        @Schema(description = "상세 데이터")
        Map<String, Object> details // 상세 데이터
) {
}
