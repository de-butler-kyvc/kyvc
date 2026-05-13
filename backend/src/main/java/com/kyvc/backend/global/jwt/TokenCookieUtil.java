package com.kyvc.backend.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

// 토큰 Cookie 생성 및 조회 유틸
@Component
@RequiredArgsConstructor
public class TokenCookieUtil {

    private final JwtProperties jwtProperties;

    // Access Token Cookie 생성
    public ResponseCookie createAccessTokenCookie(String token // Access Token 원문
    ) {
        return createCookie(
                jwtProperties.getAccessCookieName(),
                token,
                Duration.ofMinutes(jwtProperties.getAccessTokenExpirationMinutes())
        );
    }

    // Refresh Token Cookie 생성
    public ResponseCookie createRefreshTokenCookie(String token // Refresh Token 원문
    ) {
        return createCookie(
                jwtProperties.getRefreshCookieName(),
                token,
                Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays())
        );
    }

    // HttpOnly Cookie 생성
    public ResponseCookie createHttpOnlyCookie(
            String name, // Cookie 이름
            String value, // Cookie 값
            Duration maxAge // Cookie 유지 시간
    ) {
        return createCookie(name, value, maxAge);
    }

    // Access Token Cookie 삭제
    public ResponseCookie deleteAccessTokenCookie() {
        return deleteCookie(jwtProperties.getAccessCookieName());
    }

    // Refresh Token Cookie 삭제
    public ResponseCookie deleteRefreshTokenCookie() {
        return deleteCookie(jwtProperties.getRefreshCookieName());
    }

    // HttpOnly Cookie 삭제
    public ResponseCookie deleteHttpOnlyCookie(
            String name // Cookie 이름
    ) {
        return deleteCookie(name);
    }

    // Access Token 조회
    public String resolveAccessToken(HttpServletRequest request // HTTP 요청
    ) {
        return resolveCookieValue(request, jwtProperties.getAccessCookieName());
    }

    // Refresh Token 조회
    public String resolveRefreshToken(HttpServletRequest request // HTTP 요청
    ) {
        return resolveCookieValue(request, jwtProperties.getRefreshCookieName());
    }

    // Cookie 값 조회
    public String resolveCookie(
            HttpServletRequest request, // HTTP 요청
            String cookieName // Cookie 이름
    ) {
        return resolveCookieValue(request, cookieName);
    }

    // 공통 Cookie 생성
    private ResponseCookie createCookie(
            String name, // Cookie 이름
            String value, // Cookie 값
            Duration maxAge // Cookie 유지 시간
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtProperties.isSecureCookie())
                .sameSite(jwtProperties.getSameSite())
                .path(jwtProperties.getCookiePath())
                .maxAge(maxAge)
                .build();
    }

    // 공통 Cookie 삭제
    private ResponseCookie deleteCookie(String name // Cookie 이름
    ) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(jwtProperties.isSecureCookie())
                .sameSite(jwtProperties.getSameSite())
                .path(jwtProperties.getCookiePath())
                .maxAge(0)
                .build();
    }

    // Cookie 값 조회
    private String resolveCookieValue(
            HttpServletRequest request, // HTTP 요청
            String cookieName // Cookie 이름
    ) {
        Cookie[] cookies = request.getCookies(); // 요청 Cookie 목록
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
