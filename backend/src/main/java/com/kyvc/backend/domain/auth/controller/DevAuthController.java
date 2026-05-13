package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.AuthService;
import com.kyvc.backend.domain.auth.dto.DevTokenIssueRequest;
import com.kyvc.backend.domain.auth.dto.DevTokenIssueResponse;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.DevTokenProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 Dev Token API Controller
 */
@RestController
@Profile("local")
@RequestMapping("/api/auth/dev")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kyvc.dev-token", name = "enabled", havingValue = "true")
@Tag(name = "인증 / 계정", description = "사용자 인증, 계정, 개발 토큰, 모바일 로그인 및 비밀번호 재설정 API")
public class DevAuthController {

    private final AuthService authService;
    private final TokenCookieUtil tokenCookieUtil;
    private final DevTokenProperties devTokenProperties;

    /**
     * 로컬 Dev Token 발급
     *
     * @param request Dev Token 발급 요청 데이터
     * @param response HTTP 응답 객체
     * @return Dev Token 발급 응답
     */
    @Operation(
            summary = "로컬 Dev Token 발급",
            description = "로컬 개발 환경에서 테스트용 인증 토큰을 발급합니다. 입력값은 발급 대상 이메일이며, 생략 시 기본 이메일을 사용합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 ID, 이메일, 사용자 유형, 사용자 상태, 신규 생성 여부, 권한 목록 반환",
            content = @Content(schema = @Schema(implementation = DevTokenIssueResponse.class))
    )
    @PostMapping("/token")
    public ResponseEntity<CommonResponse<DevTokenIssueResponse>> issueDevToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dev Token 발급 요청 데이터",
                    required = false,
                    content = @Content(schema = @Schema(implementation = DevTokenIssueRequest.class))
            )
            @Valid @RequestBody(required = false) DevTokenIssueRequest request, // Dev Token 발급 요청 데이터
            @Parameter(hidden = true)
            HttpServletResponse response // HTTP 응답 객체
    ) {
        String email = resolveEmail(request); // 발급 대상 이메일
        AuthService.TokenIssueResult<AuthService.DevTokenIssueResult> result = authService.issueDevToken(
                email,
                devTokenProperties.isAutoCreateUser()
        );

        addCookie(response, tokenCookieUtil.createAccessTokenCookie(result.accessToken()));
        addCookie(response, tokenCookieUtil.createRefreshTokenCookie(result.refreshToken()));

        AuthService.DevTokenIssueResult body = result.body(); // 발급 결과 데이터
        return ResponseEntity.ok(CommonResponseFactory.success(new DevTokenIssueResponse(
                body.userId(),
                body.email(),
                body.userType(),
                body.userStatus(),
                body.created(),
                body.roles()
        )));
    }

    // 발급 대상 이메일 결정
    private String resolveEmail(DevTokenIssueRequest request // Dev Token 발급 요청 데이터
    ) {
        if (request == null || !StringUtils.hasText(request.email())) {
            return devTokenProperties.getDefaultEmail();
        }
        return request.email();
    }

    // Set-Cookie 헤더 추가
    private void addCookie(
            HttpServletResponse response, // HTTP 응답 객체
            ResponseCookie cookie // 추가 대상 Cookie
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
