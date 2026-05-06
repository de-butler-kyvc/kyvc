package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.MfaService;
import com.kyvc.backend.domain.auth.dto.MfaChallengeRequest;
import com.kyvc.backend.domain.auth.dto.MfaChallengeResponse;
import com.kyvc.backend.domain.auth.dto.MfaVerifyRequest;
import com.kyvc.backend.domain.auth.dto.MfaVerifyResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MFA API Controller
 */
@RestController
@RequestMapping("/api/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "MFA", description = "MFA challenge 생성 및 검증 API")
public class MfaController {

    private final MfaService mfaService;

    /**
     * MFA challenge 생성
     *
     * @param userDetails 인증 사용자 정보
     * @param request MFA challenge 생성 요청
     * @return MFA challenge 생성 응답
     */
    @Operation(
            summary = "MFA challenge 생성",
            description = "로그인 사용자의 이메일 기준 MFA challenge 생성"
    )
    @ApiResponse(
            responseCode = "200",
            description = "MFA challenge 생성 응답 반환",
            content = @Content(schema = @Schema(implementation = MfaChallengeResponse.class))
    )
    @PostMapping("/challenge")
    public ResponseEntity<CommonResponse<MfaChallengeResponse>> createChallenge(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Valid @RequestBody MfaChallengeRequest request // MFA challenge 생성 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                mfaService.createChallenge(
                        getAuthenticatedUserId(userDetails),
                        getAuthenticatedUserEmail(userDetails),
                        request
                )
        ));
    }

    /**
     * MFA challenge 검증
     *
     * @param userDetails 인증 사용자 정보
     * @param request MFA 검증 요청
     * @return MFA 검증 응답
     */
    @Operation(
            summary = "MFA challenge 검증",
            description = "로그인 사용자의 MFA challenge 인증번호 검증"
    )
    @ApiResponse(
            responseCode = "200",
            description = "MFA 검증 응답 반환",
            content = @Content(schema = @Schema(implementation = MfaVerifyResponse.class))
    )
    @PostMapping("/verify")
    public ResponseEntity<CommonResponse<MfaVerifyResponse>> verify(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Valid @RequestBody MfaVerifyRequest request // MFA 검증 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                mfaService.verify(getAuthenticatedUserId(userDetails), request)
        ));
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

    // 인증 사용자 이메일 조회
    private String getAuthenticatedUserEmail(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null || userDetails.getEmail() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getEmail();
    }
}
