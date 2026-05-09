package com.kyvc.backend.domain.vp.controller;

import com.kyvc.backend.domain.vp.application.VpVerificationService;
import com.kyvc.backend.domain.vp.dto.EligibleCredentialListResponse;
import com.kyvc.backend.domain.vp.dto.QrResolveRequest;
import com.kyvc.backend.domain.vp.dto.QrResolveResponse;
import com.kyvc.backend.domain.vp.dto.VpPresentationRequest;
import com.kyvc.backend.domain.vp.dto.VpPresentationResponse;
import com.kyvc.backend.domain.vp.dto.VpPresentationResultResponse;
import com.kyvc.backend.domain.vp.dto.VpRequestResponse;
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
 * 모바일 VP API Controller
 */
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@Tag(name = "VP / QR 검증", description = "QR 해석, VP 요청 조회, 제출 가능 VC 조회, VP 제출 및 검증 결과 API")
public class MobileVpController {

    private final VpVerificationService vpVerificationService;

    /**
     * QR Payload를 해석한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param request QR 해석 요청
     * @return QR 해석 응답
     */
    @Operation(summary = "모바일 QR 해석")
    @ApiResponse(
            responseCode = "200",
            description = "QR 해석 응답 반환",
            content = @Content(schema = @Schema(implementation = QrResolveResponse.class))
    )
    @PostMapping("/qr/resolve")
    public CommonResponse<QrResolveResponse> resolveQr(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestBody QrResolveRequest request // QR 해석 요청
    ) {
        return CommonResponseFactory.success(vpVerificationService.resolveQr(userDetails, request));
    }

    /**
     * VP 요청을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param requestId VP 요청 ID
     * @return VP 요청 응답
     */
    @Operation(
            summary = "모바일 VP 요청 조회",
            description = "모바일 앱이 VP 요청 정보를 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 요청 정보만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 요청 응답 반환",
            content = @Content(schema = @Schema(implementation = VpRequestResponse.class))
    )
    @GetMapping("/vp/requests/{requestId}")
    public CommonResponse<VpRequestResponse> getVpRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable String requestId // VP 요청 ID
    ) {
        return CommonResponseFactory.success(vpVerificationService.getVpRequest(userDetails, requestId));
    }

    /**
     * 제출 가능 Credential 목록을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param requestId VP 요청 ID
     * @return 제출 가능 Credential 목록 응답
     */
    @Operation(summary = "모바일 제출 가능 Credential 조회")
    @ApiResponse(
            responseCode = "200",
            description = "제출 가능 Credential 목록 응답 반환",
            content = @Content(schema = @Schema(implementation = EligibleCredentialListResponse.class))
    )
    @GetMapping("/vp/requests/{requestId}/eligible-credentials")
    public CommonResponse<EligibleCredentialListResponse> getEligibleCredentials(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable String requestId // VP 요청 ID
    ) {
        return CommonResponseFactory.success(vpVerificationService.getEligibleCredentials(userDetails, requestId));
    }

    /**
     * VP를 제출한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param request VP 제출 요청
     * @return VP 제출 응답
     */
    @Operation(
            summary = "모바일 VP 제출",
            description = "모바일 앱이 VP를 제출하면 backend가 Core에 VP 검증을 동기 요청합니다. Core 응답 수신 후 VP 검증 결과를 저장하고 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 검증 결과 반환",
            content = @Content(schema = @Schema(implementation = VpPresentationResponse.class))
    )
    @PostMapping("/vp/presentations")
    public CommonResponse<VpPresentationResponse> submitPresentation(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @RequestBody VpPresentationRequest request // VP 제출 요청
    ) {
        return CommonResponseFactory.success(vpVerificationService.submitPresentation(userDetails, request));
    }

    /**
     * VP 제출 결과를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param presentationId VP 제출 ID
     * @return VP 제출 결과 응답
     */
    @Operation(
            summary = "모바일 VP 제출 이력 상세 조회",
            description = "모바일 VP 제출 상세 결과를 조회합니다. Core를 직접 호출하지 않고 저장된 VP 검증 결과만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 제출 결과 응답 반환",
            content = @Content(schema = @Schema(implementation = VpPresentationResultResponse.class))
    )
    @GetMapping("/vp/presentations/{presentationId}")
    public CommonResponse<VpPresentationResultResponse> getPresentationResult(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long presentationId // VP 제출 ID
    ) {
        return CommonResponseFactory.success(
                vpVerificationService.getPresentationResult(userDetails, presentationId)
        );
    }
}
