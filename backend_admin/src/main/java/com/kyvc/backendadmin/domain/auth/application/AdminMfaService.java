package com.kyvc.backendadmin.domain.auth.application;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRepository;
import com.kyvc.backendadmin.domain.auth.config.AdminMfaProperties;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.domain.MfaEmailVerification;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaChallengeRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaChallengeResponse;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaVerifyRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminMfaVerifyResponse;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.auth.repository.MfaEmailVerificationRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.JwtTokenProvider;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.logging.LogEventLogger;
import com.kyvc.backendadmin.global.mail.EmailSender;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

/**
 * Backend Admin MFA 서비스입니다.
 *
 * <p>현재 인증된 관리자에게 이메일 MFA challenge를 생성하고, 인증번호 검증 성공 시 MFA_SESSION
 * 토큰을 발급합니다. 인증번호와 MFA 토큰 원문은 평문으로 저장하지 않고 해시만 저장합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminMfaService {

    private static final String EMAIL_CHANNEL = "EMAIL";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String ADMIN_ACTOR_TYPE = "ADMIN";

    private final AdminUserRepository adminUserRepository;
    private final MfaEmailVerificationRepository mfaEmailVerificationRepository;
    private final AuthTokenRepository authTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailSender emailSender;
    private final AdminMfaProperties mfaProperties;
    private final LogEventLogger logEventLogger;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 현재 인증된 관리자에게 이메일 MFA challenge를 생성합니다.
     *
     * @param request MFA 채널과 목적을 담은 요청
     * @return challenge ID, 만료 시각, 마스킹된 이메일 대상
     */
    @Transactional
    public AdminMfaChallengeResponse challenge(
            AdminMfaChallengeRequest request,
            String accessToken
    ) {
        validateChallengeRequest(request);
        if (!EMAIL_CHANNEL.equalsIgnoreCase(request.channel().trim())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "EMAIL channel is required.");
        }

        KyvcEnums.MfaPurpose purpose = parsePurpose(request.purpose());
        Long adminId = resolveAdminId(accessToken);
        AdminUser adminUser = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));
        if (!StringUtils.hasText(adminUser.getEmail())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        int expirationMinutes = mfaProperties.resolvedExpirationMinutes();
        String verificationCode = generateVerificationCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        String normalizedEmail = normalizeEmail(adminUser.getEmail());
        MfaEmailVerification verification = MfaEmailVerification.create(
                adminId,
                purpose,
                normalizedEmail,
                TokenHashUtil.sha256(verificationCode),
                expiresAt
        );
        mfaEmailVerificationRepository.save(verification);
        emailSender.send(
                normalizedEmail,
                buildMfaEmailSubject(),
                buildMfaEmailBody(verificationCode, expirationMinutes)
        );
        logEventLogger.info(
                "ADMIN_MFA_CHALLENGE_CREATED",
                "admin MFA challenge created",
                Map.of(
                        "adminId", adminId,
                        "challengeId", verification.getChallengeId(),
                        "target", maskEmail(normalizedEmail)
                )
        );

        return new AdminMfaChallengeResponse(
                verification.getChallengeId(),
                expiresAt,
                maskEmail(normalizedEmail)
        );
    }

    /**
     * 이메일 MFA 인증번호를 검증하고 MFA_SESSION 토큰을 발급합니다.
     *
     * @param request challenge ID와 인증번호를 담은 요청
     * @return MFA 세션 토큰과 만료 시각
     */
    @Transactional
    public AdminMfaVerifyResponse verify(
            AdminMfaVerifyRequest request,
            String accessToken
    ) {
        validateVerifyRequest(request);
        Long adminId = resolveAdminId(accessToken);
        MfaEmailVerification verification = mfaEmailVerificationRepository.findByChallengeId(request.challengeId().trim())
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_NOT_FOUND));

        if (KyvcEnums.ActorType.ADMIN != verification.getActorType() || !adminId.equals(verification.getActorId())) {
            throw new ApiException(ErrorCode.MFA_NOT_FOUND);
        }
        if (verification.isVerified()) {
            throw new ApiException(ErrorCode.MFA_ALREADY_USED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (verification.isExpired(now)) {
            verification.markExpired();
            throw new ApiException(ErrorCode.MFA_EXPIRED);
        }

        int maxFailedAttempts = mfaProperties.resolvedMaxFailedAttempts();
        if (verification.getFailedCount() >= maxFailedAttempts) {
            throw new ApiException(ErrorCode.MFA_INVALID_CODE);
        }
        if (!TokenHashUtil.sha256(request.verificationCode().trim()).equals(verification.getVerificationCodeHash())) {
            verification.increaseFailedCount(maxFailedAttempts);
            throw new ApiException(ErrorCode.MFA_INVALID_CODE);
        }

        verification.verify(now);
        String mfaToken = randomToken();
        LocalDateTime expiresAt = now.plusMinutes(mfaProperties.resolvedSessionExpirationMinutes());
        authTokenRepository.save(AuthToken.create(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                KyvcEnums.TokenType.MFA_SESSION,
                TokenHashUtil.sha256(mfaToken),
                expiresAt
        ));
        logEventLogger.info(
                "ADMIN_MFA_VERIFIED",
                "admin MFA verified",
                Map.of(
                        "adminId", adminId,
                        "challengeId", verification.getChallengeId()
                )
        );
        return new AdminMfaVerifyResponse(mfaToken, expiresAt);
    }

    private void validateChallengeRequest(AdminMfaChallengeRequest request) {
        if (request == null || !StringUtils.hasText(request.channel()) || !StringUtils.hasText(request.purpose())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateVerifyRequest(AdminMfaVerifyRequest request) {
        if (request == null
                || !StringUtils.hasText(request.challengeId())
                || !StringUtils.hasText(request.verificationCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private KyvcEnums.MfaPurpose parsePurpose(String purpose) {
        try {
            return KyvcEnums.MfaPurpose.valueOf(purpose.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "Invalid MFA purpose.");
        }
    }

    private Long resolveAdminId(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "MFA admin context is required.");
        }
        jwtTokenProvider.validateToken(accessToken);
        if (!ACCESS_TOKEN_TYPE.equals(jwtTokenProvider.getTokenType(accessToken))) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN_TYPE);
        }
        if (!ADMIN_ACTOR_TYPE.equals(jwtTokenProvider.getActorType(accessToken))) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        return jwtTokenProvider.getUserId(accessToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateVerificationCode() {
        int codeLength = mfaProperties.resolvedCodeLength();
        StringBuilder builder = new StringBuilder(codeLength);
        for (int index = 0; index < codeLength; index++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

    private String buildMfaEmailSubject() {
        return "[KYvC Admin] MFA verification code";
    }

    private String buildMfaEmailBody(
            String verificationCode,
            int expirationMinutes
    ) {
        return """
                Hello,
                Your KYvC Admin MFA verification code is below.

                Verification code: %s
                Expires in: %d minutes

                If you did not request this code, ignore this email.
                """.formatted(verificationCode, expirationMinutes);
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() == 1) {
            return localPart + "***" + domainPart;
        }
        return localPart.substring(0, 1) + "***" + domainPart;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
