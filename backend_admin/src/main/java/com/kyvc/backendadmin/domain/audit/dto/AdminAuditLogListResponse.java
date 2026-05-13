package com.kyvc.backendadmin.domain.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 감사로그 목록 조회 결과를 data.items와 data.page 구조로 전달하는 DTO입니다.
 */
@Schema(description = "감사로그 목록 응답. data.items에는 감사로그 목록, data.page에는 페이지 정보가 포함됩니다.")
public record AdminAuditLogListResponse(
        @Schema(description = "감사로그 목록")
        List<AdminAuditLogResponse> items,
        @Schema(description = "페이지 정보")
        AdminAuditLogPageResponse page
) {
}
