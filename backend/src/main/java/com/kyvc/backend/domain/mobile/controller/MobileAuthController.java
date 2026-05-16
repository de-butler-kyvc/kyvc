package com.kyvc.backend.domain.mobile.controller;

import com.kyvc.backend.domain.mobile.application.MobileAuthService;
import com.kyvc.backend.domain.mobile.dto.MobileAutoLoginResponse;
import com.kyvc.backend.domain.mobile.dto.MobileLoginRequest;
import com.kyvc.backend.domain.mobile.dto.MobileLoginResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모바일 인증 API Controller
 */
@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    /**
     * 모바일 로그인
     *
     * @param request 모바일 로그인 요청
     * @param response HTTP 응답 객체
     * @return 모바일 로그인 응답
     */
    @Operation(
            summary = "모바일 이메일/비밀번호 로그인",
            description = """
                    비로그인 모바일 앱이 email, password, deviceId, os 등 사용자 입력값으로 로그인합니다.
                    JWT/Cookie 인증이 필요하지 않습니다.
                    로그인 성공 시 기존 JWT Cookie 정책에 따라 Access Token과 Refresh Token을 발급합니다.
                    이 API는 VP 로그인 API가 아닙니다.
                    VP 로그인은 POST /api/mobile/auth/vp-login/challenge 이후 POST /api/mobile/auth/vp-login을 사용합니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "모바일 로그인 응답 반환",
            content = @Content(schema = @Schema(implementation = MobileLoginResponse.class))
    )
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<MobileLoginResponse>> login(
            @Valid @RequestBody MobileLoginRequest request, // 모바일 로그인 요청
            HttpServletResponse response // HTTP 응답 객체
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                mobileAuthService.login(request, response)
        ));
    }

    /**
     * 모바일 자동로그인
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @return 모바일 자동로그인 응답
     */
    @Operation(
            summary = "모바일 자동로그인",
            description = "Refresh Token Cookie를 검증하고 Access Token과 Refresh Token Cookie를 재발급합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "모바일 자동로그인 응답 반환",
            content = @Content(schema = @Schema(implementation = MobileAutoLoginResponse.class))
    )
    @PostMapping("/auto-login")
    public ResponseEntity<CommonResponse<MobileAutoLoginResponse>> autoLogin(
            HttpServletRequest request, // HTTP 요청 객체
            HttpServletResponse response // HTTP 응답 객체
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                mobileAuthService.autoLogin(request, response)
        ));
    }
}
