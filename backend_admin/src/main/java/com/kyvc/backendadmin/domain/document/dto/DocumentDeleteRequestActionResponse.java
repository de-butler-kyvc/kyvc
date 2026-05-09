package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 원본서류 삭제 요청 처리 결과 응답입니다.
 */
@Schema(description = "원본서류 삭제 요청 처리 결과")
public record DocumentDeleteRequestActionResponse(
        /** 삭제 요청 ID */
        @Schema(description = "삭제 요청 ID", example = "1")
        Long requestId,

        /** 문서 ID */
        @Schema(description = "문서 ID", example = "20")
        Long documentId,

        /** 처리 후 삭제 요청 상태 */
        @Schema(description = "처리 후 삭제 요청 상태", example = "APPROVED")
        String status,

        /** 처리 후 문서 업로드 상태 */
        @Schema(description = "처리 후 문서 업로드 상태", example = "DELETED")
        String documentUploadStatus,

        /** 처리 일시 */
        @Schema(description = "처리 일시", example = "2026-05-10T10:30:00")
        String processedAt
) {
}
