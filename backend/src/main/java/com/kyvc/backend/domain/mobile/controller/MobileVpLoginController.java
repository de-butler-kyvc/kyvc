package com.kyvc.backend.domain.mobile.controller;

import com.kyvc.backend.domain.mobile.application.MobileVpLoginService;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginChallengeRequest;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginChallengeResponse;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginRequest;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 모바일 VP 로그인 API Controller
 */
@RestController
@RequestMapping("/api/mobile/auth/vp-login")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class MobileVpLoginController {

    private final MobileVpLoginService mobileVpLoginService;

    /**
     * 모바일 VP 로그인 challenge를 생성한다.
     *
     * @param request 모바일 VP 로그인 challenge 요청
     * @return 모바일 VP 로그인 challenge 응답
     */
    @Operation(
            summary = "모바일 VP 로그인 challenge 생성",
            description = """
                    비로그인 모바일 앱이 VP 로그인 시작 시 호출합니다.
                    JWT/Cookie 인증이 필요하지 않습니다.
                    deviceId 등 모바일 기기 정보를 입력받고 backend가 Core에 VP challenge를 동기 요청합니다.
                    생성된 requestId, challenge, nonce, domain, aud, expiresAt을 반환합니다.
                    이 API는 일반 VP 제출 API가 아니며 로그인 토큰을 발급하지 않습니다.
                    토큰 발급은 POST /api/mobile/auth/vp-login에서 수행합니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "모바일 VP 로그인 challenge 응답 반환",
            content = @Content(schema = @Schema(implementation = MobileVpLoginChallengeResponse.class))
    )
    @PostMapping("/challenge")
    public ResponseEntity<CommonResponse<MobileVpLoginChallengeResponse>> createChallenge(
            @Valid @RequestBody MobileVpLoginChallengeRequest request // 모바일 VP 로그인 challenge 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(mobileVpLoginService.createChallenge(request)));
    }

    /**
     * 모바일 VP 로그인을 처리한다.
     *
     * @param request 모바일 VP 로그인 요청
     * @param response HTTP 응답
     * @return 모바일 VP 로그인 응답
     */
    @Operation(
            summary = "모바일 VP 로그인",
            description = """
                    비로그인 모바일 앱이 Wallet에서 생성한 VP JWT를 제출해 로그인합니다.
                    requestId, credentialId, vpJwt, nonce, challenge, deviceId를 입력받습니다.
                    backend는 저장된 VP 로그인 challenge와 nonce를 검증하고 Core에 VP 검증을 동기 요청합니다.
                    검증 성공 시 기존 모바일 로그인과 동일하게 Access Token과 Refresh Token을 HttpOnly Cookie로 발급합니다.
                    이 API는 일반 VP 제출 API가 아니며 POST /api/mobile/vp/presentations를 대체하지 않습니다.
                    VP JWT 원문은 저장하거나 로그에 남기지 않습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "모바일 VP 로그인 응답 반환",
            content = @Content(schema = @Schema(implementation = MobileVpLoginResponse.class))
    )
    @PostMapping
    public ResponseEntity<CommonResponse<MobileVpLoginResponse>> login(
            @Valid @RequestBody MobileVpLoginRequest request, // 모바일 VP 로그인 요청
            HttpServletResponse response // HTTP 응답
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(mobileVpLoginService.login(request, response)));
    }
}
