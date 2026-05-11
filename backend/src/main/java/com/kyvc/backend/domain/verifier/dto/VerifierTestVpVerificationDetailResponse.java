package com.kyvc.backend.domain.verifier.dto;

import com.kyvc.backend.domain.vp.dto.VpVerificationResultResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifier 테스트 VP 검증 상세 응답
 *
 * @param testId 테스트 검증 ID
 * @param verificationStatus VP 검증 상태
 * @param status VP 검증 상태 호환 필드
 * @param requestedClaims 요청 Claim 목록
 * @param result 검증 결과
 * @param resultSummary 검증 결과 요약
 * @param failureReason 실패 사유
 * @param createdAt 생성 일시
 * @param verifiedAt 검증 일시
 */
@Schema(description = "Verifier 테스트 VP 검증 상세 응답")
public record VerifierTestVpVerificationDetailResponse(
        @Schema(description = "테스트 검증 ID", example = "1")
        Long testId, // 테스트 검증 ID
        @Schema(description = "VP 검증 상태", example = "VALID")
        String verificationStatus, // VP 검증 상태
        @Schema(description = "VP 검증 상태 호환 필드", example = "VALID")
        String status, // VP 검증 상태 호환 필드
        @Schema(description = "요청 Claim 목록")
        List<String> requestedClaims, // 요청 Claim 목록
        @Schema(description = "검증 결과")
        VpVerificationResultResponse result, // 검증 결과
        @Schema(description = "검증 결과 요약", example = "검증 성공")
        String resultSummary, // 검증 결과 요약
        @Schema(description = "실패 사유", example = "INVALID_SIGNATURE")
        String failureReason, // 실패 사유
        @Schema(description = "생성 일시", example = "2026-05-11T10:00:00")
        LocalDateTime createdAt, // 생성 일시
        @Schema(description = "검증 일시", example = "2026-05-11T10:00:00")
        LocalDateTime verifiedAt // 검증 일시
) {
    public VerifierTestVpVerificationDetailResponse(
            Long testId, // 테스트 검증 ID
            String status, // VP 검증 상태
            List<String> requestedClaims, // 요청 Claim 목록
            VpVerificationResultResponse result, // 검증 결과
            String failureReason, // 실패 사유
            LocalDateTime createdAt, // 생성 일시
            LocalDateTime verifiedAt // 검증 일시
    ) {
        this(testId, status, status, requestedClaims, result, failureReason, failureReason, createdAt, verifiedAt);
    }
}
