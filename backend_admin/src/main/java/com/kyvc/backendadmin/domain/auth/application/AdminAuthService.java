package com.kyvc.backendadmin.domain.auth.application;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRepository;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.dto.AdminLoginRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminLoginResponse;
import com.kyvc.backendadmin.domain.auth.dto.AdminSessionResponse;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.JwtProperties;
import com.kyvc.backendadmin.global.jwt.JwtTokenProvider;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.jwt.TokenPrincipal;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

// Backend Admin 로그인, 로그아웃, 토큰 재발급, 세션 조회 유스케이스 서비스
/**
 * Backend Admin 인증 유스케이스를 처리하는 서비스입니다.
 *
 * <p>관리자 계정 상태와 비밀번호를 검증하고 JWT를 발급하며, Refresh Token의
 * 서버 저장 상태를 관리합니다. 읽기성 세션 조회는 read-only 트랜잭션으로 처리하고,
 * 로그인/로그아웃처럼 토큰 상태가 변경되는 흐름은 쓰기 트랜잭션으로 처리합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    // Access Token claim 검증에 사용하는 토큰 타입 문자열
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    // Refresh Token claim 검증에 사용하는 토큰 타입 문자열
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";
    // 관리자 토큰임을 식별하는 actorType 값
    private static final String ADMIN_ACTOR_TYPE = "ADMIN";

    // 관리자 계정과 권한 조회 저장소
    private final AdminUserRepository adminUserRepository;
    // Refresh Token 저장/조회/폐기 저장소
    private final AuthTokenRepository authTokenRepository;
    // BCrypt 비밀번호 검증기
    private final PasswordEncoder passwordEncoder;
    // JWT 발급 및 검증 provider
    private final JwtTokenProvider jwtTokenProvider;
    // JWT 만료 시간과 쿠키 설정 프로퍼티
    private final JwtProperties jwtProperties;

    // 이메일/비밀번호로 관리자 로그인 후 Access/Refresh Token 발급
    /**
     * 관리자 로그인을 처리합니다.
     *
     * <p>이메일로 관리자를 조회하고 계정 상태, 비밀번호, 권한 할당 여부를 검증한 뒤
     * Access Token과 Refresh Token을 발급합니다. Refresh Token 원문은 저장하지 않고
     * 해시만 auth_tokens에 저장합니다.</p>
     *
     * @param request 로그인 이메일과 비밀번호
     * @return 발급된 토큰, 관리자 요약 정보와 권한 목록
     */
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser adminUser = adminUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_LOGIN_FAILED));
        validateAdminCanLogin(adminUser);

        if (!passwordEncoder.matches(request.password(), adminUser.getPasswordHash())) {
            throw new ApiException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        List<String> roles = adminUserRepository.findRoleCodesByAdminId(adminUser.getAdminId());
        validateRoles(roles);
        TokenPrincipal principal = adminPrincipal(adminUser, roles);
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        String refreshToken = jwtTokenProvider.createRefreshToken(principal);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(jwtProperties.getRefreshTokenExpirationDays());

        authTokenRepository.save(AuthToken.create(
                KyvcEnums.ActorType.ADMIN,
                adminUser.getAdminId(),
                KyvcEnums.TokenType.REFRESH,
                TokenHashUtil.sha256(refreshToken),
                refreshExpiresAt
        ));
        adminUser.updateLastLoginAt(LocalDateTime.now());

        return new AdminLoginResponse(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpirationMinutes() * 60,
                toAdminSummary(adminUser),
                roles
        );
    }

    // Refresh Token 해시를 찾아 활성 토큰이면 폐기 처리
    /**
     * 관리자 로그아웃을 처리합니다.
     *
     * <p>Refresh Token이 전달된 경우 해시로 저장 토큰을 조회하고, 활성 상태이면
     * REVOKED 상태로 전이합니다. 토큰이 없거나 이미 없는 경우에도 멱등적으로 성공 처리합니다.</p>
     *
     * @param refreshToken 폐기할 Refresh Token 원문
     */
    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }

        authTokenRepository.findByTokenHashAndTokenType(
                        TokenHashUtil.sha256(refreshToken),
                        KyvcEnums.TokenType.REFRESH
                )
                .filter(AuthToken::isActive)
                .ifPresent(authToken -> authToken.revoke(LocalDateTime.now()));
    }

    // Refresh Token 유효성, DB 저장 상태, 관리자 상태를 확인하고 새 Access Token 발급
    /**
     * Refresh Token으로 Access Token을 재발급합니다.
     *
     * <p>JWT 서명과 tokenType, actorType을 검증하고, auth_tokens에 저장된 해시 토큰의
     * ACTIVE 상태와 만료 여부를 확인합니다. 이후 관리자 상태와 권한을 다시 확인해 새
     * Access Token을 발급합니다.</p>
     *
     * @param refreshToken 재발급에 사용할 Refresh Token 원문
     * @return 새 Access Token과 기존 Refresh Token, 관리자 정보와 권한 목록
     */
    @Transactional(readOnly = true)
    public AdminLoginResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new ApiException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND);
        }

        jwtTokenProvider.validateToken(refreshToken);
        if (!REFRESH_TOKEN_TYPE.equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN_TYPE);
        }
        if (!ADMIN_ACTOR_TYPE.equals(jwtTokenProvider.getActorType(refreshToken))) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        AuthToken storedToken = authTokenRepository.findByTokenHashAndTokenType(
                        TokenHashUtil.sha256(refreshToken),
                        KyvcEnums.TokenType.REFRESH
                )
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND));
        validateStoredToken(storedToken, ErrorCode.AUTH_REFRESH_TOKEN_REVOKED);

        AdminUser adminUser = adminUserRepository.findById(storedToken.getActorId())
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));
        validateAdminCanLogin(adminUser);

        List<String> roles = adminUserRepository.findRoleCodesByAdminId(adminUser.getAdminId());
        validateRoles(roles);
        String accessToken = jwtTokenProvider.createAccessToken(adminPrincipal(adminUser, roles));
        // TODO refresh token rotation policy 확정 후 refresh token 재발급과 기존 토큰 폐기를 적용한다.
        return new AdminLoginResponse(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpirationMinutes() * 60,
                toAdminSummary(adminUser),
                roles
        );
    }

    // Access Token이 없으면 비인증 세션, 있으면 검증된 관리자 세션 반환
    /**
     * 현재 세션 정보를 조회합니다.
     *
     * <p>Access Token이 없으면 비인증 세션을 반환합니다. 토큰이 있으면 JWT와 관리자 상태,
     * 권한 정보를 검증해 인증된 세션 응답을 반환합니다.</p>
     *
     * @param accessToken 세션 조회에 사용할 Access Token 원문
     * @return 인증 여부와 관리자 세션 정보
     */
    @Transactional(readOnly = true)
    public AdminSessionResponse session(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return AdminSessionResponse.unauthenticated();
        }

        jwtTokenProvider.validateToken(accessToken);
        if (!ACCESS_TOKEN_TYPE.equals(jwtTokenProvider.getTokenType(accessToken))) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN_TYPE);
        }
        if (!ADMIN_ACTOR_TYPE.equals(jwtTokenProvider.getActorType(accessToken))) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        AdminUser adminUser = adminUserRepository.findById(jwtTokenProvider.getUserId(accessToken))
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));
        validateAdminCanLogin(adminUser);

        List<String> roles = adminUserRepository.findRoleCodesByAdminId(adminUser.getAdminId());
        validateRoles(roles);
        return new AdminSessionResponse(true, toAdminSummary(adminUser), roles, 0);
    }

    // SecurityContext의 현재 관리자 ID로 내 정보 조회
    /**
     * 현재 인증된 관리자 정보를 조회합니다.
     *
     * <p>SecurityContext에서 관리자 ID를 가져와 관리자 계정과 권한 목록을 조회합니다.</p>
     *
     * @return 현재 관리자 정보와 권한 목록
     */
    @Transactional(readOnly = true)
    public AdminSessionResponse me() {
        AdminUser adminUser = adminUserRepository.findById(SecurityUtil.getCurrentAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));
        List<String> roles = adminUserRepository.findRoleCodesByAdminId(adminUser.getAdminId());
        validateRoles(roles);
        return new AdminSessionResponse(true, toAdminSummary(adminUser), roles, 0);
    }

    // 관리자 엔티티를 응답용 요약 DTO로 변환
    /**
     * 관리자 엔티티를 공통 요약 응답으로 변환합니다.
     *
     * @param adminUser 변환할 관리자 엔티티
     * @return 관리자 요약 응답
     */
    public AdminLoginResponse.AdminSummary toAdminSummary(AdminUser adminUser) {
        return new AdminLoginResponse.AdminSummary(
                adminUser.getAdminId(),
                adminUser.getEmail(),
                adminUser.getDisplayName(),
                adminUser.getStatus().name()
        );
    }

    // JWT 발급에 사용할 관리자 principal 생성
    private TokenPrincipal adminPrincipal(AdminUser adminUser, List<String> roles) {
        return new TokenPrincipal(adminUser.getAdminId(), adminUser.getEmail(), ADMIN_ACTOR_TYPE, roles);
    }

    // DB에 저장된 토큰 상태와 만료 여부 검증
    private void validateStoredToken(AuthToken authToken, ErrorCode inactiveErrorCode) {
        if (!authToken.isActive()) {
            throw new ApiException(inactiveErrorCode);
        }
        if (authToken.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }
    }

    // 관리자 계정 로그인 가능 상태 검증
    private void validateAdminCanLogin(AdminUser adminUser) {
        if (adminUser.isLocked()) {
            throw new ApiException(ErrorCode.ADMIN_LOCKED);
        }
        if (!adminUser.isActive()) {
            throw new ApiException(ErrorCode.ADMIN_INACTIVE);
        }
    }

    // 관리자에게 최소 하나 이상의 권한이 할당되어 있는지 검증
    private void validateRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ApiException(ErrorCode.ADMIN_ROLE_NOT_FOUND);
        }
    }
}
