package com.kyvc.backend.domain.finance.controller;

import com.kyvc.backend.domain.finance.application.FinanceKycQrService;
import com.kyvc.backend.domain.finance.dto.FinanceKycIssueQrRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycIssueQrResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycQrStatusResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융사 방문 KYC QR API Controller
 */
@RestController
@RequestMapping("/api/finance/kyc/applications/{kycId}")
@RequiredArgsConstructor
@Tag(name = "Finance Visit KYC QR", description = "금융사 방문 KYC VC 수령 QR API")
public class FinanceKycQrController {

    private final FinanceKycQrService financeKycQrService;

    /**
     * VC 수령 QR 발급
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @param request QR 발급 요청
     * @return QR 발급 응답
     */
    @Operation(
            summary = "금융사 방문 KYC VC 수령 QR 발급",
            description = "승인된 금융사 방문 KYC의 모바일 VC 수령 QR을 발급합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "QR 발급 결과",
            content = @Content(schema = @Schema(implementation = FinanceKycIssueQrResponse.class))
    )
    @PostMapping("/issue-qr")
    public ResponseEntity<CommonResponse<FinanceKycIssueQrResponse>> issueQr(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId, // KYC 신청 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "QR 발급 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FinanceKycIssueQrRequest.class))
            )
            @Valid @RequestBody FinanceKycIssueQrRequest request // QR 발급 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycQrService.issueQr(userDetails, kycId, request)
        ));
    }

    /**
     * VC 수령 QR 상태 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return QR 상태 응답
     */
    @Operation(
            summary = "금융사 방문 KYC VC 수령 QR 상태 조회",
            description = "Credential과 Offer 기존 컬럼 기준으로 QR 상태를 계산해 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "QR 상태 반환",
            content = @Content(schema = @Schema(implementation = FinanceKycQrStatusResponse.class))
    )
    @GetMapping("/qr-status")
    public ResponseEntity<CommonResponse<FinanceKycQrStatusResponse>> getQrStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "KYC 신청 ID", example = "1")
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                financeKycQrService.getQrStatus(userDetails, kycId)
        ));
    }
}
