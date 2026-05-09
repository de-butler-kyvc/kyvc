package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Credential 요청 이력 목록 응답
 *
 * @param requests Credential 요청 이력 목록
 * @param totalCount 요청 이력 총 건수
 */
@Schema(description = "Credential 요청 이력 목록 응답")
public record CredentialRequestListResponse(
        @Schema(description = "Credential 요청 이력 목록")
        List<CredentialRequestSummaryResponse> requests, // Credential 요청 이력 목록
        @Schema(description = "요청 이력 총 건수", example = "1")
        int totalCount // 요청 이력 총 건수
) {
}
