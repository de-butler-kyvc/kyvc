package com.kyvc.backend.domain.kyc.controller;

import com.kyvc.backend.domain.kyc.application.KycSubmissionService;
import com.kyvc.backend.domain.kyc.dto.KycApplicationSummaryResponse;
import com.kyvc.backend.domain.kyc.dto.KycSubmitResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * KYC 제출 API Controller
 */
@RestController
@RequestMapping({
        "/api/corporate/kyc/applications/{kycId}",
        "/api/user/kyc/applications/{kycId}"
})
@RequiredArgsConstructor
@Tag(name = "KYC 제출", description = "KYC 제출 전 요약 조회 및 제출 API")
public class KycSubmissionController {

    private final KycSubmissionService kycSubmissionService;

    /**
     * KYC 제출 전 요약 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return KYC 제출 전 요약 응답
     */
    @Operation(
            summary = "KYC 제출 전 요약 조회",
            description = "KYC 제출 전 현재 작성 상태, 필수서류 충족 여부, 제출 가능 여부를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "법인 정보, 업로드 문서, 필수서류 충족 여부, 제출 가능 여부, 누락 항목 목록 반환",
            content = @Content(schema = @Schema(implementation = KycApplicationSummaryResponse.class))
    )
    @GetMapping("/summary")
    public ResponseEntity<CommonResponse<KycApplicationSummaryResponse>> getSummary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycSubmissionService.getSummary(getAuthenticatedUserId(userDetails), kycId)
        ));
    }

    /**
     * KYC 제출
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return KYC 제출 응답
     */
    @Operation(
            summary = "KYC 제출",
            description = "KYC 제출 가능 조건을 모두 만족하면 Core Stub AI 심사 요청을 생성하고 상태를 AI_REVIEWING으로 변경합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "제출된 KYC 신청 ID, KYC 상태, 제출일시, 제출 결과 메시지 반환",
            content = @Content(schema = @Schema(implementation = KycSubmitResponse.class))
    )
    @PostMapping("/submit")
    public ResponseEntity<CommonResponse<KycSubmitResponse>> submit(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                kycSubmissionService.submit(getAuthenticatedUserId(userDetails), kycId)
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
