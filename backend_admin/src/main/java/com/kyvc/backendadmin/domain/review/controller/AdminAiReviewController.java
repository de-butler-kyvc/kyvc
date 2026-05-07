package com.kyvc.backendadmin.domain.review.controller;

import com.kyvc.backendadmin.domain.review.application.AdminAiReviewService;
import com.kyvc.backendadmin.domain.review.application.AdminAiReviewRetryService;
import com.kyvc.backendadmin.domain.review.dto.AdminAiReviewDetailResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewAgentAuthorityResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewBeneficialOwnerResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewMismatchResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewRetryRequest;
import com.kyvc.backendadmin.domain.review.dto.AiReviewRetryResponse;
import com.kyvc.backendadmin.domain.review.dto.KycReviewHistoryResponse;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Backend Admin AI 심사 결과 조회 API를 담당합니다.
 */
@Tag(name = "Backend Admin AI Review", description = "백엔드 관리자 AI 심사 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/kyc/applications")
public class AdminAiReviewController {

    private final AdminAiReviewService adminAiReviewService;
    private final AdminAiReviewRetryService adminAiReviewRetryService;

    /**
     * AI 심사 결과 상세 정보를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 심사 결과 상세 정보
     */
    @Operation(
            summary = "AI 심사 결과 상세 조회",
            description = "특정 KYC 신청 건의 AI 심사 상태, 심사 결과, 신뢰도 점수, Core 요청 상태를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 심사 결과 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @GetMapping("/{kycId}/ai-review")
    public CommonResponse<AdminAiReviewDetailResponse> getAiReviewDetail(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminAiReviewService.getAiReviewDetail(kycId));
    }

    /**
     * KYC 신청 건에 대해 AI 재심사를 요청한다.
     *
     * @param kycId KYC 신청 ID
     * @param request AI 재심사 요청 정보
     * @return AI 재심사 요청 생성 결과
     */
    @Operation(
            summary = "AI 재심사 요청",
            description = "관리자가 KYC 신청 건에 대해 AI 재심사를 요청한다. 실제 AI 실행은 Core에서 처리하며, Backend Admin은 core_requests에 요청을 생성한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 재심사 요청 생성 성공"),
            @ApiResponse(responseCode = "400", description = "재심사 요청이 불가능한 KYC 상태"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @PostMapping("/{kycId}/ai-review/retry")
    public CommonResponse<AiReviewRetryResponse> retryAiReview(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "AI 재심사 요청 정보", required = true)
            @Valid @org.springframework.web.bind.annotation.RequestBody AiReviewRetryRequest request
    ) {
        return CommonResponseFactory.success(adminAiReviewRetryService.retry(kycId, request));
    }

    /**
     * AI 문서 불일치 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 문서 불일치 결과
     */
    @Operation(
            summary = "AI 문서 불일치 결과 조회",
            description = "AI 심사 결과 중 사업자등록증, 주주명부, 등기부등본 등 문서 간 불일치 항목을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 문서 불일치 결과 조회 성공"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @GetMapping("/{kycId}/ai-review/mismatches")
    public CommonResponse<AiReviewMismatchResponse> getMismatches(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminAiReviewService.getMismatches(kycId));
    }

    /**
     * AI 실제소유자 판단 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 실제소유자 판단 결과
     */
    @Operation(
            summary = "AI 실제소유자 판단 결과 조회",
            description = "AI가 판단한 주주별 실제소유자 여부, 지분율, 판단 근거, 신뢰도를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 실제소유자 판단 결과 조회 성공"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @GetMapping("/{kycId}/ai-review/beneficial-owners")
    public CommonResponse<AiReviewBeneficialOwnerResponse> getBeneficialOwners(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminAiReviewService.getBeneficialOwners(kycId));
    }

    /**
     * AI 대리인 권한 판단 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 대리인 권한 판단 결과
     */
    @Operation(
            summary = "AI 대리인 권한 판단 결과 조회",
            description = "AI가 판단한 대리인 위임장, 서명, 직인, 권한 범위, 유효 여부를 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 대리인 권한 판단 결과 조회 성공"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @GetMapping("/{kycId}/ai-review/agent-authority")
    public CommonResponse<AiReviewAgentAuthorityResponse> getAgentAuthority(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminAiReviewService.getAgentAuthority(kycId));
    }

    /**
     * KYC 심사 이력을 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return KYC 심사 이력 목록
     */
    @Operation(
            summary = "KYC 심사 이력 조회",
            description = "KYC 신청 건의 AI 심사, 수동 심사, 승인, 반려, 보완요청, VC 발급 등 심사 이력을 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KYC 심사 이력 조회 성공"),
            @ApiResponse(responseCode = "404", description = "KYC 신청을 찾을 수 없음")
    })
    @GetMapping("/{kycId}/review-histories")
    public CommonResponse<List<KycReviewHistoryResponse>> getReviewHistories(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId
    ) {
        return CommonResponseFactory.success(adminAiReviewService.getReviewHistories(kycId));
    }
}
