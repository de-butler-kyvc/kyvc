package com.kyvc.backendadmin.domain.review.controller;

import com.kyvc.backendadmin.domain.review.application.AdminReviewService;
import com.kyvc.backendadmin.domain.review.dto.AdminReviewActionResponse;
import com.kyvc.backendadmin.domain.review.dto.AdminReviewApproveRequest;
import com.kyvc.backendadmin.domain.review.dto.AdminReviewRejectRequest;
import com.kyvc.backendadmin.domain.review.dto.AdminSupplementRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * KYC 수동심사 승인/반려/보완요청 API를 담당합니다.
 */
@Tag(name = "KYC Review Admin", description = "백엔드 관리자 KYC 수동심사 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/kyc/applications")
public class AdminReviewController {

    private static final String MFA_SESSION_TOKEN_HEADER = "X-MFA-Session-Token";

    private final AdminReviewService adminReviewService;

    /**
     * KYC 수동심사를 승인합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request 승인 요청
     * @return 승인 처리 결과 응답
     */
    @Operation(summary = "KYC 수동심사 승인", description = "MFA_SESSION 토큰이 필요한 KYC 수동심사 승인 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 성공"),
            @ApiResponse(responseCode = "400", description = "상태 전이가 불가능한 경우"),
            @ApiResponse(responseCode = "403", description = "권한 부족"),
            @ApiResponse(responseCode = "404", description = "KYC 신청이 없거나 MFA 토큰이 없는 경우")
    })
    @PostMapping("/{kycId}/manual-review/approve")
    public CommonResponse<AdminReviewActionResponse> approve(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId,
            @RequestHeader(value = MFA_SESSION_TOKEN_HEADER, required = false) String mfaSessionToken,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "KYC 수동심사 승인 요청", required = true)
            @org.springframework.web.bind.annotation.RequestBody(required = false) AdminReviewApproveRequest request
    ) {
        return CommonResponseFactory.success(adminReviewService.approve(kycId, withMfaToken(request, mfaSessionToken)));
    }

    /**
     * KYC 수동심사를 반려합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request 반려 요청
     * @return 반려 처리 결과 응답
     */
    @Operation(summary = "KYC 수동심사 반려", description = "MFA_SESSION 토큰이 필요한 KYC 수동심사 반려 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반려 성공"),
            @ApiResponse(responseCode = "400", description = "상태 전이가 불가능한 경우"),
            @ApiResponse(responseCode = "403", description = "권한 부족"),
            @ApiResponse(responseCode = "404", description = "KYC 신청이 없거나 MFA 토큰 또는 공통코드가 없는 경우")
    })
    @PostMapping("/{kycId}/manual-review/reject")
    public CommonResponse<AdminReviewActionResponse> reject(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "KYC 수동심사 반려 요청", required = true)
            @RequestHeader(value = MFA_SESSION_TOKEN_HEADER, required = false) String mfaSessionToken,
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminReviewRejectRequest request
    ) {
        return CommonResponseFactory.success(adminReviewService.reject(kycId, withMfaToken(request, mfaSessionToken)));
    }

    /**
     * KYC 보완요청을 생성합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request 보완요청 생성 요청
     * @return 보완요청 처리 결과 응답
     */
    @Operation(summary = "KYC 보완요청 생성", description = "SUPPLEMENT_REASON과 DOCUMENT_TYPE 공통코드를 검증한 뒤 KYC 보완요청을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보완요청 성공"),
            @ApiResponse(responseCode = "400", description = "상태 전이가 불가능한 경우"),
            @ApiResponse(responseCode = "403", description = "권한 부족"),
            @ApiResponse(responseCode = "404", description = "KYC 신청 또는 공통코드가 없는 경우")
    })
    @PostMapping("/{kycId}/supplements")
    public CommonResponse<AdminReviewActionResponse> requestSupplement(
            @Parameter(description = "KYC 신청 ID", required = true)
            @PathVariable Long kycId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "KYC 보완요청 생성 요청", required = true)
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminSupplementRequest request
    ) {
        return CommonResponseFactory.success(adminReviewService.requestSupplement(kycId, request));
    }

    private AdminReviewApproveRequest withMfaToken(AdminReviewApproveRequest request, String headerToken) {
        String mfaToken = StringUtils.hasText(headerToken)
                ? headerToken
                : request == null ? null : request.mfaToken();
        return new AdminReviewApproveRequest(mfaToken, request == null ? null : request.comment());
    }

    private AdminReviewRejectRequest withMfaToken(AdminReviewRejectRequest request, String headerToken) {
        String mfaToken = StringUtils.hasText(headerToken) ? headerToken : request.mfaToken();
        return new AdminReviewRejectRequest(mfaToken, request.rejectReasonCode(), request.comment());
    }
}
