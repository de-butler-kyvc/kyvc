package com.kyvc.backend.domain.kyc.controller;

import com.kyvc.backend.domain.kyc.application.KycAiReviewQueryService;
import com.kyvc.backend.domain.kyc.dto.KycAiReviewDetailResponse;
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
 * KYC AI 심사 결과 API Controller
 */
@RestController
@RequestMapping("/api/user/kyc/applications/{kycId}")
@RequiredArgsConstructor
@Tag(name = "KYC 신청 / 서류", description = "KYC 신청, 필수서류, 문서 업로드, 제출, 완료 및 Credential Offer API")
public class KycAiReviewController {

    private final KycAiReviewQueryService kycAiReviewQueryService;

    /**
     * AI 심사 결과 상세 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return AI 심사 결과 상세 응답
     */
    @Operation(
            summary = "AI 심사 결과 상세 조회",
            description = "로그인 사용자의 KYC 신청에 저장된 AI 심사 결과 상세 정보를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "AI 심사 결과 상세 응답",
            content = @Content(schema = @Schema(implementation = KycAiReviewDetailResponse.class))
    )
    @GetMapping("/ai-review-result")
    public ResponseEntity<CommonResponse<KycAiReviewDetailResponse>> getAiReviewResult(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "10")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycAiReviewQueryService.getDetail(getAuthenticatedUserId(userDetails), kycId)
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
