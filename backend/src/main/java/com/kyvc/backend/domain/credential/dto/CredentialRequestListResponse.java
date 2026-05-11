package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Credential 요청 이력 목록 응답
 *
 * @param items Credential 요청 이력 목록
 * @param credentialRequests Credential 요청 이력 목록
 * @param totalCount 전체 건수
 */
@Schema(description = "Credential 요청 이력 목록 응답")
public record CredentialRequestListResponse(
        @Schema(description = "Credential 요청 이력 목록")
        List<CredentialRequestHistoryResponse> items, // Credential 요청 이력 목록
        @Schema(description = "Credential 요청 이력 목록")
        List<CredentialRequestHistoryResponse> credentialRequests, // 기존 응답 호환 목록
        @Schema(description = "전체 건수", example = "2")
        int totalCount // 전체 건수
) {
    public CredentialRequestListResponse(
            List<CredentialRequestHistoryResponse> items, // Credential 요청 이력 목록
            int totalCount // 전체 건수
    ) {
        this(items, items, totalCount);
    }

    public CredentialRequestListResponse {
        items = items == null ? List.of() : List.copyOf(items);
        credentialRequests = credentialRequests == null ? items : List.copyOf(credentialRequests);
    }
}
