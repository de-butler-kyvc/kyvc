package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.AuthRoleService;
import com.kyvc.backend.domain.auth.dto.AuthRoleResponse;
import com.kyvc.backend.domain.auth.dto.AuthRoleSelectRequest;
import com.kyvc.backend.domain.auth.dto.AuthRoleSelectResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 역할 API Controller
 */
@RestController
@RequestMapping("/api/auth/roles")
@RequiredArgsConstructor
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class AuthRoleController {

    private final AuthRoleService authRoleService;
    private final TokenCookieUtil tokenCookieUtil;

    /**
     * 선택 가능 역할 목록 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 선택 가능 역할 목록 응답
     */
    @Operation(
            summary = "선택 가능 역할 목록 조회",
            description = "로그인 사용자가 보유한 역할 목록과 현재 선택 역할을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "보유 역할 목록 반환",
            content = @Content(schema = @Schema(implementation = AuthRoleResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<AuthRoleResponse>> getRoles(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(authRoleService.getRoles(userDetails)));
    }

    /**
     * 세션 역할 선택
     *
     * @param userDetails 인증 사용자 정보
     * @param request 역할 선택 요청
     * @param response HTTP 응답 객체
     * @return 역할 선택 응답
     */
    @Operation(
            summary = "세션 역할 선택",
            description = "로그인 사용자가 보유한 역할을 선택하고 선택 역할이 반영된 Access Token Cookie를 재발급합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "선택 역할 코드 반환",
            content = @Content(schema = @Schema(implementation = AuthRoleSelectResponse.class))
    )
    @PostMapping("/select")
    public ResponseEntity<CommonResponse<AuthRoleSelectResponse>> selectRole(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "역할 선택 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AuthRoleSelectRequest.class))
            )
            @Valid @RequestBody AuthRoleSelectRequest request, // 역할 선택 요청
            @Parameter(hidden = true)
            HttpServletResponse response // HTTP 응답 객체
    ) {
        AuthRoleService.SelectedRoleResult result = authRoleService.selectRole(
                getAuthenticatedUserId(userDetails),
                request
        );
        addCookie(response, tokenCookieUtil.createAccessTokenCookie(result.accessToken()));
        return ResponseEntity.ok(CommonResponseFactory.success(result.body()));
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

    // Set-Cookie 헤더 추가
    private void addCookie(
            HttpServletResponse response, // HTTP 응답 객체
            ResponseCookie cookie // 추가 대상 Cookie
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
