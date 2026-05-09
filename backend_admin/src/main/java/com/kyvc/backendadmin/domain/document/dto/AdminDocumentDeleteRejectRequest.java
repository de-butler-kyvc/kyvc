package com.kyvc.backendadmin.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문서 삭제 요청 반려 요청")
public record AdminDocumentDeleteRejectRequest(
        @Schema(description = "처리 사유", example = "심사 진행 중인 필수 문서이므로 삭제할 수 없습니다.")
        String processedReason
) {
}
