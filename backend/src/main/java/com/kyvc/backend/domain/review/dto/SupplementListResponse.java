package com.kyvc.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 보완요청 목록 응답
 *
 * @param supplements 보완요청 목록
 */
@Schema(description = "보완요청 목록 응답")
public record SupplementListResponse(
        @Schema(description = "보완요청 목록")
        List<SupplementDetailResponse> supplements // 보완요청 목록
) {

    public SupplementListResponse {
        supplements = supplements == null ? List.of() : List.copyOf(supplements);
    }
}
