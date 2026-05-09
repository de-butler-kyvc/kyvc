package com.kyvc.backend.domain.credential.controller;

import com.kyvc.backend.domain.credential.application.CredentialService;
import com.kyvc.backend.domain.credential.application.CredentialRequestService;
import com.kyvc.backend.domain.credential.dto.CredentialDetailResponse;
import com.kyvc.backend.domain.credential.dto.CredentialIssueResponse;
import com.kyvc.backend.domain.credential.dto.CredentialListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialOfferResponse;
import com.kyvc.backend.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backend.domain.credential.dto.CredentialRequestDetailResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRequestListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRequestResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRevokeRequest;
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
     * @param status Credential 상태 필터
     * @return Credential 목록 응답
     */
    @Operation(
            summary = "Credential 목록 조회",
            description = "로그인 사용자의 Credential 목록을 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 Credential 상태만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Credential 목록 반환",
            content = @Content(schema = @Schema(implementation = CredentialListResponse.class))
    )
    @GetMapping("/credentials")
    public CommonResponse<CredentialListResponse> getCredentials(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestParam(required = false) String status // Credential 상태 필터
    ) {
        return CommonResponseFactory.success(credentialService.getCredentials(userDetails, status));
    }

    /**
     * KYC 승인 건에 대해 VC 발급을 요청한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param kycId KYC 신청 ID
     * @return Credential 발급 응답
     */
    @Operation(
            summary = "사용자 VC 발급 요청",
            description = "KYC 승인 건에 대해 VC 발급을 요청합니다. CoreAdapter를 통해 Core VC 발급 API를 동기 호출하고, Core 응답 수신 후 Credential 상태를 저장합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Credential 발급 결과 반환",
            content = @Content(schema = @Schema(implementation = CredentialIssueResponse.class))
    )
    @PostMapping("/kyc/applications/{kycId}/credentials")
    public CommonResponse<CredentialIssueResponse> issueCredential(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long kycId // KYC 신청 ID
    ) {
        return CommonResponseFactory.success(credentialService.issueCredential(userDetails, kycId));
    }

    /**
     * Credential 재발급을 요청한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @param request 재발급 요청
     * @return Credential 요청 처리 응답
     */
    @Operation(
            summary = "사용자 VC 재발급 요청",
            description = "VC 재발급을 요청합니다. Core 재발급 endpoint가 확인된 경우 동기 호출하고, Core 응답 수신 후 요청 상태와 Credential 상태를 저장합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Credential 재발급 요청 결과 반환",
            content = @Content(schema = @Schema(implementation = CredentialRequestResponse.class))
    )
    @PostMapping("/credentials/{credentialId}/reissue-requests")
    public CommonResponse<CredentialRequestResponse> requestReissue(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId, // Credential ID
            @Valid @RequestBody(required = false) CredentialReissueRequest request // 재발급 요청
    ) {
        return CommonResponseFactory.success(credentialRequestService.requestReissue(userDetails, credentialId, request));
    }

    /**
     * Credential 폐기를 요청한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @param request 폐기 요청
     * @return Credential 요청 처리 응답
     */
    @Operation(
            summary = "사용자 VC 폐기 요청",
            description = "VC 폐기를 요청합니다. CoreAdapter를 통해 Core VC 폐기 API를 동기 호출하고, Core 응답 수신 후 Credential 상태와 요청 이력을 저장합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Credential 폐기 요청 결과 반환",
            content = @Content(schema = @Schema(implementation = CredentialRequestResponse.class))
    )
    @PostMapping("/credentials/{credentialId}/revoke-requests")
    public CommonResponse<CredentialRequestResponse> requestRevoke(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialId, // Credential ID
            @Valid @RequestBody(required = false) CredentialRevokeRequest request // 폐기 요청
    ) {
        return CommonResponseFactory.success(credentialRequestService.requestRevoke(userDetails, credentialId, request));
    }

    /**
     * Credential 요청 이력 목록을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param type 요청 유형 필터
     * @param status 요청 상태 필터
     * @return Credential 요청 이력 목록 응답
     */
    @Operation(
            summary = "Credential 요청 이력 목록 조회",
            description = "로그인 사용자의 Credential 요청 이력 목록을 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 요청 상태만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Credential 요청 이력 목록 반환",
            content = @Content(schema = @Schema(implementation = CredentialRequestListResponse.class))
    )
    @GetMapping("/credential-requests")
    public CommonResponse<CredentialRequestListResponse> getCredentialRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestParam(required = false) String type, // 요청 유형 필터
            @RequestParam(required = false) String status // 요청 상태 필터
    ) {
        return CommonResponseFactory.success(credentialRequestService.getCredentialRequests(userDetails, type, status));
    }

    /**
     * Credential 요청 이력 상세를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialRequestId Credential 요청 ID
     * @return Credential 요청 이력 상세 응답
     */
    @Operation(
            summary = "Credential 요청 이력 상세 조회",
            description = "Credential 요청 이력 상세를 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 요청 결과만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Credential 요청 이력 상세 반환",
            content = @Content(schema = @Schema(implementation = CredentialRequestDetailResponse.class))
    )
    @GetMapping("/credential-requests/{credentialRequestId}")
    public CommonResponse<CredentialRequestDetailResponse> getCredentialRequestDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long credentialRequestId // Credential 요청 ID
    ) {
        return CommonResponseFactory.success(credentialRequestService.getCredentialRequestDetail(userDetails, credentialRequestId));
    }

    /**
     * Credential 상세를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param credentialId Credential ID
     * @return Credential 상세 응답
     */
    @Operation(
            summary = "Credential 상세 조회",
            description = "Credential 상세 메타데이터를 조회합니다. VC 원문과 Core raw payload는 응답하지 않습니다."
    )
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
