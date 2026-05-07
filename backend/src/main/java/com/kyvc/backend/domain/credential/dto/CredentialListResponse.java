package com.kyvc.backend.domain.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 사용자 Credential 목록 응답
 *
 * @param credentials Credential 요약 목록
 * @param totalCount Credential 총 건수
 */
@Schema(description = "사용자 Credential 목록 응답")
public record CredentialListResponse(
        @Schema(description = "Credential 요약 목록")
        List<CredentialSummaryResponse> credentials, // Credential 요약 목록
        @Schema(description = "Credential 총 건수", example = "2")
        int totalCount // Credential 총 건수
) {
}

