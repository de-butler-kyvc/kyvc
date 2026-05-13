package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.WebVpLoginService;
import com.kyvc.backend.domain.auth.dto.WebVpLoginCompleteResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginResolveRequest;
import com.kyvc.backend.domain.auth.dto.WebVpLoginResolveResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginStartResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginStatusResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginSubmitRequest;
import com.kyvc.backend.domain.auth.dto.WebVpLoginSubmitResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 웹 VP 로그인 API Controller
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "인증 / VP 로그인", description = "법인 사용자웹 VP 로그인 API")
public class WebVpLoginController {

    private final WebVpLoginService webVpLoginService;

    /**
     * 웹 VP 로그인 요청 생성
     *
     * @param response HTTP 응답
     * @return 웹 VP 로그인 시작 응답
     */
    @Operation(summary = "웹 VP 로그인 요청 생성")
    @PostMapping("/api/auth/vp-login-requests")
    public ResponseEntity<CommonResponse<WebVpLoginStartResponse>> start(
            @Parameter(hidden = true)
            HttpServletResponse response // HTTP 응답
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(webVpLoginService.start(response)));
    }

    /**
     * 웹 VP 로그인 요청 상태 조회
     *
     * @param requestId VP 로그인 요청 ID
     * @return 웹 VP 로그인 상태 응답
     */
    @Operation(summary = "웹 VP 로그인 요청 상태 조회")
    @GetMapping("/api/auth/vp-login-requests/{requestId}/status")
    public ResponseEntity<CommonResponse<WebVpLoginStatusResponse>> status(
            @PathVariable String requestId // VP 로그인 요청 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(webVpLoginService.status(requestId)));
    }

    /**
     * 웹 VP 로그인 완료
     *
     * @param requestId VP 로그인 요청 ID
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 웹 VP 로그인 완료 응답
     */
    @Operation(summary = "웹 VP 로그인 완료")
    @PostMapping("/api/auth/vp-login-requests/{requestId}/complete")
    public ResponseEntity<CommonResponse<WebVpLoginCompleteResponse>> complete(
            @PathVariable String requestId, // VP 로그인 요청 ID
            @Parameter(hidden = true)
            HttpServletRequest request, // HTTP 요청
            @Parameter(hidden = true)
            HttpServletResponse response // HTTP 응답
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(webVpLoginService.complete(requestId, request, response)));
    }

    /**
     * 모바일 QR 토큰 해석
     *
     * @param request QR 해석 요청
     * @return 웹 VP 로그인 QR 해석 응답
     */
    @Operation(summary = "모바일 웹 VP 로그인 QR 해석")
    @PostMapping("/api/mobile/auth/vp-login-requests/resolve")
    public ResponseEntity<CommonResponse<WebVpLoginResolveResponse>> resolve(
            @RequestBody WebVpLoginResolveRequest request // QR 해석 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(webVpLoginService.resolve(request)));
    }

    /**
     * 모바일 VP 객체 제출
     *
     * @param requestId VP 로그인 요청 ID
     * @param request VP 제출 요청
     * @return 웹 VP 로그인 제출 응답
     */
    @Operation(summary = "모바일 웹 VP 로그인 VP 제출")
    @PostMapping("/api/mobile/auth/vp-login-requests/{requestId}/submit")
    public ResponseEntity<CommonResponse<WebVpLoginSubmitResponse>> submit(
            @PathVariable String requestId, // VP 로그인 요청 ID
            @RequestBody WebVpLoginSubmitRequest request // VP 제출 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(webVpLoginService.submit(requestId, request)));
    }
}
