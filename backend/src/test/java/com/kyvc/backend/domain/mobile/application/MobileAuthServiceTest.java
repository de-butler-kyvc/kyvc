package com.kyvc.backend.domain.mobile.application;

import com.kyvc.backend.domain.auth.application.AuthService;
import com.kyvc.backend.domain.auth.dto.TokenRefreshResponse;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.mobile.dto.MobileAutoLoginResponse;
import com.kyvc.backend.domain.mobile.repository.MobileDeviceBindingRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.JwtTokenProvider;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileAuthServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CORPORATE_ID = 10L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_ACCESS_TOKEN = "new-access-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    @Mock
    private AuthService authService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private TokenCookieUtil tokenCookieUtil;
    @Mock
    private MobileDeviceBindingRepository mobileDeviceBindingRepository;
    @Mock
    private CorporateRepository corporateRepository;

    private MobileAuthService service;

    @BeforeEach
    void setUp() {
        service = new MobileAuthService(
                authService,
                jwtTokenProvider,
                tokenCookieUtil,
                mobileDeviceBindingRepository,
                corporateRepository
        );
    }

    @Test
    void autoLogin_succeedsWithRefreshTokenCookieOnly() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        Corporate corporate = mock(Corporate.class);

        when(tokenCookieUtil.resolveRefreshToken(request)).thenReturn(REFRESH_TOKEN);
        when(authService.refresh(REFRESH_TOKEN)).thenReturn(new AuthService.TokenIssueResult<>(
                new TokenRefreshResponse(true),
                NEW_ACCESS_TOKEN,
                NEW_REFRESH_TOKEN
        ));
        when(tokenCookieUtil.createAccessTokenCookie(NEW_ACCESS_TOKEN))
                .thenReturn(ResponseCookie.from("KYVC_ACCESS_TOKEN", NEW_ACCESS_TOKEN).httpOnly(true).build());
        when(tokenCookieUtil.createRefreshTokenCookie(NEW_REFRESH_TOKEN))
                .thenReturn(ResponseCookie.from("KYVC_REFRESH_TOKEN", NEW_REFRESH_TOKEN).httpOnly(true).build());
        when(jwtTokenProvider.getUserId(NEW_ACCESS_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getEmail(NEW_ACCESS_TOKEN)).thenReturn("user@example.com");
        when(jwtTokenProvider.getUserType(NEW_ACCESS_TOKEN)).thenReturn("CORPORATE_USER");
        when(jwtTokenProvider.getExpiration(NEW_ACCESS_TOKEN)).thenReturn(Instant.parse("2026-05-16T13:00:00Z"));
        when(jwtTokenProvider.getExpiration(NEW_REFRESH_TOKEN)).thenReturn(Instant.parse("2026-06-15T13:00:00Z"));
        when(corporate.getCorporateId()).thenReturn(CORPORATE_ID);
        when(corporate.getCorporateName()).thenReturn("테스트 법인");
        when(corporateRepository.findByUserId(USER_ID)).thenReturn(Optional.of(corporate));

        MobileAutoLoginResponse autoLoginResponse = service.autoLogin(request, response);

        assertThat(autoLoginResponse.autoLogin()).isTrue();
        assertThat(autoLoginResponse.userId()).isEqualTo(USER_ID);
        assertThat(autoLoginResponse.corporateId()).isEqualTo(CORPORATE_ID);
        assertThat(autoLoginResponse.email()).isEqualTo("user@example.com");
        assertThat(autoLoginResponse.roleCode()).isEqualTo("CORPORATE_USER");
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .anySatisfy(header -> assertThat(header).contains("KYVC_ACCESS_TOKEN=" + NEW_ACCESS_TOKEN))
                .anySatisfy(header -> assertThat(header).contains("KYVC_REFRESH_TOKEN=" + NEW_REFRESH_TOKEN));
        verify(authService).refresh(REFRESH_TOKEN);
    }

    @Test
    void autoLogin_failsWhenRefreshTokenCookieMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenCookieUtil.resolveRefreshToken(request)).thenReturn(null);

        assertThatThrownBy(() -> service.autoLogin(request, response))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_NOT_FOUND);

        verify(authService, never()).refresh(REFRESH_TOKEN);
    }
}
