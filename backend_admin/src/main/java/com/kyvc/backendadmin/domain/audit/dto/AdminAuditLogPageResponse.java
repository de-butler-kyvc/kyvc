package com.kyvc.backendadmin.domain.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 감사로그 목록의 페이지 정보를 전달하는 DTO입니다.
 */
@Schema(description = "감사로그 페이지 정보")
public record AdminAuditLogPageResponse(
        @Schema(description = "페이지 번호", example = "0")
        int number,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "전체 건수", example = "100")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {
}
