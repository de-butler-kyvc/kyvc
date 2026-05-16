package com.kyvc.backend.domain.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * KYC AI 심사 결과 상세 응답
 *
 * @param kycId KYC 신청 ID
 * @param applicationStatusCode KYC 신청 상태 코드
 * @param aiReviewStatusCode AI 심사 상태 코드
 * @param overallResultCode AI 종합 결과 코드
 * @param confidenceScore AI 신뢰도 점수
 * @param reviewedAt 심사 반영 일시
 * @param manualReviewRequired 수기 심사 필요 여부
 * @param supplementRequired 보완 필요 여부
 * @param summary AI 심사 요약
 * @param documentResults 문서별 심사 결과 목록
 * @param mismatchResults 문서 간 불일치 결과 목록
 * @param beneficialOwnerResults 실소유자 심사 결과 목록
 * @param delegationResult 위임권한 심사 결과
 * @param reviewReasons 심사 사유 목록
 */
@Schema(description = "KYC AI 심사 결과 상세 응답")
public record KycAiReviewDetailResponse(
        @Schema(description = "KYC 신청 ID", example = "10")
        Long kycId, // KYC 신청 ID
        @Schema(description = "KYC 신청 상태 코드", example = "MANUAL_REVIEW")
        String applicationStatusCode, // KYC 신청 상태 코드
        @Schema(description = "AI 심사 상태 코드", example = "SUCCESS")
        String aiReviewStatusCode, // AI 심사 상태 코드
        @Schema(description = "AI 종합 결과 코드", example = "NEED_MANUAL_REVIEW", nullable = true)
        String overallResultCode, // AI 종합 결과 코드
        @Schema(description = "AI 신뢰도 점수", example = "0.82", nullable = true)
        BigDecimal confidenceScore, // AI 신뢰도 점수
        @Schema(description = "심사 반영 일시", example = "2026-05-16T21:30:00", nullable = true)
        LocalDateTime reviewedAt, // 심사 반영 일시
        @Schema(description = "수기 심사 필요 여부", example = "true")
        boolean manualReviewRequired, // 수기 심사 필요 여부
        @Schema(description = "보완 필요 여부", example = "false")
        boolean supplementRequired, // 보완 필요 여부
        @Schema(description = "AI 심사 요약", example = "일부 문서 간 대표자 정보 확인 필요", nullable = true)
        String summary, // AI 심사 요약
        @Schema(description = "문서별 심사 결과 목록")
        List<DocumentResult> documentResults, // 문서별 심사 결과 목록
        @Schema(description = "문서 간 불일치 결과 목록")
        List<MismatchResult> mismatchResults, // 문서 간 불일치 결과 목록
        @Schema(description = "실소유자 심사 결과 목록")
        List<BeneficialOwnerResult> beneficialOwnerResults, // 실소유자 심사 결과 목록
        @Schema(description = "위임권한 심사 결과", nullable = true)
        DelegationResult delegationResult, // 위임권한 심사 결과
        @Schema(description = "심사 사유 목록")
        List<String> reviewReasons // 심사 사유 목록
) {
    public KycAiReviewDetailResponse {
        documentResults = documentResults == null ? List.of() : List.copyOf(documentResults);
        mismatchResults = mismatchResults == null ? List.of() : List.copyOf(mismatchResults);
        beneficialOwnerResults = beneficialOwnerResults == null ? List.of() : List.copyOf(beneficialOwnerResults);
        reviewReasons = reviewReasons == null ? List.of() : List.copyOf(reviewReasons);
    }

    /**
     * 문서별 심사 결과
     *
     * @param documentId 문서 ID
     * @param documentTypeCode 문서 유형 코드
     * @param documentTypeName 문서 유형명
     * @param resultCode 심사 결과 코드
     * @param confidenceScore 신뢰도 점수
     * @param message 심사 메시지
     */
    public record DocumentResult(
            Long documentId, // 문서 ID
            String documentTypeCode, // 문서 유형 코드
            String documentTypeName, // 문서 유형명
            String resultCode, // 심사 결과 코드
            BigDecimal confidenceScore, // 신뢰도 점수
            String message // 심사 메시지
    ) {
    }

    /**
     * 문서 간 불일치 결과
     *
     * @param fieldName 필드명
     * @param sourceDocumentTypeCode 기준 문서 유형 코드
     * @param targetDocumentTypeCode 대상 문서 유형 코드
     * @param severityCode 심각도 코드
     * @param message 심사 메시지
     */
    public record MismatchResult(
            String fieldName, // 필드명
            String sourceDocumentTypeCode, // 기준 문서 유형 코드
            String targetDocumentTypeCode, // 대상 문서 유형 코드
            String severityCode, // 심각도 코드
            String message // 심사 메시지
    ) {
    }

    /**
     * 실소유자 심사 결과
     *
     * @param ownerName 실소유자명
     * @param ownershipRatio 지분율
     * @param resultCode 심사 결과 코드
     * @param message 심사 메시지
     */
    public record BeneficialOwnerResult(
            String ownerName, // 실소유자명
            BigDecimal ownershipRatio, // 지분율
            String resultCode, // 심사 결과 코드
            String message // 심사 메시지
    ) {
    }

    /**
     * 위임권한 심사 결과
     *
     * @param resultCode 심사 결과 코드
     * @param message 심사 메시지
     */
    public record DelegationResult(
            String resultCode, // 심사 결과 코드
            String message // 심사 메시지
    ) {
    }
}
