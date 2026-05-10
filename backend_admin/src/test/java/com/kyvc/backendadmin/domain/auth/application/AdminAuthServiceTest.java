package com.kyvc.backendadmin.domain.auth.application;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRepository;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.dto.AdminLoginRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminLoginResponse;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.JwtProperties;
import com.kyvc.backendadmin.global.jwt.JwtTokenProvider;
import com.kyvc.backendadmin.global.jwt.TokenPrincipal;
import com.kyvc.backendadmin.global.logging.LogEventLogger;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    private static final String EMAIL = "operator@kyvc.local";
    private static final String PASSWORD = "Password123!";
    private static final String BCRYPT_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi2GZrI6QErYjvZBmy3lMEZjv3apJSO";

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private LogEventLogger logEventLogger;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminAuthService = new AdminAuthService(
                adminUserRepository,
                authTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties,
                logEventLogger
        );
    }

    @Test
    void loginFailsWithoutJpaAccessWhenStoredPasswordHashIsNotBcrypt() {
        AdminUser adminUser = activeAdmin(1L, "{change-me-bcrypt-hash}");
        when(adminUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(adminUser));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> adminAuthService.login(new AdminLoginRequest(EMAIL, PASSWORD))
        );

        assertEquals(ErrorCode.AUTH_LOGIN_FAILED, exception.getErrorCode());
        verify(logEventLogger).warn(eq("auth.login.failed"), eq(ErrorCode.AUTH_LOGIN_FAILED.getMessage()), anyMap());
        verify(adminUserRepository, never()).findRoleCodesByAdminId(any());
        verifyNoInteractions(passwordEncoder, jwtTokenProvider, authTokenRepository);
    }

    @Test
    void loginFailsWithBusinessExceptionWhenAdminHasNoActiveRoles() {
        AdminUser adminUser = activeAdmin(10L, BCRYPT_HASH);
        when(adminUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(PASSWORD, BCRYPT_HASH)).thenReturn(true);
        when(adminUserRepository.findRoleCodesByAdminId(10L)).thenReturn(null);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> adminAuthService.login(new AdminLoginRequest(EMAIL, PASSWORD))
        );

        assertEquals(ErrorCode.ADMIN_ROLE_NOT_FOUND, exception.getErrorCode());
        verifyNoInteractions(jwtTokenProvider, authTokenRepository);
    }

    @Test
    void loginSucceedsForOperatorWithActiveRoleMapping() {
        AdminUser adminUser = activeAdmin(30L, BCRYPT_HASH);
        when(adminUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(PASSWORD, BCRYPT_HASH)).thenReturn(true);
        when(adminUserRepository.findRoleCodesByAdminId(30L)).thenReturn(List.of("ROLE_OPERATOR"));
        when(jwtTokenProvider.createAccessToken(any(TokenPrincipal.class))).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(any(TokenPrincipal.class))).thenReturn("refresh-token");
        when(jwtProperties.getAccessTokenExpirationMinutes()).thenReturn(30L);
        when(jwtProperties.getRefreshTokenExpirationDays()).thenReturn(14L);

        AdminLoginResponse response = adminAuthService.login(new AdminLoginRequest(EMAIL, PASSWORD));

        assertEquals(List.of("ROLE_OPERATOR"), response.roles());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        verify(authTokenRepository).save(any(AuthToken.class));
    }

    private AdminUser activeAdmin(Long adminId, String passwordHash) {
        AdminUser adminUser = AdminUser.create(
                EMAIL,
                "Operator",
                passwordHash,
                KyvcEnums.AdminUserStatus.ACTIVE
        );
        ReflectionTestUtils.setField(adminUser, "adminId", adminId);
        return adminUser;
    }
}
