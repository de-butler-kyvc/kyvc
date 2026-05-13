package com.kyvc.backendadmin.domain.auth.controller;

import com.kyvc.backendadmin.domain.auth.application.AdminAuthService;
import com.kyvc.backendadmin.domain.auth.application.AdminMfaService;
import com.kyvc.backendadmin.domain.auth.application.AdminPasswordService;
import com.kyvc.backendadmin.domain.auth.dto.AdminLoginRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminLoginResponse;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaChallengeRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaChallengeResponse;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaVerifyRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaVerifyResponse;
import com.kyvc.backendadmin.domain.auth.dto.AdminPasswordChangeRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminPasswordResetConfirmRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminPasswordResetRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminSessionResponse;
import com.kyvc.backendadmin.global.jwt.TokenCookieUtil;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Backend Admin 인증 API 컨트롤러
/**
 * Backend Admin 인증 API를 제공하는 컨트롤러입니다.
 *
 * <p>로그인, 로그아웃, 토큰 재발급, 세션 조회, MFA, 비밀번호 재설정,
 * 내 정보 조회와 비밀번호 변경 API를 담당합니다.</p>
 */
@Tag(name = "Admin Auth", description = "Backend Admin 로그인, 토큰, MFA, 비밀번호, 내 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAuthController {

    // refreshToken을 요청 본문으로 받을 때 사용하는 필드명
    private static final String REFRESH_TOKEN_FIELD = "refreshToken";

    // 로그인, 로그아웃, 토큰 재발급, 세션/내 정보 조회 서비스
    private final AdminAuthService adminAuthService;
    // MFA challenge/verify 처리 서비스
    private final AdminMfaService adminMfaService;
    // 비밀번호 재설정/변경 처리 서비스
    private final AdminPasswordService adminPasswordService;
    // Access/Refresh Token 쿠키 생성 및 조회 유틸
    private final TokenCookieUtil tokenCookieUtil;

    // 관리자 로그인
    /**
     * 관리자 로그인 요청을 처리합니다.
     *
     * @param request 관리자 이메일과 비밀번호를 담은 요청 본문
     * @return Access Token, Refresh Token, 관리자 정보와 권한 목록을 담은 공통 응답
     */
    @Operation(summary = "관리자 로그인", description = "관리자 이메일과 비밀번호를 검증하고 Access Token, Refresh Token을 발급합니다.")
    @PostMapping("/auth/login")
    public ResponseEntity<CommonResponse<AdminLoginResponse>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookieUtil.createAccessTokenCookie(response.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, tokenCookieUtil.createRefreshTokenCookie(response.refreshToken()).toString())
                .body(CommonResponseFactory.success(response));
    }

    // 관리자 로그아웃
    /**
     * 관리자 로그아웃 요청을 처리합니다.
     *
     * @param body Refresh Token을 포함할 수 있는 요청 본문
     * @param request Refresh Token 쿠키를 조회하기 위한 HTTP 요청
     * @return 토큰 폐기와 쿠키 삭제 결과를 담은 성공 응답
     */
    @Operation(summary = "관리자 로그아웃", description = "Refresh Token을 폐기하고 인증 쿠키를 삭제합니다.")
    @PostMapping("/auth/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request
    ) {
        adminAuthService.logout(resolveRefreshToken(body, request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookieUtil.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, tokenCookieUtil.deleteRefreshTokenCookie().toString())
                .body(CommonResponseFactory.successWithoutData());
    }

    // Refresh Token 기반 Access Token 재발급
    /**
     * Refresh Token으로 Access Token을 재발급합니다.
     *
     * @param body Refresh Token을 포함할 수 있는 요청 본문
     * @param request Refresh Token 쿠키를 조회하기 위한 HTTP 요청
     * @return 새 Access Token과 관리자 세션 정보를 담은 공통 응답
     */
    @Operation(summary = "Access Token 재발급", description = "Refresh Token의 JWT와 서버 저장 상태를 검증한 뒤 새 Access Token을 발급합니다.")
    @PostMapping({"/auth/refresh", "/auth/token/refresh"})
    public ResponseEntity<CommonResponse<AdminLoginResponse>> refresh(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request
    ) {
        AdminLoginResponse response = adminAuthService.refresh(resolveRefreshToken(body, request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookieUtil.createAccessTokenCookie(response.accessToken()).toString())
                .body(CommonResponseFactory.success(response));
    }

    // 현재 세션 조회, 토큰이 없으면 authenticated=false 응답
    /**
     * 현재 관리자 세션을 조회합니다.
     *
     * @param request Access Token 쿠키 또는 Authorization 헤더를 조회하기 위한 HTTP 요청
     * @return 인증 여부, 관리자 정보, 권한 목록과 읽지 않은 알림 수를 담은 공통 응답
     */
    @Operation(summary = "관리자 세션 조회", description = "Access Token이 없으면 authenticated=false를 반환하고, 토큰이 있으면 현재 관리자 세션 정보를 반환합니다.")
    @GetMapping("/auth/session")
    public CommonResponse<AdminSessionResponse> session(HttpServletRequest request) {
        return CommonResponseFactory.success(adminAuthService.session(resolveAccessToken(request)));
    }

    // 관리자 MFA 이메일 challenge 생성
    /**
     * 관리자 MFA challenge를 생성합니다.
     *
     * @param request MFA 채널과 목적을 담은 요청 본문
     * @return challenge ID, 만료 시각, 마스킹된 대상 정보를 담은 공통 응답
     */
    @Operation(
            summary = "MFA challenge 생성",
            description = "Swagger 테스트 시 channel=EMAIL, purpose=KYC_APPROVE 또는 KYC_REJECT 등 MFA 목적을 입력합니다. 현재 인증된 관리자 이메일로 인증번호를 발송하고, 인증번호 원문은 저장하지 않고 해시만 저장합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "MFA challenge 생성 성공",
            content = @Content(schema = @Schema(implementation = AdminMfaChallengeResponse.class))
    )
    @PostMapping("/auth/mfa/challenge")
    public CommonResponse<AdminMfaChallengeResponse> challenge(
            @Valid @RequestBody AdminMfaChallengeRequest request,
            HttpServletRequest servletRequest
    ) {
        return CommonResponseFactory.success(adminMfaService.challenge(request, resolveAccessToken(servletRequest)));
    }

    // 관리자 MFA 인증번호 검증
    /**
     * 관리자 MFA 인증번호를 검증합니다.
     *
     * @param request challenge ID와 인증번호를 담은 요청 본문
     * @return MFA 세션 토큰과 만료 시각을 담은 공통 응답
     */
    @Operation(
            summary = "MFA 인증번호 검증",
            description = "Swagger 테스트 시 challengeId와 이메일로 받은 verificationCode를 입력합니다. 검증에 성공하면 중요 작업 승인에 사용할 MFA_SESSION 토큰을 발급합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "MFA 인증번호 검증 성공",
            content = @Content(schema = @Schema(implementation = AdminMfaVerifyResponse.class))
    )
    @PostMapping("/auth/mfa/verify")
    public CommonResponse<AdminMfaVerifyResponse> verify(
            @Valid @RequestBody AdminMfaVerifyRequest request,
            HttpServletRequest servletRequest
    ) {
        return CommonResponseFactory.success(adminMfaService.verify(request, resolveAccessToken(servletRequest)));
    }

    // 비밀번호 재설정 요청, 이메일 존재 여부와 무관하게 동일 성공 응답
    /**
     * 관리자 비밀번호 재설정을 요청합니다.
     *
     * @param request 비밀번호 재설정 대상 이메일을 담은 요청 본문
     * @return 이메일 존재 여부와 무관하게 동일한 성공 응답
     */
    @Operation(summary = "비밀번호 재설정 요청", description = "관리자 이메일로 비밀번호 재설정 토큰을 생성합니다. 이메일 존재 여부와 무관하게 동일한 성공 응답을 반환합니다.")
    @PostMapping("/auth/password-reset/request")
    public CommonResponse<Void> requestPasswordReset(@Valid @RequestBody AdminPasswordResetRequest request) {
        adminPasswordService.requestPasswordReset(request);
        return CommonResponseFactory.successWithoutData();
    }

    // 비밀번호 재설정 토큰으로 새 비밀번호 확정
    /**
     * 관리자 비밀번호 재설정을 확정합니다.
     *
     * @param request 비밀번호 재설정 토큰과 새 비밀번호를 담은 요청 본문
     * @return 비밀번호 변경 처리 결과를 담은 성공 응답
     */
    @Operation(summary = "비밀번호 재설정 확정", description = "비밀번호 재설정 토큰을 검증하고 새 비밀번호를 BCrypt로 저장합니다.")
    @PostMapping("/auth/password-reset/confirm")
    public CommonResponse<Void> confirmPasswordReset(@Valid @RequestBody AdminPasswordResetConfirmRequest request) {
        adminPasswordService.confirmPasswordReset(request);
        return CommonResponseFactory.successWithoutData();
    }

    // 현재 로그인한 관리자 정보 조회
    /**
     * 현재 로그인한 관리자 정보를 조회합니다.
     *
     * @return 현재 관리자 정보, 권한 목록과 읽지 않은 알림 수를 담은 공통 응답
     */
    @Operation(summary = "내 정보 조회", description = "현재 인증된 관리자 정보와 권한 목록을 조회합니다.")
    @GetMapping({"/auth/me", "/me"})
    public CommonResponse<AdminSessionResponse> me() {
        return CommonResponseFactory.success(adminAuthService.me());
    }

    // 현재 로그인한 관리자 비밀번호 변경
    /**
     * 현재 로그인한 관리자의 비밀번호를 변경합니다.
     *
     * @param request 현재 비밀번호와 새 비밀번호를 담은 요청 본문
     * @return 비밀번호 변경 처리 결과를 담은 성공 응답
     */
    @Operation(summary = "내 비밀번호 변경", description = "현재 비밀번호를 검증한 뒤 새 비밀번호를 BCrypt로 저장하고 기존 Refresh Token을 폐기합니다.")
    @PatchMapping({"/auth/change-password", "/me/password"})
    public CommonResponse<Void> changePassword(@Valid @RequestBody AdminPasswordChangeRequest request) {
        adminPasswordService.changePassword(request);
        return CommonResponseFactory.successWithoutData();
    }

    // 쿠키 또는 Authorization 헤더에서 Access Token 조회
    private String resolveAccessToken(HttpServletRequest request) {
        String cookieToken = tokenCookieUtil.resolveAccessToken(request);
        if (StringUtils.hasText(cookieToken)) {
            return cookieToken;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    // 요청 본문 또는 쿠키에서 Refresh Token 조회
    private String resolveRefreshToken(Map<String, String> body, HttpServletRequest request) {
        if (body != null && StringUtils.hasText(body.get(REFRESH_TOKEN_FIELD))) {
            return body.get(REFRESH_TOKEN_FIELD);
        }
        return tokenCookieUtil.resolveRefreshToken(request);
    }
}
