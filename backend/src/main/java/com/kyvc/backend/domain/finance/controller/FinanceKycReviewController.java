package com.kyvc.backend.domain.finance.controller;

import com.kyvc.backend.domain.finance.application.FinanceKycReviewService;
import com.kyvc.backend.domain.finance.dto.FinanceKycResultResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycSubmitResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융사 방문 KYC 심사 API Controller
 */
@RestController
@RequestMapping("/api/finance/kyc/applications/{kycId}")
@RequiredArgsConstructor
@Tag(name = "Finance Visit KYC Review", description = "금융사 방문 KYC AI 심사 API")
public class FinanceKycReviewController {

    private final FinanceKycReviewService financeKycReviewService;

    /**
     * 금융사 방문 KYC 제출
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return KYC 제출 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 제출",
            description = "금융사 방문 KYC를 제출하고 Core AI 심사를 동기 호출합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 제출 및 AI 심사 결과",
            content = @Content(schema = @Schema(implementation = FinanceKycSubmitResponse.class))
    )
    @PostMapping("/submit")
    public ResponseEntity<CommonResponse<FinanceKycSubmitResponse>> submit(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycReviewService.submit(userDetails, kycId)
        ));
    }

    /**
     * 금융사 방문 KYC 심사 결과 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return 심사 결과 응답
     */
    @Operation(
            summary = "금융사 방문 KYC 심사 결과 조회",
            description = "Core를 재호출하지 않고 backend DB에 반영된 금융사 방문 KYC 심사 결과를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "KYC 심사 결과 반환",
            content = @Content(schema = @Schema(implementation = FinanceKycResultResponse.class))
    )
    @GetMapping("/result")
    public ResponseEntity<CommonResponse<FinanceKycResultResponse>> getResult(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycReviewService.getResult(userDetails, kycId)
        ));
    }
}
