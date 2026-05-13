package com.kyvc.backend.domain.credential.controller;

import com.kyvc.backend.domain.credential.application.CredentialOfferService;
import com.kyvc.backend.domain.credential.application.MobileWalletService;
import com.kyvc.backend.domain.credential.dto.WalletCredentialAcceptRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialAcceptResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialConfirmRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialConfirmResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialDetailResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialListResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialOfferResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialPrepareRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialPrepareResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialStatusRefreshResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mobile Wallet API Controller
 */
@RestController
@RequestMapping("/api/mobile/wallet")
@RequiredArgsConstructor
@Tag(name = "VC / Wallet", description = "모바일 기기, Wallet, Credential Offer 수락, VC 보관 및 조회 API")
public class MobileWalletController {

    private final MobileWalletService mobileWalletService;
    private final CredentialOfferService credentialOfferService;

    /**
     * Credential Offer를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param offerId Offer ID
     * @return Wallet Credential Offer 응답
     */
    @Operation(summary = "모바일 Wallet Credential Offer 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet Credential Offer 반환",
            content = @Content(schema = @Schema(implementation = WalletCredentialOfferResponse.class))
    )
    @GetMapping("/credential-offers/{offerId}")
    public CommonResponse<WalletCredentialOfferResponse> getCredentialOffer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long offerId // Offer ID
    ) {
        return CommonResponseFactory.success(
                credentialOfferService.getMobileOffer(resolveUserId(userDetails), offerId)
        );
    }

    /**
     * Credential Offer 수령을 준비한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param offerId Offer ID
     * @param request Wallet Credential 준비 요청
     * @return Wallet Credential 준비 응답
     */
    @Operation(summary = "모바일 Wallet Credential 준비")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet Credential 준비 응답 반환",
            content = @Content(schema = @Schema(implementation = WalletCredentialPrepareResponse.class))
    )
    @PostMapping("/credential-offers/{offerId}/prepare")
    public CommonResponse<WalletCredentialPrepareResponse> prepareCredentialOffer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long offerId, // Offer ID
            @RequestBody WalletCredentialPrepareRequest request // Wallet Credential 준비 요청
    ) {
        return CommonResponseFactory.success(
                credentialOfferService.prepareWalletCredential(resolveUserId(userDetails), offerId, request)
        );
    }

    /**
     * Credential Offer Wallet 저장을 확정한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param offerId Offer ID
     * @param request Wallet Credential 저장 확정 요청
     * @return Wallet Credential 저장 확정 응답
     */
    @Operation(summary = "모바일 Wallet Credential 저장 확정")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet Credential 저장 확정 응답 반환",
            content = @Content(schema = @Schema(implementation = WalletCredentialConfirmResponse.class))
    )
    @PostMapping("/credential-offers/{offerId}/confirm")
    public CommonResponse<WalletCredentialConfirmResponse> confirmCredentialOffer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long offerId, // Offer ID
            @RequestBody WalletCredentialConfirmRequest request // Wallet Credential 저장 확정 요청
    ) {
        return CommonResponseFactory.success(
                credentialOfferService.confirmWalletCredential(resolveUserId(userDetails), offerId, request)
        );
    }

    /**
     * Credential Offer를 수락한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param offerId Offer ID
     * @param request Offer 수락 요청
     * @return Wallet Credential 수락 응답
     */
    @Operation(summary = "모바일 Wallet Credential Offer 수락")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet Credential 수락 처리 결과 반환",
            content = @Content(schema = @Schema(implementation = WalletCredentialAcceptResponse.class))
    )
    @PostMapping("/credential-offers/{offerId}/accept")
    public CommonResponse<WalletCredentialAcceptResponse> acceptCredentialOffer(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long offerId, // Offer ID
            @RequestBody WalletCredentialAcceptRequest request // Offer 수락 요청
    ) {
        credentialOfferService.rejectDeprecatedAcceptIfCredentialOfferExists(offerId);
        return CommonResponseFactory.success(
                mobileWalletService.acceptCredentialOffer(userDetails, offerId, request)
        );
    }

    /**
     * Wallet 저장 Credential 목록을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @return Wallet Credential 목록 응답
     */
    @Operation(summary = "모바일 Wallet Credential 목록 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet Credential 목록 반환",
            content = @Content(schema = @Schema(implementation = WalletCredentialListResponse.class))
    )
    @GetMapping("/credentials")
    public CommonResponse<WalletCredentialListResponse> getWalletCredentials(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return CommonResponseFactory.success(mobileWalletService.getWalletCredentials(userDetails));
    }

    /**
     * Wallet 저장 Credential 상세를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @return Wallet Credential 상세 응답
     */
    @Operation(summary = "모바일 Wallet Credential 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet Credential 상세 반환",
            content = @Content(schema = @Schema(implementation = WalletCredentialDetailResponse.class))
    )
    @GetMapping("/credentials/{credentialId}")
    public CommonResponse<WalletCredentialDetailResponse> getWalletCredentialDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId // Credential ID
    ) {
        return CommonResponseFactory.success(
                mobileWalletService.getWalletCredentialDetail(userDetails, credentialId)
        );
    }

    /**
     * Wallet 저장 Credential 상태를 갱신한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @return Wallet Credential 상태 갱신 응답
     */
    @Operation(summary = "모바일 Wallet Credential 상태 갱신")
    @ApiResponse(
            responseCode = "200",
            description = "Wallet Credential 상태 갱신 응답 반환",
            content = @Content(schema = @Schema(implementation = WalletCredentialStatusRefreshResponse.class))
    )
    @PostMapping("/credentials/{credentialId}/refresh-status")
    public CommonResponse<WalletCredentialStatusRefreshResponse> refreshCredentialStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId // Credential ID
    ) {
        return CommonResponseFactory.success(
                mobileWalletService.refreshCredentialStatus(userDetails, credentialId)
        );
    }

    private Long resolveUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return userDetails == null ? null : userDetails.getUserId();
    }
}
