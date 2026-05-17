package com.kyvc.backend.domain.verifier.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 금융사 VP 요청 상세 응답
 *
 * @param requestId VP 요청 ID
 * @param status VP 검증 상태
 * @param verificationStatus 검증 상태 코드
 * @param purpose VP 요청 목적
 * @param requestedClaims 요청 Claim 목록
 * @param qrPayload QR Payload
 * @param corporateId 법인 ID
 * @param corporateName 법인명
 * @param result 검증 결과 요약
 * @param expiresAt 만료 일시
 * @param createdAt 생성 일시
 * @param verifiedAt 검증 일시
 * @param submittedAt 제출 일시
 * @param checks 검증 항목 목록
 */
@Schema(description = "금융사 VP 요청 상세 응답")
public record FinanceVpRequestDetailResponse(
        @Schema(description = "VP 요청 ID", example = "vp-req-001")
        String requestId, // VP 요청 ID
        @Schema(description = "VP 검증 상태", example = "REQUESTED")
        String status, // VP 검증 상태
        @Schema(description = "검증 상태 코드", example = "VALID")
        String verificationStatus, // 검증 상태 코드
        @Schema(description = "VP 요청 목적", example = "ACCOUNT_OPENING")
        String purpose, // VP 요청 목적
        @Schema(description = "요청 Claim 목록")
        List<String> requestedClaims, // 요청 Claim 목록
        @Schema(description = "QR Payload")
        String qrPayload, // QR Payload
        @Schema(description = "법인 ID", example = "10")
        Long corporateId, // 법인 ID
        @Schema(description = "법인명", example = "테스트 법인")
        String corporateName, // 법인명
        @Schema(description = "검증 결과 요약")
        FinanceVpRequestResultResponse result, // 검증 결과 요약
        @Schema(description = "만료 일시", example = "2026-05-10T10:10:00")
        LocalDateTime expiresAt, // 만료 일시
        @Schema(description = "생성 일시", example = "2026-05-10T10:00:00")
        LocalDateTime createdAt, // 생성 일시
        @Schema(description = "검증 일시", example = "2026-05-10T10:05:00")
        LocalDateTime verifiedAt, // 검증 일시
        @Schema(description = "제출 일시", example = "2026-05-10T10:03:00")
        LocalDateTime submittedAt, // 제출 일시
        @Schema(description = "검증 항목 목록")
        List<FinanceVpVerificationCheckResponse> checks // 검증 항목 목록
) {
    public FinanceVpRequestDetailResponse {
        checks = checks == null ? List.of() : checks;
    }

    public FinanceVpRequestDetailResponse(
            String requestId, // VP 요청 ID
            String status, // VP 검증 상태
            String verificationStatus, // 검증 상태 코드
            String purpose, // VP 요청 목적
            List<String> requestedClaims, // 요청 Claim 목록
            String qrPayload, // QR Payload
            Long corporateId, // 법인 ID
            String corporateName, // 법인명
            FinanceVpRequestResultResponse result, // 검증 결과 요약
            LocalDateTime expiresAt, // 만료 일시
            LocalDateTime createdAt, // 생성 일시
            LocalDateTime verifiedAt // 검증 일시
    ) {
        this(requestId, status, verificationStatus, purpose, requestedClaims, qrPayload, corporateId, corporateName, result, expiresAt, createdAt, verifiedAt, null, List.of());
    }

    public FinanceVpRequestDetailResponse(
            String requestId, // VP 요청 ID
            String status, // VP 검증 상태
            String purpose, // VP 요청 목적
            List<String> requestedClaims, // 요청 Claim 목록
            String qrPayload, // QR Payload
            Long corporateId, // 법인 ID
            String corporateName, // 법인명
            FinanceVpRequestResultResponse result, // 검증 결과 요약
            LocalDateTime expiresAt, // 만료 일시
            LocalDateTime createdAt, // 생성 일시
            LocalDateTime verifiedAt // 검증 일시
    ) {
        this(requestId, status, status, purpose, requestedClaims, qrPayload, corporateId, corporateName, result, expiresAt, createdAt, verifiedAt);
    }
}
