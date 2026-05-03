package com.kyvc.backend.domain.auth.controller;

import com.kyvc.backend.domain.auth.application.AuthService;
import com.kyvc.backend.domain.auth.dto.DevTokenIssueRequest;
import com.kyvc.backend.domain.auth.dto.DevTokenIssueResponse;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.DevTokenProperties;
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
    @PostMapping("/token")
    public ResponseEntity<CommonResponse<DevTokenIssueResponse>> issueDevToken(
            @Valid @RequestBody(required = false) DevTokenIssueRequest request, // Dev Token 발급 요청 데이터
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
