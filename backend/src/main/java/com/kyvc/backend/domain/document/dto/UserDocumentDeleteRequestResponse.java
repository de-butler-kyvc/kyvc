package com.kyvc.backend.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사용자 문서 삭제 요청 응답
 *
 * @param requestId 삭제 요청 ID
 * @param documentId 문서 ID
 * @param requestedByUserId 요청 사용자 ID
 * @param status 삭제 요청 상태 코드
 * @param reason 삭제 요청 사유
 * @param requestedAt 요청 일시
 */
@Schema(description = "사용자 문서 삭제 요청 응답")
public record UserDocumentDeleteRequestResponse(
        @Schema(description = "삭제 요청 ID", example = "1")
        Long requestId, // 삭제 요청 ID
        @Schema(description = "문서 ID", example = "10")
        Long documentId, // 문서 ID
        @Schema(description = "요청 사용자 ID", example = "1")
        Long requestedByUserId, // 요청 사용자 ID
        @Schema(description = "삭제 요청 상태 코드", example = "REQUESTED")
        String status, // 삭제 요청 상태 코드
        @Schema(description = "삭제 요청 사유", example = "잘못 업로드한 문서")
        String reason, // 삭제 요청 사유
        @Schema(description = "요청 일시", example = "2026-05-11T12:00:00")
        LocalDateTime requestedAt // 요청 일시
) {
}
