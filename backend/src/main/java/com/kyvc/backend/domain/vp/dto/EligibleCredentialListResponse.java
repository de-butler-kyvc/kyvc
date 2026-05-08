package com.kyvc.backend.domain.vp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 제출 가능 Credential 목록 응답
 *
 * @param requestId VP 요청 ID
 * @param credentials 제출 가능 Credential 목록
 * @param totalCount 목록 개수
 */
@Schema(description = "제출 가능 Credential 목록 응답")
public record EligibleCredentialListResponse(
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "제출 가능 Credential 목록")
        List<EligibleCredentialResponse> credentials, // 제출 가능 Credential 목록
        @Schema(description = "목록 개수", example = "1")
        int totalCount // 목록 개수
) {
}
