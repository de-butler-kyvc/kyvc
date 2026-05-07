package com.kyvc.backend.domain.credential.controller;

import com.kyvc.backend.domain.credential.application.CredentialService;
import com.kyvc.backend.domain.credential.dto.CredentialDetailResponse;
import com.kyvc.backend.domain.credential.dto.CredentialListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialOfferResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credential API Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "Credential", description = "사용자 Credential 조회 API")
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * Credential 목록을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @return Credential 목록 응답
     */
    @Operation(summary = "Credential 목록 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Credential 목록 반환",
            content = @Content(schema = @Schema(implementation = CredentialListResponse.class))
    )
    @GetMapping("/credentials")
    public CommonResponse<CredentialListResponse> getCredentials(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return CommonResponseFactory.success(credentialService.getCredentials(userDetails));
    }

    /**
     * Credential 상세를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @return Credential 상세 응답
     */
    @Operation(summary = "Credential 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Credential 상세 반환",
            content = @Content(schema = @Schema(implementation = CredentialDetailResponse.class))
    )
    @GetMapping("/credentials/{credentialId}")
    public CommonResponse<CredentialDetailResponse> getCredentialDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId // Credential ID
    ) {
        return CommonResponseFactory.success(credentialService.getCredentialDetail(userDetails, credentialId));
    }

    /**
     * KYC 기준 Credential Offer를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return Credential Offer 응답
     */
    @Operation(summary = "Credential Offer 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Credential Offer 반환",
            content = @Content(schema = @Schema(implementation = CredentialOfferResponse.class))
    )
    @GetMapping("/kyc/applications/{kycId}/credential-offer")
    public CommonResponse<CredentialOfferResponse> getCredentialOffer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return CommonResponseFactory.success(credentialService.getCredentialOffer(userDetails, kycId));
    }
}

