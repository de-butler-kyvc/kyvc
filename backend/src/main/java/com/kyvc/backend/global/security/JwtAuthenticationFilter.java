package com.kyvc.backend.global.security;

import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.JwtTokenProvider;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// JWT 인증 필터
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenCookieUtil tokenCookieUtil;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request // 요청 정보
    ) {
        String path = request.getRequestURI(); // 요청 경로
        return "/api/auth/login".equals(path)
                || "/api/auth/signup/corporate".equals(path)
                || "/api/auth/token/refresh".equals(path)
                || "/api/auth/password-reset/request".equals(path)
                || "/api/auth/password-reset/confirm".equals(path)
                || "/api/auth/dev/token".equals(path)
                || "/api/mobile/auth/login".equals(path)
                || path.startsWith("/api/verifier/")
                || path.startsWith("/api/internal/")
                || "/health".equals(path)
                || path.startsWith("/actuator/health")
                || "/actuator/info".equals(path)
                || "/swagger-ui.html".equals(path)
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            FilterChain filterChain // 다음 필터 체인
    ) throws ServletException, IOException {
        String accessToken = resolveAccessToken(request); // 요청 Access Token

        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtTokenProvider.validateToken(accessToken);

            String tokenType = jwtTokenProvider.getTokenType(accessToken); // JWT 토큰 유형
            if (!"ACCESS".equals(tokenType)) {
                throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN_TYPE);
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                CustomUserDetails userDetails = new CustomUserDetails(
                        jwtTokenProvider.getUserId(accessToken),
                        jwtTokenProvider.getEmail(accessToken),
                        jwtTokenProvider.getUserType(accessToken),
                        jwtTokenProvider.getRoles(accessToken),
                        true
                );

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                ); // Security 인증 객체

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            SecurityContextHolder.clearContext();
            jwtAuthenticationEntryPoint.writeErrorResponse(response, exception.getErrorCode(), exception.getMessage());
        }
    }

    // Access Token 조회
    private String resolveAccessToken(HttpServletRequest request // 요청 정보
    ) {
        String cookieToken = tokenCookieUtil.resolveAccessToken(request); // Access Token Cookie 값
        if (StringUtils.hasText(cookieToken)) {
            return cookieToken;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION); // Authorization 헤더
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
