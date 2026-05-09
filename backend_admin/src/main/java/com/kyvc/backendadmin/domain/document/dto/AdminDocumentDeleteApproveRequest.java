package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문서 삭제 요청 승인 요청")
public record AdminDocumentDeleteApproveRequest(
        @Schema(description = "처리 사유", example = "삭제 요청 사유가 타당하여 승인합니다.")
        String processedReason
) {
}
