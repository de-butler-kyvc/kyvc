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
public class AuthController {

    private final AuthService authService;
    private final TokenCookieUtil tokenCookieUtil;

    /**
     * 법인 사용자 회원가입
     *
     * @param request 회원가입 요청 데이터
     * @return 회원가입 응답
     */
    @PostMapping("/signup/corporate")
    public ResponseEntity<CommonResponse<CorporateSignupResponse>> signupCorporate(
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
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request, // 로그인 요청 데이터
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
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<LogoutResponse>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            HttpServletRequest request, // HTTP 요청 객체
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
    @PostMapping("/token/refresh")
    public ResponseEntity<CommonResponse<TokenRefreshResponse>> refreshToken(
            HttpServletRequest request, // HTTP 요청 객체
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
