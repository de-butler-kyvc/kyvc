package com.kyvc.backend.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 문서 미리보기 응답
 *
 * @param previewUrl 문서 미리보기 URL
 * @param expiresAt URL 만료 일시
 */
@Schema(description = "문서 미리보기 응답")
public record DocumentPreviewResponse(
        @Schema(description = "문서 미리보기 URL", example = "/api/user/kyc/applications/1/documents/1/preview-content?expiresAt=2026-05-05T12:10:00")
        String previewUrl, // 문서 미리보기 URL
        @Schema(description = "URL 만료 일시", example = "2026-05-05T12:10:00")
        LocalDateTime expiresAt // URL 만료 일시
) {
}
