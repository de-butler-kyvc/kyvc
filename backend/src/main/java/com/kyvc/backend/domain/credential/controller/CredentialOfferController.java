package com.kyvc.backend.domain.credential.controller;

import com.kyvc.backend.domain.credential.application.CredentialOfferService;
import com.kyvc.backend.domain.credential.dto.CredentialOfferCreateResponse;
import com.kyvc.backend.domain.credential.dto.CredentialOfferStatusResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credential Offer API Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "VC / Wallet", description = "Credential Offer 생성과 상태 조회 API")
public class CredentialOfferController {

    private final CredentialOfferService credentialOfferService;

    /**
     * KYC 승인 건에 대한 Credential Offer QR을 생성한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return Credential Offer 생성 응답
     */
    @Operation(summary = "Credential Offer 생성")
    @ApiResponse(
            responseCode = "200",
            description = "Credential Offer 생성 응답 반환",
            content = @Content(schema = @Schema(implementation = CredentialOfferCreateResponse.class))
    )
    @PostMapping("/kyc/applications/{kycId}/credential-offers")
    public CommonResponse<CredentialOfferCreateResponse> createOffer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return CommonResponseFactory.success(
                credentialOfferService.createOffer(resolveUserId(userDetails), kycId)
        );
    }

    /**
     * Credential Offer 상태를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param offerId Offer ID
     * @return Credential Offer 상태 응답
     */
    @Operation(summary = "Credential Offer 상태 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Credential Offer 상태 응답 반환",
            content = @Content(schema = @Schema(implementation = CredentialOfferStatusResponse.class))
    )
    @GetMapping("/credential-offers/{offerId}/status")
    public CommonResponse<CredentialOfferStatusResponse> getOfferStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long offerId // Offer ID
    ) {
        return CommonResponseFactory.success(
                credentialOfferService.getOfferStatus(resolveUserId(userDetails), offerId)
        );
    }

    private Long resolveUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return userDetails == null ? null : userDetails.getUserId();
    }
}
