package com.kyvc.backend.domain.verifier.controller;

import com.kyvc.backend.domain.verifier.application.VerifierVpService;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationDetailResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationResponse;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifier API Controller
 */
@RestController
@RequestMapping("/api/verifier")
@RequiredArgsConstructor
@Tag(name = "Verifier", description = "Verifier 테스트 검증과 기업 재인증 요청 API")
public class VerifierController {

    private final VerifierVpService verifierVpService;

    /**
     * Verifier 테스트 VP 검증을 실행
     *
     * @param userDetails 인증 사용자 정보
     * @param request 테스트 VP 검증 요청
     * @return 테스트 VP 검증 응답
     */
    @Operation(
            summary = "Verifier 테스트 VP 검증 실행",
            description = "Verifier 테스트 VP 검증을 실행합니다. CoreAdapter를 통해 Core VP 검증 API를 동기 호출하고, Core 응답 수신 후 검증 결과를 저장하고 반환합니다. Core callback은 사용하지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Verifier 테스트 VP 검증 응답",
            content = @Content(schema = @Schema(implementation = VerifierTestVpVerificationResponse.class))
    )
    @PostMapping("/test-vp-verifications")
    public CommonResponse<VerifierTestVpVerificationResponse> testVpVerification(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Valid @RequestBody VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
    ) {
        return CommonResponseFactory.success(verifierVpService.testVpVerification(userDetails, request));
    }

    /**
     * Verifier 테스트 VP 검증 이력 상세를 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param testId 테스트 검증 ID
     * @return 테스트 VP 검증 상세 응답
     */
    @Operation(
            summary = "Verifier 테스트 VP 검증 이력 상세 조회",
            description = "저장된 테스트 VP 검증 이력 상세를 조회합니다. 검증 실행은 POST API에서 동기 처리되며, 본 API는 Core를 직접 호출하지 않습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Verifier 테스트 VP 검증 상세 응답",
            content = @Content(schema = @Schema(implementation = VerifierTestVpVerificationDetailResponse.class))
    )
    @GetMapping("/test-vp-verifications/{testId}")
    public CommonResponse<VerifierTestVpVerificationDetailResponse> getTestVpVerification(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long testId // 테스트 검증 ID
    ) {
        return CommonResponseFactory.success(verifierVpService.getTestVpVerification(userDetails, testId));
    }

    /**
     * Verifier 기업 재인증 요청을 생성
     *
     * @param userDetails 인증 사용자 정보
     * @param request 재인증 요청 생성 요청
     * @return 재인증 요청 생성 응답
     */
    @Operation(
            summary = "Verifier 기업 재인증 요청 생성",
            description = "Verifier 기업 재인증 요청을 생성합니다. Core를 호출하지 않으며, 실제 VP 검증은 모바일 VP 제출 API에서 처리됩니다. resultNotifyUrl은 외부 Verifier 결과 통지 URL이며 Core 내부 수신 URL 용도가 아닙니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Verifier 기업 재인증 요청 생성 응답",
            content = @Content(schema = @Schema(implementation = VerifierReAuthRequestCreateResponse.class))
    )
    @PostMapping("/re-auth-requests")
    public CommonResponse<VerifierReAuthRequestCreateResponse> createReAuthRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Valid @RequestBody VerifierReAuthRequestCreateRequest request // 재인증 요청 생성 요청
    ) {
        return CommonResponseFactory.success(verifierVpService.createReAuthRequest(userDetails, request));
    }
}
