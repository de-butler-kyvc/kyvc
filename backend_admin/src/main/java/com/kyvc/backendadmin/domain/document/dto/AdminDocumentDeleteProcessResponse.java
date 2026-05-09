package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "문서 삭제 요청 처리 결과")
public record AdminDocumentDeleteProcessResponse(
        @Schema(description = "삭제 요청 ID", example = "1")
        Long requestId,
        @Schema(description = "문서 ID", example = "100")
        Long documentId,
        @Schema(description = "처리 전 요청 상태 코드", example = "REQUESTED")
        String beforeRequestStatusCode,
        @Schema(description = "처리 후 요청 상태 코드", example = "APPROVED")
        String afterRequestStatusCode,
        @Schema(description = "처리 전 문서 업로드 상태 코드", example = "UPLOADED")
        String beforeUploadStatusCode,
        @Schema(description = "처리 후 문서 업로드 상태 코드", example = "DELETED")
        String afterUploadStatusCode,
        @Schema(description = "처리 관리자 ID", example = "1")
        Long processedByAdminId,
        @Schema(description = "처리 일시")
        LocalDateTime processedAt
) {
}
