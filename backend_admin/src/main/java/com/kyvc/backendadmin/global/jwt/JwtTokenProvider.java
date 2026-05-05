package com.kyvc.backendadmin.global.jwt;

import com.kyvc.backendadmin.domain.user.domain.User;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// JWT 발급 및 검증 컴포넌트
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ACTOR_TYPE = "actorType";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    void initialize() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    // Access Token 생성
    public String createAccessToken(User user // 토큰 발급 대상 사용자
    ) {
        return createAccessToken(toTokenPrincipal(user));
    }

    // Access Token 생성
    public String createAccessToken(TokenPrincipal principal // 토큰 발급 대상 주체
    ) {
        return createToken(principal, ACCESS_TOKEN_TYPE, Duration.ofMinutes(jwtProperties.getAccessTokenExpirationMinutes()));
    }

    // Refresh Token 생성
    public String createRefreshToken(User user // 토큰 발급 대상 사용자
    ) {
        return createRefreshToken(toTokenPrincipal(user));
    }

    // Refresh Token 생성
    public String createRefreshToken(TokenPrincipal principal // 토큰 발급 대상 주체
    ) {
        return createToken(principal, REFRESH_TOKEN_TYPE, Duration.ofDays(jwtProperties.getRefreshTokenExpirationDays()));
    }

    // JWT 유효성 검증
    public boolean validateToken(String token // 검증 대상 JWT
    ) {
        Claims claims = parseClaims(token);
        validateIssuer(claims);
        return true;
    }

    // 토큰 유형 조회
    public String getTokenType(String token // 조회 대상 JWT
    ) {
        return getRequiredStringClaim(parseClaims(token), CLAIM_TOKEN_TYPE, ErrorCode.AUTH_TOKEN_INVALID);
    }

    // 사용자 ID 조회
    public Long getUserId(String token // 조회 대상 JWT
    ) {
        Claims claims = parseClaims(token);
        validateIssuer(claims);
        try {
            return Long.parseLong(claims.getSubject());
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    // 이메일 조회
    public String getEmail(String token // 조회 대상 JWT
    ) {
        return getRequiredStringClaim(parseClaims(token), CLAIM_EMAIL, ErrorCode.AUTH_TOKEN_INVALID);
    }

    // 사용자 유형 조회
    public String getUserType(String token // 조회 대상 JWT
    ) {
        Claims claims = parseClaims(token);
        Object userType = claims.get(CLAIM_USER_TYPE);
        if (userType instanceof String value && !value.isBlank()) {
            return value;
        }
        return getRequiredStringClaim(claims, CLAIM_ACTOR_TYPE, ErrorCode.AUTH_TOKEN_INVALID);
    }

    // 행위자 유형 조회
    public String getActorType(String token // 조회 대상 JWT
    ) {
        return getRequiredStringClaim(parseClaims(token), CLAIM_ACTOR_TYPE, ErrorCode.AUTH_TOKEN_INVALID);
    }

    // 권한 목록 조회
    public List<String> getRoles(String token // 조회 대상 JWT
    ) {
        Object rolesClaim = parseClaims(token).get(CLAIM_ROLES); // 권한 클레임 원본
        if (!(rolesClaim instanceof List<?> roles)) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        return roles.stream()
                .map(String::valueOf)
                .toList();
    }

    // JWT ID 조회
    public String getJti(String token // 조회 대상 JWT
    ) {
        Claims claims = parseClaims(token);
        String jti = claims.getId(); // JWT ID
        if (jti == null || jti.isBlank()) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return jti;
    }

    // 만료 시각 조회
    public Instant getExpiration(String token // 조회 대상 JWT
    ) {
        Date expiration = parseClaims(token).getExpiration(); // JWT 만료 시각
        if (expiration == null) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return expiration.toInstant();
    }

    // JWT 공통 생성
    private String createToken(
            TokenPrincipal principal, // 토큰 발급 대상 주체
            String tokenType, // 토큰 유형
            Duration duration // 유효기간
    ) {
        Instant now = Instant.now(); // 발급 시각
        Instant expiration = now.plus(duration); // 만료 시각
        List<String> roles = normalizeRoles(principal.roles()); // 권한 목록

        return Jwts.builder()
                .subject(String.valueOf(principal.actorId()))
                .claim(CLAIM_EMAIL, principal.email())
                .claim(CLAIM_ACTOR_TYPE, principal.actorType())
                .claim(CLAIM_USER_TYPE, principal.actorType())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    // 사용자 엔티티를 토큰 주체로 변환
    private TokenPrincipal toTokenPrincipal(User user // 토큰 발급 대상 사용자
    ) {
        return new TokenPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getUserTypeCode().name(),
                resolveRoles(user)
        );
    }

    // 사용자 권한 목록 생성
    private List<String> resolveRoles(User user // 토큰 발급 대상 사용자
    ) {
        return List.of("ROLE_" + user.getUserTypeCode().name());
    }

    // 권한 접두사 보정
    private List<String> normalizeRoles(List<String> roles // 토큰 권한 목록
    ) {
        if (roles == null || roles.isEmpty()) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        List<String> normalizedRoles = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .distinct()
                .toList();
        if (normalizedRoles.isEmpty()) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return normalizedRoles;
    }

    // JWT Claims 파싱
    private Claims parseClaims(String token // 파싱 대상 JWT
    ) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            validateIssuer(claims);
            return claims;
        } catch (ExpiredJwtException exception) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    // 발급자 검증
    private void validateIssuer(Claims claims // 검증 대상 Claims
    ) {
        String issuer = claims.getIssuer(); // 토큰 발급자
        if (issuer == null || !jwtProperties.getIssuer().equals(issuer)) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    // 문자열 클레임 조회
    private String getRequiredStringClaim(
            Claims claims, // 조회 대상 Claims
            String claimName, // 클레임 키
            ErrorCode errorCode // 실패 시 에러 코드
    ) {
        validateIssuer(claims);
        Object claimValue = claims.get(claimName); // 클레임 원본값
        if (!(claimValue instanceof String value) || value.isBlank()) {
            throw new ApiException(errorCode);
        }
        return value;
    }
}
