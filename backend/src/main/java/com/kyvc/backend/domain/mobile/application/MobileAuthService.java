package com.kyvc.backend.domain.mobile.application;

import com.kyvc.backend.domain.auth.application.AuthService;
import com.kyvc.backend.domain.auth.dto.LoginRequest;
import com.kyvc.backend.domain.auth.dto.LoginResponse;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.mobile.domain.MobileDeviceBinding;
import com.kyvc.backend.domain.mobile.dto.MobileLoginRequest;
import com.kyvc.backend.domain.mobile.dto.MobileLoginResponse;
import com.kyvc.backend.domain.mobile.repository.MobileDeviceBindingRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.JwtTokenProvider;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// 모바일 인증 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class MobileAuthService {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenCookieUtil tokenCookieUtil;
    private final MobileDeviceBindingRepository mobileDeviceBindingRepository;
    private final CorporateRepository corporateRepository;

    // 모바일 로그인
    public MobileLoginResponse login(
            MobileLoginRequest request, // 모바일 로그인 요청
            HttpServletResponse response // HTTP 응답 객체
    ) {
        validateLoginRequest(request);

        AuthService.TokenIssueResult<LoginResponse> result = authService.login(
                new LoginRequest(request.email(), request.password())
        );

        addCookie(response, tokenCookieUtil.createAccessTokenCookie(result.accessToken()));
        addCookie(response, tokenCookieUtil.createRefreshTokenCookie(result.refreshToken()));

        Long userId = result.body().userId(); // 로그인 사용자 ID
        boolean deviceRegistered = false; // 기기 등록 여부
        if (StringUtils.hasText(request.deviceId())) {
            MobileDeviceBinding deviceBinding = mobileDeviceBindingRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                    .orElse(null); // 기존 기기 조회 결과
            if (deviceBinding != null && deviceBinding.isActive()) {
                deviceBinding.updateLastUsedAt(LocalDateTime.now());
                mobileDeviceBindingRepository.save(deviceBinding);
                deviceRegistered = true;
            }
        }

        return new MobileLoginResponse(
                userId,
                result.body().userType(),
                result.body().email(),
                corporateRepository.findByUserId(userId)
                        .map(corporate -> corporate.getCorporateName())
                        .orElse(null),
                request.deviceId(),
                deviceRegistered,
                toLocalDateTime(jwtTokenProvider.getExpiration(result.accessToken()))
        );
    }

    // 모바일 로그인 요청 검증
    private void validateLoginRequest(
            MobileLoginRequest request // 모바일 로그인 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.email())
                || !StringUtils.hasText(request.password())
                || !StringUtils.hasText(request.deviceId())
                || !StringUtils.hasText(request.os())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 쿠키 헤더 추가
    private void addCookie(
            HttpServletResponse response, // HTTP 응답 객체
            ResponseCookie cookie // 추가 대상 쿠키
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Instant LocalDateTime 변환
    private LocalDateTime toLocalDateTime(
            Instant instant // 변환 대상 시각
    ) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
