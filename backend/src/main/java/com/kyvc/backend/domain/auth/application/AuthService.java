package com.kyvc.backend.domain.auth.application;

import com.kyvc.backend.domain.auth.domain.AuthToken;
import com.kyvc.backend.domain.auth.dto.CorporateSignupRequest;
import com.kyvc.backend.domain.auth.dto.CorporateSignupResponse;
import com.kyvc.backend.domain.auth.dto.LoginRequest;
import com.kyvc.backend.domain.auth.dto.LoginResponse;
import com.kyvc.backend.domain.auth.dto.LogoutResponse;
import com.kyvc.backend.domain.auth.dto.TokenRefreshResponse;
import com.kyvc.backend.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backend.domain.user.domain.User;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.JwtTokenProvider;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// 인증 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LogEventLogger logEventLogger;

    // 법인 사용자 회원가입
    public CorporateSignupResponse signupCorporate(
            CorporateSignupRequest request // 회원가입 요청 데이터
    ) {
        validateSignupRequest(request);
        String email = normalizeEmail(request.email()); // 정규화된 이메일
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.createCorporateUser(
                email,
                passwordEncoder.encode(request.password()),
                normalizeRequired(request.userName()),
                normalizeOptional(request.phone()),
                normalizeRequired(request.corporateName())
        );
        User savedUser = userRepository.save(user);

        logEventLogger.info("auth.signup.success", "Corporate user signup success", Map.of(
                "userId", savedUser.getUserId(),
                "email", savedUser.getEmail()
        ));

        return new CorporateSignupResponse(
                savedUser.getOnboardingCorporateName(),
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getUserName(),
                savedUser.getPhone(),
                savedUser.getUserTypeCode().name(),
                savedUser.getUserStatusCode().name()
        );
    }

    // 로그인 처리
    public TokenIssueResult<LoginResponse> login(
            LoginRequest request // 로그인 요청 데이터
    ) {
        String email = normalizeEmail(request.email()); // 정규화된 이메일
        Map<String, Object> logFields = new LinkedHashMap<>(); // 로그인 로그 필드
        logFields.put("email", email);

        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException(ErrorCode.AUTH_LOGIN_FAILED));

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new ApiException(ErrorCode.AUTH_LOGIN_FAILED);
            }

            if (!user.isActive()) {
                throw new ApiException(ErrorCode.USER_INACTIVE);
            }

            logEventLogger.info("auth.login.success", "User login success", Map.of(
                    "userId", user.getUserId(),
                    "email", user.getEmail()
            ));

            return issueTokens(
                    user,
                    new LoginResponse(
                            user.getUserId(),
                            user.getEmail(),
                            user.getUserTypeCode().name(),
                            resolveRoles(user)
                    )
            );
        } catch (ApiException exception) {
            logEventLogger.warn("auth.login.failed", exception.getMessage(), logFields);
            throw exception;
        }
    }

    // 로컬 Dev Token 발급
    public TokenIssueResult<DevTokenIssueResult> issueDevToken(
            String email, // 발급 대상 이메일
            boolean autoCreateUser // 사용자 자동 생성 여부
    ) {
        String normalizedEmail = normalizeEmail(email); // 정규화된 이메일
        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null); // 기존 사용자 조회 결과
        boolean created = existingUser == null; // 신규 생성 사용자 여부
        User user = created
                ? createDevUser(normalizedEmail, autoCreateUser)
                : existingUser;

        if (!user.isActive()) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }

        logEventLogger.info("auth.dev_token.issued", "Dev token issued", Map.of(
                "userId", user.getUserId(),
                "email", user.getEmail(),
                "created", created
        ));

        return issueTokens(
                user,
                new DevTokenIssueResult(
                        user.getUserId(),
                        user.getEmail(),
                        user.getUserTypeCode().name(),
                        user.getUserStatusCode().name(),
                        created,
                        resolveRoles(user)
                )
        );
    }

    // 로그아웃 처리
    public LogoutResponse logout(
            Long authenticatedUserId, // 인증된 사용자 ID
            String refreshToken // Refresh Token 원문
    ) {
        requireRefreshToken(refreshToken);
        validateRefreshToken(refreshToken);

        Long refreshUserId = jwtTokenProvider.getUserId(refreshToken); // Refresh Token 사용자 ID
        if (!authenticatedUserId.equals(refreshUserId)) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        String tokenHash = TokenHashUtil.sha256(refreshToken); // Refresh Token 해시
        authTokenRepository.findActiveRefreshTokenByHash(tokenHash)
                .ifPresent(authTokenRepository::revoke);

        logEventLogger.info("auth.logout.success", "User logout success", Map.of(
                "userId", authenticatedUserId
        ));

        return new LogoutResponse(true);
    }

    // 토큰 재발급 처리
    public TokenIssueResult<TokenRefreshResponse> refresh(
            String refreshToken // Refresh Token 원문
    ) {
        Map<String, Object> logFields = new LinkedHashMap<>(); // 재발급 로그 필드

        try {
            requireRefreshToken(refreshToken);
            validateRefreshToken(refreshToken);

            Long userId = jwtTokenProvider.getUserId(refreshToken); // Refresh Token 사용자 ID
            logFields.put("userId", userId);

            String tokenHash = TokenHashUtil.sha256(refreshToken); // Refresh Token 해시
            AuthToken authToken = authTokenRepository.findRefreshTokenByHash(tokenHash)
                    .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND));

            if (!authToken.isActive()) {
                throw new ApiException(ErrorCode.AUTH_REFRESH_TOKEN_REVOKED);
            }

            if (authToken.isExpired(LocalDateTime.now())) {
                authTokenRepository.revoke(authToken);
                throw new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

            if (!user.isActive()) {
                authTokenRepository.revoke(authToken);
                throw new ApiException(ErrorCode.USER_INACTIVE);
            }

            authTokenRepository.revoke(authToken);

            logEventLogger.info("auth.token.refresh.success", "Refresh token success", Map.of(
                    "userId", user.getUserId(),
                    "email", user.getEmail()
            ));

            return issueTokens(user, new TokenRefreshResponse(true));
        } catch (ApiException exception) {
            logEventLogger.warn("auth.token.refresh.failed", exception.getMessage(), logFields.isEmpty() ? null : logFields);
            throw exception;
        }
    }

    // 토큰 발급 공통 처리
    private <T> TokenIssueResult<T> issueTokens(
            User user, // 토큰 발급 대상 사용자
            T body // 응답 본문
    ) {
        String accessToken = jwtTokenProvider.createAccessToken(user); // 신규 Access Token
        String refreshToken = jwtTokenProvider.createRefreshToken(user); // 신규 Refresh Token
        authTokenRepository.save(createRefreshTokenEntity(user, refreshToken));
        return new TokenIssueResult<>(body, accessToken, refreshToken);
    }

    // 검증 완료 사용자 토큰 발급
    public TokenIssueResult<Void> issueTokensForVerifiedUser(
            User user // 토큰 발급 대상 사용자
    ) {
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }
        return issueTokens(user, null);
    }

    // 사용자 권한 목록 조회
    public List<String> resolveUserRoles(
            User user // 권한 조회 대상 사용자
    ) {
        if (user == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return resolveRoles(user);
    }

    // Dev 사용자 생성
    private User createDevUser(
            String email, // 생성 대상 이메일
            boolean autoCreateUser // 자동 생성 허용 여부
    ) {
        if (!autoCreateUser) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.save(User.createCorporateUser(
                email,
                passwordEncoder.encode(UUID.randomUUID().toString())
        ));
    }

    // 회원가입 요청 검증
    private void validateSignupRequest(
            CorporateSignupRequest request // 회원가입 요청 데이터
    ) {
        if (request == null || !StringUtils.hasText(request.userName()) || !StringUtils.hasText(request.corporateName())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // Refresh Token 엔티티 생성
    private AuthToken createRefreshTokenEntity(
            User user, // 토큰 발급 대상 사용자
            String refreshToken // Refresh Token 원문
    ) {
        return AuthToken.createRefreshToken(
                user.getUserId(),
                TokenHashUtil.sha256(refreshToken),
                jwtTokenProvider.getJti(refreshToken),
                LocalDateTime.now(),
                toLocalDateTime(jwtTokenProvider.getExpiration(refreshToken))
        );
    }

    // Refresh Token 기본 검증
    private void validateRefreshToken(String refreshToken // Refresh Token 원문
    ) {
        jwtTokenProvider.validateToken(refreshToken);

        if (!REFRESH_TOKEN_TYPE.equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN_TYPE);
        }
    }

    // Refresh Token 필수값 검증
    private void requireRefreshToken(String refreshToken // Refresh Token 원문
    ) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 이메일 정규화
    private String normalizeEmail(String email // 원본 이메일
    ) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
    }

    // 선택 문자열 정규화
    private String normalizeOptional(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    // 권한 목록 생성
    private List<String> resolveRoles(User user // 권한 생성 대상 사용자
    ) {
        return List.of("ROLE_" + user.getUserTypeCode().name());
    }

    // Instant LocalDateTime 변환
    private LocalDateTime toLocalDateTime(Instant instant // 변환 대상 시각
    ) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    // 토큰 발급 결과
    public record TokenIssueResult<T>(
            T body,
            String accessToken,
            String refreshToken
    ) {
    }

    // Dev Token 발급 결과
    public record DevTokenIssueResult(
            Long userId,
            String email,
            String userType,
            String userStatus,
            boolean created,
            List<String> roles
    ) {
    }
}
