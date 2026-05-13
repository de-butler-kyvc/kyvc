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
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
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
            summary = "로그인 사용자 MFA 이메일 인증번호 발송",
            description = """
                    JWT/Cookie로 인증된 로그인 사용자의 이메일로 MFA 인증번호를 발송합니다.
                    요청 Body에는 email을 받지 않습니다.
                    인증 대상 이메일은 Access Token의 사용자 정보에서 조회합니다.
                    이 API는 회원가입 전 이메일 인증 API가 아니며, 비로그인 사용자는 호출할 수 없습니다.
                    회원가입 화면 이메일 인증은 POST /api/auth/email-verifications/request를 사용합니다.
                    """
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
            summary = "로그인 사용자 MFA 인증번호 검증",
            description = """
                    JWT/Cookie로 인증된 로그인 사용자가 MFA challengeId와 인증번호를 제출해 MFA를 검증합니다.
                    검증 성공 시 중요 작업에서 사용할 MFA session token을 반환합니다.
                    이 API는 회원가입 전 이메일 인증번호 검증 API가 아닙니다.
                    회원가입 화면 이메일 인증번호 검증은 POST /api/auth/email-verifications/verify를 사용합니다.
                    """
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
