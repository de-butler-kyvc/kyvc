package com.kyvc.backend.domain.kyc.controller;

import com.kyvc.backend.domain.kyc.application.KycResultService;
import com.kyvc.backend.domain.kyc.dto.KycCompletionResponse;
import com.kyvc.backend.domain.kyc.dto.KycReviewSummaryResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KYC 결과 조회 API Controller
 */
@RestController
@RequestMapping("/api/corporate/kyc/applications/{kycId}")
@RequiredArgsConstructor
@Tag(name = "KYC 결과 조회", description = "KYC 심사 결과 요약, 완료 화면 조회 API")
public class KycResultController {

    private final KycResultService kycResultService;

    /**
     * AI 심사 결과 요약 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 요청 ID
     * @return KYC 심사 결과 요약 응답
     */
    @Operation(
            summary = "AI 심사 결과 요약 조회",
            description = "로그인 사용자의 KYC 요청에 저장된 AI 심사 결과 요약 정보를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 심사 결과 요약 반환",
            content = @Content(schema = @Schema(implementation = KycReviewSummaryResponse.class))
    )
    @GetMapping("/ai-review-summary")
    public ResponseEntity<CommonResponse<KycReviewSummaryResponse>> getAiReviewSummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 요청 ID", example = "1")
            @PathVariable Long kycId // KYC 요청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycResultService.getAiReviewSummary(getAuthenticatedUserId(userDetails), kycId)
        ));
    }

    /**
     * KYC 완료 화면 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 요청 ID
     * @return KYC 완료 화면 응답
     */
    @Operation(
            summary = "KYC 완료 화면 조회",
            description = "로그인 사용자의 승인 완료 KYC 요청에 대한 완료 화면 정보를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 완료 화면 정보 반환",
            content = @Content(schema = @Schema(implementation = KycCompletionResponse.class))
    )
    @GetMapping("/completion")
    public ResponseEntity<CommonResponse<KycCompletionResponse>> getCompletion(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 요청 ID", example = "1")
            @PathVariable Long kycId // KYC 요청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycResultService.getCompletion(getAuthenticatedUserId(userDetails), kycId)
        ));
    }

    // 인증 사용자 ID 조회
    private Long getAuthenticatedUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
