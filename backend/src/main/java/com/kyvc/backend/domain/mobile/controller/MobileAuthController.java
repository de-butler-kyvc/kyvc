package com.kyvc.backend.domain.mobile.controller;

import com.kyvc.backend.domain.mobile.application.MobileAuthService;
import com.kyvc.backend.domain.mobile.dto.MobileLoginRequest;
import com.kyvc.backend.domain.mobile.dto.MobileLoginResponse;
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
 * 모바일 인증 API Controller
 */
@RestController
@RequestMapping("/api/mobile/auth")
@RequiredArgsConstructor
@Tag(name = "모바일 인증", description = "모바일 로그인 API")
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
            summary = "모바일 로그인",
            description = "모바일 앱 로그인 후 기존 JWT Cookie 정책에 따라 인증 토큰을 발급합니다."
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
}
