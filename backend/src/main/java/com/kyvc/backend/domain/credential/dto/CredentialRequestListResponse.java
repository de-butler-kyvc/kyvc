package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Credential 요청 이력 목록 응답
 *
 * @param credentialRequests Credential 요청 이력 목록
 * @param totalCount 전체 건수
 */
@Schema(description = "Credential 요청 이력 목록 응답")
public record CredentialRequestListResponse(
        @Schema(description = "Credential 요청 이력 목록")
        List<CredentialRequestHistoryResponse> credentialRequests, // Credential 요청 이력 목록
        @Schema(description = "전체 건수", example = "2")
        int totalCount // 전체 건수
) {
}
