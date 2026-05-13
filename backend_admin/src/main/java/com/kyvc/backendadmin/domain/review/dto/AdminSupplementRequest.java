package com.kyvc.backendadmin.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC 보완요청 생성에 사용하는 DTO입니다.
 */
@Schema(description = "KYC 보완요청 생성 요청")
public record AdminSupplementRequest(
        @Schema(description = "SUPPLEMENT_REASON 공통코드", example = "DOCUMENT_INCOMPLETE")
        @NotBlank
        String supplementReasonCode,
        @Schema(description = "보완요청 제목", example = "사업자등록증 재제출 요청")
        String title,
        @Schema(description = "보완요청 상세 메시지", example = "최근 3개월 이내 발급된 사업자등록증을 제출해 주세요.")
        @NotBlank
        String message,
        @Schema(description = "DOCUMENT_TYPE 공통코드 목록", example = "[\"BUSINESS_REGISTRATION\"]")
        @NotEmpty
        List<@NotBlank String> documentTypes,
        @Schema(description = "보완 제출 기한")
        LocalDateTime dueAt
) {
}
