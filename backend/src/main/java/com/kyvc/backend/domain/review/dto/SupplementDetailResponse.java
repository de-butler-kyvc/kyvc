package com.kyvc.backend.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 보완요청 상세 응답
 *
 * @param supplementId 보완요청 ID
 * @param kycId KYC 요청 ID
 * @param supplementStatus 보완요청 상태
 * @param supplementReasonCode 보완 사유 코드
 * @param title 보완요청 제목
 * @param message 보완요청 메시지
 * @param requestReason 보완 요청 사유
 * @param requestedDocumentTypeCodes 요청 문서 유형 코드 목록
 * @param uploadedDocuments 업로드된 보완 문서 목록
 * @param requestedAt 요청 일시
 * @param dueAt 제출 기한
 * @param completedAt 완료 일시
 * @param submittedComment 제출 코멘트
 */
@Schema(description = "보완요청 상세 응답")
public record SupplementDetailResponse(
        @Schema(description = "보완요청 ID", example = "1")
        Long supplementId, // 보완요청 ID
        @Schema(description = "KYC 요청 ID", example = "1")
        Long kycId, // KYC 요청 ID
        @Schema(description = "보완요청 상태", example = "REQUESTED")
        String supplementStatus, // 보완요청 상태
        @Schema(description = "보완 사유 코드", example = "MISSING_REQUIRED_DOC")
        String supplementReasonCode, // 보완 사유 코드
        @Schema(description = "보완요청 제목", example = "필수 서류 보완 요청")
        String title, // 보완요청 제목
        @Schema(description = "보완요청 메시지", example = "사업자등록증과 위임장을 다시 제출해 주세요.")
        String message, // 보완요청 메시지
        @Schema(description = "보완 요청 사유", example = "필수 제출 서류 누락")
        String requestReason, // 보완 요청 사유
        @Schema(description = "요청 문서 유형 코드 목록")
        List<String> requestedDocumentTypeCodes, // 요청 문서 유형 코드 목록
        @Schema(description = "업로드된 보완 문서 목록")
        List<SupplementDocumentResponse> uploadedDocuments, // 업로드된 보완 문서 목록
        @Schema(description = "요청 일시", example = "2026-05-05T10:00:00")
        LocalDateTime requestedAt, // 요청 일시
        @Schema(description = "제출 기한", example = "2026-05-10T23:59:59")
        LocalDateTime dueAt, // 제출 기한
        @Schema(description = "완료 일시", example = "2026-05-06T14:00:00")
        LocalDateTime completedAt, // 완료 일시
        @Schema(description = "제출 코멘트", example = "요청하신 서류를 보완 제출했습니다.")
        String submittedComment // 제출 코멘트
) {

    public SupplementDetailResponse {
        requestedDocumentTypeCodes = requestedDocumentTypeCodes == null
                ? List.of()
                : List.copyOf(requestedDocumentTypeCodes);
        uploadedDocuments = uploadedDocuments == null ? List.of() : List.copyOf(uploadedDocuments);
    }
}
