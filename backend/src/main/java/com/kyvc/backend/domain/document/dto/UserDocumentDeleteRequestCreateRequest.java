package com.kyvc.backend.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 문서 삭제 요청 생성 요청
 *
 * @param reason 삭제 요청 사유
 */
@Schema(description = "사용자 문서 삭제 요청 생성 요청")
public record UserDocumentDeleteRequestCreateRequest(
        @Schema(description = "삭제 요청 사유", example = "잘못 업로드한 문서")
        @NotBlank(message = "삭제 요청 사유는 필수입니다.")
        String reason // 삭제 요청 사유
) {
}
