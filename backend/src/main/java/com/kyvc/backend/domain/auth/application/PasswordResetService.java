package com.kyvc.backend.domain.auth.application;

import com.kyvc.backend.domain.auth.config.PasswordResetProperties;
import com.kyvc.backend.domain.auth.domain.AuthToken;
import com.kyvc.backend.domain.auth.dto.PasswordResetConfirmRequest;
import com.kyvc.backend.domain.auth.dto.PasswordResetConfirmResponse;
import com.kyvc.backend.domain.auth.dto.PasswordResetRequest;
import com.kyvc.backend.domain.auth.dto.PasswordResetRequestResponse;
import com.kyvc.backend.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backend.domain.user.domain.User;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.mail.EmailSender;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

// 비밀번호 재설정 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final PasswordResetProperties passwordResetProperties;

    // 비밀번호 재설정 요청 생성
    public PasswordResetRequestResponse request(
            PasswordResetRequest request // 비밀번호 재설정 요청 생성 요청
    ) {
        validateResetRequest(request);

        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email)
                .filter(User::isActive)
                .ifPresent(this::issuePasswordResetTokenAndSendEmail);

        return new PasswordResetRequestResponse(true);
    }

    // 비밀번호 재설정 확정
    public PasswordResetConfirmResponse confirm(
            PasswordResetConfirmRequest request // 비밀번호 재설정 확정 요청
    ) {
        validateConfirmRequest(request);

        String tokenHash = TokenHashUtil.sha256(request.resetToken().trim());
        AuthToken authToken = authTokenRepository.findByHashAndType(tokenHash, KyvcEnums.TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        if (authToken.getActorTypeCode() != KyvcEnums.ActorType.USER) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }
        if (!authToken.isActive()) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        if (authToken.isExpired(now)) {
            authTokenRepository.revoke(authToken);
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(authToken.getActorId())
                .orElseThrow(() -> new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }

        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        authToken.markUsed(now);
        authTokenRepository.save(authToken);

        return new PasswordResetConfirmResponse(true);
    }

    // 비밀번호 재설정 토큰 발급 및 이메일 발송
    private void issuePasswordResetTokenAndSendEmail(
            User user // 토큰 발급 대상 사용자
    ) {
        String resetToken = issuePasswordResetToken(user);
        emailSender.send(
                user.getEmail(),
                buildPasswordResetEmailSubject(),
                buildPasswordResetEmailBody(resetToken)
        );
    }

    // 비밀번호 재설정 토큰 발급
    private String issuePasswordResetToken(
            User user // 토큰 발급 대상 사용자
    ) {
        LocalDateTime now = LocalDateTime.now();
        String resetToken = UUID.randomUUID().toString();
        AuthToken authToken = AuthToken.createUserToken(
                user.getUserId(),
                KyvcEnums.TokenType.PASSWORD_RESET,
                TokenHashUtil.sha256(resetToken),
                null,
                now,
                now.plusMinutes(passwordResetProperties.resolvedExpirationMinutes())
        );
        authTokenRepository.save(authToken);
        return resetToken;
    }

    // 비밀번호 재설정 요청 검증
    private void validateResetRequest(
            PasswordResetRequest request // 비밀번호 재설정 요청 생성 요청
    ) {
        if (request == null || !StringUtils.hasText(request.email())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 비밀번호 재설정 확정 요청 검증
    private void validateConfirmRequest(
            PasswordResetConfirmRequest request // 비밀번호 재설정 확정 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.resetToken())
                || !StringUtils.hasText(request.newPassword())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 이메일 정규화
    private String normalizeEmail(
            String email // 원본 이메일
    ) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 비밀번호 재설정 이메일 제목 생성
    private String buildPasswordResetEmailSubject() {
        return "[KYvC] 비밀번호 재설정 안내";
    }

    // 비밀번호 재설정 이메일 본문 생성
    private String buildPasswordResetEmailBody(
            String resetToken // 재설정 토큰 원문
    ) {
        if (passwordResetProperties.hasBaseUrl()) {
            return """
                    안녕하세요.
                    KYvC 비밀번호 재설정 요청이 접수되었습니다.

                    아래 링크에서 비밀번호를 재설정해 주세요.
                    %s

                    본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                    """.formatted(createPasswordResetLink(resetToken));
        }

        return """
                안녕하세요.
                KYvC 비밀번호 재설정 요청이 접수되었습니다.

                비밀번호 재설정 토큰: %s

                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(resetToken);
    }

    // 비밀번호 재설정 링크 생성
    private String createPasswordResetLink(
            String resetToken // 재설정 토큰 원문
    ) {
        String baseUrl = passwordResetProperties.getBaseUrl().trim();
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + delimiter + "token=" + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
    }
}

