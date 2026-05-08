package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.AuthService;
import com.kyvc.backend.domain.auth.dto.CorporateSignupRequest;
import com.kyvc.backend.domain.auth.dto.CorporateSignupResponse;
import com.kyvc.backend.domain.auth.dto.LoginRequest;
import com.kyvc.backend.domain.auth.dto.LoginResponse;
import com.kyvc.backend.domain.auth.dto.LogoutResponse;
import com.kyvc.backend.domain.auth.dto.TokenRefreshResponse;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API Controller
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class AuthController {

    private final AuthService authService;
    private final TokenCookieUtil tokenCookieUtil;

    /**
     * 법인 사용자 회원가입
     *
     * @param request 회원가입 요청 데이터
     * @return 회원가입 응답
     */
    @Operation(
            summary = "법인 사용자 회원가입",
            description = "법인 사용자를 가입 처리합니다. 입력값은 로그인 이메일과 비밀번호입니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "생성된 사용자 ID, 이메일, 사용자 유형, 사용자 상태 반환",
            content = @Content(schema = @Schema(implementation = CorporateSignupResponse.class))
    )
    @PostMapping("/signup/corporate")
    public ResponseEntity<CommonResponse<CorporateSignupResponse>> signupCorporate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "법인 사용자 회원가입 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CorporateSignupRequest.class))
            )
            @Valid @RequestBody CorporateSignupRequest request // 회원가입 요청 데이터
    ) {
        CorporateSignupResponse response = authService.signupCorporate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(response));
    }

    /**
     * 사용자 로그인
     *
     * @param request 로그인 요청 데이터
     * @param response HTTP 응답 객체
     * @return 로그인 응답
     */
    @Operation(
            summary = "사용자 로그인",
            description = "이메일과 비밀번호로 로그인합니다. 입력값은 로그인 이메일과 비밀번호이며, 인증 토큰은 HttpOnly Cookie로 발급합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 ID, 이메일, 사용자 유형, 권한 목록 반환",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
    )
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponse>> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "로그인 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequest.class))
            )
            @Valid @RequestBody LoginRequest request, // 로그인 요청 데이터
            @Parameter(hidden = true)
            HttpServletResponse response // HTTP 응답 객체
    ) {
        AuthService.TokenIssueResult<LoginResponse> result = authService.login(request);
        addAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(CommonResponseFactory.success(result.body()));
    }

    /**
     * 사용자 로그아웃
     *
     * @param userDetails 인증 사용자 정보
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @return 로그아웃 응답
     */
    @Operation(
            summary = "사용자 로그아웃",
            description = "로그인 사용자를 로그아웃 처리하고 인증 Cookie를 삭제합니다. 사용자가 직접 입력하는 요청 본문은 없습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그아웃 완료 여부 반환",
            content = @Content(schema = @Schema(implementation = LogoutResponse.class))
    )
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<LogoutResponse>> logout(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(hidden = true)
            HttpServletRequest request, // HTTP 요청 객체
            @Parameter(hidden = true)
            HttpServletResponse response // HTTP 응답 객체
    ) {
        LogoutResponse logoutResponse = authService.logout(
                userDetails.getUserId(),
                tokenCookieUtil.resolveRefreshToken(request)
        );
        deleteAuthCookies(response);
        return ResponseEntity.ok(CommonResponseFactory.success(logoutResponse));
    }

    /**
     * 토큰 재발급
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @return 토큰 재발급 응답
     */
    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token Cookie로 Access Token과 Refresh Token을 재발급합니다. 사용자가 직접 입력하는 요청 본문은 없습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "토큰 재발급 완료 여부 반환",
            content = @Content(schema = @Schema(implementation = TokenRefreshResponse.class))
    )
    @PostMapping("/token/refresh")
    public ResponseEntity<CommonResponse<TokenRefreshResponse>> refreshToken(
            @Parameter(hidden = true)
            HttpServletRequest request, // HTTP 요청 객체
            @Parameter(hidden = true)
            HttpServletResponse response // HTTP 응답 객체
    ) {
        AuthService.TokenIssueResult<TokenRefreshResponse> result = authService.refresh(
                tokenCookieUtil.resolveRefreshToken(request)
        );
        addAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(CommonResponseFactory.success(result.body()));
    }

    // 인증 Cookie 추가
    private void addAuthCookies(
            HttpServletResponse response, // HTTP 응답 객체
            String accessToken, // Access Token 원문
            String refreshToken // Refresh Token 원문
    ) {
        addCookie(response, tokenCookieUtil.createAccessTokenCookie(accessToken));
        addCookie(response, tokenCookieUtil.createRefreshTokenCookie(refreshToken));
    }

    // 인증 Cookie 삭제
    private void deleteAuthCookies(HttpServletResponse response // HTTP 응답 객체
    ) {
        addCookie(response, tokenCookieUtil.deleteAccessTokenCookie());
        addCookie(response, tokenCookieUtil.deleteRefreshTokenCookie());
    }

    // Set-Cookie 헤더 추가
    private void addCookie(
            HttpServletResponse response, // HTTP 응답 객체
            ResponseCookie cookie // 추가 대상 Cookie
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
