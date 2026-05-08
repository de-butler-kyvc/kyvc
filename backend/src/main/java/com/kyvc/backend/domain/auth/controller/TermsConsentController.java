package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.TermsConsentService;
import com.kyvc.backend.domain.auth.dto.TermsConsentRequest;
import com.kyvc.backend.domain.auth.dto.TermsConsentResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 약관 동의 API Controller
 */
@RestController
@RequestMapping("/api/common/terms/consents")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class TermsConsentController {

    private final TermsConsentService termsConsentService;

    /**
     * 내 약관 동의 상태 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 내 약관 동의 상태 응답
     */
    @Operation(
            summary = "내 약관 동의 상태 조회",
            description = "로그인 사용자의 약관별 동의 상태와 필수 약관 전체 동의 여부를 조회합니다. 입력값은 없으며 JWT 인증 정보로 사용자를 식별합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 ID, 필수 약관 전체 동의 여부, 약관별 동의 상태 목록 반환",
            content = @Content(schema = @Schema(implementation = TermsConsentResponse.class))
    )
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<TermsConsentResponse>> getMyTermsConsent(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(termsConsentService.getMyTermsConsent(getAuthenticatedUserId(userDetails)))
        );
    }

    /**
     * 약관 동의 저장
     *
     * @param userDetails 인증 사용자 정보
     * @param request 약관 동의 요청 데이터
     * @return 저장된 약관 동의 상태 응답
     */
    @Operation(
            summary = "약관 동의 저장",
            description = "로그인 사용자의 약관 동의 목록을 저장합니다. 입력값은 약관 코드, 약관 버전, 필수 약관 여부, 동의 여부입니다. 출력값은 저장 후 약관 동의 상태입니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "저장 후 사용자 ID, 필수 약관 전체 동의 여부, 약관별 동의 상태 목록 반환",
            content = @Content(schema = @Schema(implementation = TermsConsentResponse.class))
    )
    @PostMapping
    public ResponseEntity<CommonResponse<TermsConsentResponse>> saveMyTermsConsent(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "저장할 약관 동의 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TermsConsentRequest.class))
            )
            @Valid @RequestBody TermsConsentRequest request // 약관 동의 요청 데이터
    ) {
        return ResponseEntity.ok(
                CommonResponseFactory.success(
                        termsConsentService.saveMyTermsConsent(getAuthenticatedUserId(userDetails), request)
                )
        );
    }

    // 인증 사용자 ID 조회
    private Long getAuthenticatedUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        return userDetails.getUserId();
    }
}
