package com.kyvc.backend.domain.credential.controller;

import com.kyvc.backend.domain.credential.application.CredentialService;
import com.kyvc.backend.domain.credential.dto.CredentialDetailResponse;
import com.kyvc.backend.domain.credential.application.CredentialRequestService;
import com.kyvc.backend.domain.credential.dto.CredentialListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialOfferResponse;
import com.kyvc.backend.domain.credential.dto.CredentialOperationResponse;
import com.kyvc.backend.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backend.domain.credential.dto.CredentialRequestHistoryResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRequestListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credential API Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "VC / Wallet", description = "모바일 기기, Wallet, Credential Offer 수락, VC 보관 및 조회 API")
public class CredentialController {

    private final CredentialService credentialService;
    private final CredentialRequestService credentialRequestService;

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

    /**
     * VC 재발급을 요청합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @param request VC 재발급 요청
     * @return VC 재발급 요청 처리 응답
     */
    @Operation(
            summary = "VC 재발급 요청",
            description = "VC 재발급을 요청합니다. Core 전용 재발급 endpoint가 없는 경우 Core VC 발급 API를 재사용해 신규 Credential을 발급하고, Core 응답 수신 후 요청 상태와 Credential 상태를 저장합니다. Core callback은 사용하지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VC 재발급 요청 처리 결과",
            content = @Content(schema = @Schema(implementation = CredentialOperationResponse.class))
    )
    @PostMapping("/credentials/{credentialId}/reissue-requests")
    public CommonResponse<CredentialOperationResponse> requestCredentialReissue(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId, // Credential ID
            @Valid @RequestBody CredentialReissueRequest request // VC 재발급 요청
    ) {
        return CommonResponseFactory.success(
                credentialRequestService.requestReissue(userDetails, credentialId, request)
        );
    }

    /**
     * VC 폐기를 요청합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @param request VC 폐기 요청
     * @return VC 폐기 요청 처리 응답
     */
    @Operation(
            summary = "VC 폐기 요청",
            description = "VC 폐기를 요청합니다. CoreAdapter를 통해 Core VC 폐기 API를 동기 호출하고, Core 응답 수신 후 Credential 상태와 요청 이력을 저장합니다. Core callback은 사용하지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VC 폐기 요청 처리 결과",
            content = @Content(schema = @Schema(implementation = CredentialOperationResponse.class))
    )
    @PostMapping("/credentials/{credentialId}/revoke-requests")
    public CommonResponse<CredentialOperationResponse> requestCredentialRevoke(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId, // Credential ID
            @Valid @RequestBody CredentialRevokeRequest request // VC 폐기 요청
    ) {
        return CommonResponseFactory.success(
                credentialRequestService.requestRevoke(userDetails, credentialId, request)
        );
    }

    /**
     * Credential 요청 이력을 조회합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @param requestType 요청 유형
     * @param status 요청 상태
     * @return Credential 요청 이력 목록 응답
     */
    @Operation(summary = "Credential 요청 이력 목록 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Credential 요청 이력 목록 반환",
            content = @Content(schema = @Schema(implementation = CredentialRequestListResponse.class))
    )
    @GetMapping("/credential-requests")
    public CommonResponse<CredentialRequestListResponse> getCredentialRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestParam(required = false) KyvcEnums.CredentialRequestType requestType, // 요청 유형
            @RequestParam(required = false) KyvcEnums.CredentialRequestStatus status // 요청 상태
    ) {
        return CommonResponseFactory.success(
                credentialRequestService.getCredentialRequests(userDetails, requestType, status)
        );
    }

    /**
     * Credential 요청 이력 상세를 조회합니다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialRequestId Credential 요청 ID
     * @return Credential 요청 이력 상세 응답
     */
    @Operation(summary = "Credential 요청 이력 상세 조회")
    @ApiResponse(
            responseCode = "200",
            description = "Credential 요청 이력 상세 반환",
            content = @Content(schema = @Schema(implementation = CredentialRequestHistoryResponse.class))
    )
    @GetMapping("/credential-requests/{credentialRequestId}")
    public CommonResponse<CredentialRequestHistoryResponse> getCredentialRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialRequestId // Credential 요청 ID
    ) {
        return CommonResponseFactory.success(
                credentialRequestService.getCredentialRequest(userDetails, credentialRequestId)
        );
    }
}
