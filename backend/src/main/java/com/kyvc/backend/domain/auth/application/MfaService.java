package com.kyvc.backend.domain.auth.application;

import com.kyvc.backend.domain.auth.config.MfaProperties;
import com.kyvc.backend.domain.auth.domain.AuthToken;
import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;
import com.kyvc.backend.domain.auth.dto.MfaChallengeRequest;
import com.kyvc.backend.domain.auth.dto.MfaChallengeResponse;
import com.kyvc.backend.domain.auth.dto.MfaVerifyRequest;
import com.kyvc.backend.domain.auth.dto.MfaVerifyResponse;
import com.kyvc.backend.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backend.domain.auth.repository.MfaEmailVerificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.mail.EmailSender;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// MFA 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class MfaService {

    private static final String EMAIL_CHANNEL = "EMAIL"; // 허용 MFA 채널

    private final MfaEmailVerificationRepository mfaEmailVerificationRepository;
    private final AuthTokenRepository authTokenRepository;
    private final EmailSender emailSender;
    private final MfaProperties mfaProperties;

    // MFA challenge 생성
    public MfaChallengeResponse createChallenge(
            Long userId, // 사용자 ID
            String email, // 사용자 이메일
            MfaChallengeRequest request // MFA challenge 생성 요청
    ) {
        validateUser(userId, email);
        validateChallengeRequest(request);

        KyvcEnums.MfaPurpose purpose = parsePurpose(request.purpose());
        LocalDateTime now = LocalDateTime.now();
        int expirationMinutes = mfaProperties.resolvedExpirationMinutes(); // 인증코드 만료시간 분
        LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);
        String verificationCode = generateVerificationCode();

        MfaEmailVerification verification = MfaEmailVerification.create(
                userId,
                normalizeEmail(email),
                purpose,
                TokenHashUtil.sha256(verificationCode),
                now,
                expiresAt
        );
        MfaEmailVerification savedVerification = mfaEmailVerificationRepository.save(verification);

        emailSender.send(
                savedVerification.getEmail(),
                buildMfaEmailSubject(),
                buildMfaEmailBody(verificationCode, expirationMinutes)
        );

        return new MfaChallengeResponse(
                String.valueOf(savedVerification.getMfaVerificationId()),
                expiresAt,
                maskEmail(savedVerification.getEmail())
        );
    }

    // MFA challenge 검증
    public MfaVerifyResponse verify(
            Long userId, // 사용자 ID
            MfaVerifyRequest request // MFA 검증 요청
    ) {
        validateUserId(userId);
        validateVerifyRequest(request);

        Long challengeId = parseChallengeId(request.challengeId());
        MfaEmailVerification verification = mfaEmailVerificationRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_CHALLENGE_NOT_FOUND));
        if (!verification.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (verification.getMfaStatusCode() == KyvcEnums.MfaStatus.EXPIRED) {
            throw new ApiException(ErrorCode.MFA_CODE_EXPIRED);
        }
        if (verification.getMfaStatusCode() == KyvcEnums.MfaStatus.FAILED) {
            throw new ApiException(ErrorCode.MFA_CODE_ATTEMPT_EXCEEDED);
        }
        if (!verification.isRequested()) {
            throw new ApiException(ErrorCode.INVALID_STATUS);
        }
        if (verification.isExpired(now)) {
            verification.markExpired();
            mfaEmailVerificationRepository.save(verification);
            throw new ApiException(ErrorCode.MFA_CODE_EXPIRED);
        }

        String verificationCodeHash = TokenHashUtil.sha256(request.verificationCode().trim());
        int maxFailedAttempts = mfaProperties.resolvedMaxFailedAttempts(); // 최대 실패 횟수
        if (!verification.matchesCodeHash(verificationCodeHash)) {
            verification.increaseFailure(maxFailedAttempts);
            mfaEmailVerificationRepository.save(verification);
            if (verification.isAttemptExceeded(maxFailedAttempts)) {
                throw new ApiException(ErrorCode.MFA_CODE_ATTEMPT_EXCEEDED);
            }
            throw new ApiException(ErrorCode.MFA_CODE_INVALID);
        }

        verification.markVerified(now);
        mfaEmailVerificationRepository.save(verification);

        String mfaSessionToken = UUID.randomUUID().toString();
        AuthToken authToken = AuthToken.createUserToken(
                userId,
                KyvcEnums.TokenType.MFA_SESSION,
                TokenHashUtil.sha256(mfaSessionToken),
                null,
                now,
                now.plusMinutes(mfaProperties.resolvedSessionExpirationMinutes())
        );
        authTokenRepository.save(authToken);

        return new MfaVerifyResponse(true, mfaSessionToken);
    }

    // 사용자 정보 검증
    private void validateUser(
            Long userId, // 사용자 ID
            String email // 사용자 이메일
    ) {
        validateUserId(userId);
        if (!StringUtils.hasText(email)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // MFA challenge 요청 검증
    private void validateChallengeRequest(
            MfaChallengeRequest request // MFA challenge 생성 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.channel())
                || !StringUtils.hasText(request.purpose())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!EMAIL_CHANNEL.equals(request.channel().trim().toUpperCase(Locale.ROOT))) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // MFA 검증 요청 검증
    private void validateVerifyRequest(
            MfaVerifyRequest request // MFA 검증 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.challengeId())
                || !StringUtils.hasText(request.verificationCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // challenge ID 파싱
    private Long parseChallengeId(
            String challengeId // challenge ID 문자열
    ) {
        try {
            return Long.parseLong(challengeId.trim());
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // MFA 목적 코드 파싱
    private KyvcEnums.MfaPurpose parsePurpose(
            String purpose // MFA 목적 코드
    ) {
        try {
            return KyvcEnums.MfaPurpose.valueOf(purpose.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 이메일 정규화
    private String normalizeEmail(
            String email // 원본 이메일
    ) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 인증번호 생성
    private String generateVerificationCode() {
        int codeLength = mfaProperties.resolvedCodeLength(); // 인증코드 길이
        StringBuilder builder = new StringBuilder(codeLength); // 인증코드 버퍼
        for (int index = 0; index < codeLength; index++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }

    // MFA 이메일 제목 생성
    private String buildMfaEmailSubject() {
        return "[KYvC] MFA 인증번호 안내";
    }

    // MFA 이메일 본문 생성
    private String buildMfaEmailBody(
            String verificationCode, // 인증번호 원문
            int expirationMinutes // 인증코드 만료시간 분
    ) {
        return """
                안녕하세요.
                KYvC 인증번호는 아래와 같습니다.

                인증번호: %s
                유효시간: %d분

                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(verificationCode, expirationMinutes);
    }

    // 이메일 마스킹
    private String maskEmail(
            String email // 사용자 이메일
    ) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        return localPart.substring(0, 1) + "***" + domainPart;
    }
}

