package com.kyvc.backend.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 문서 삭제 응답
 *
 * @param deleted 삭제 처리 여부
 */
@Schema(description = "문서 삭제 응답")
public record DocumentDeleteResponse(
        @Schema(description = "삭제 처리 여부", example = "true")
        Boolean deleted // 삭제 처리 여부
) {
}
