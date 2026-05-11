package com.kyvc.backend.domain.auth.application;

import com.kyvc.backend.domain.auth.config.MfaProperties;
import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;
import com.kyvc.backend.domain.auth.dto.EmailVerificationRequest;
import com.kyvc.backend.domain.auth.dto.EmailVerificationRequestResponse;
import com.kyvc.backend.domain.auth.dto.EmailVerificationVerifyRequest;
import com.kyvc.backend.domain.auth.dto.EmailVerificationVerifyResponse;
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
import java.util.concurrent.ThreadLocalRandom;

// 회원가입 이메일 인증 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class EmailVerificationService {

    private final MfaEmailVerificationRepository mfaEmailVerificationRepository;
    private final MfaProperties mfaProperties;
    private final EmailSender emailSender;

    // 이메일 인증번호 발송
    public EmailVerificationRequestResponse request(
            EmailVerificationRequest request // 이메일 인증번호 발송 요청
    ) {
        validateRequest(request);
        String email = normalizeEmail(request.email());
        KyvcEnums.MfaPurpose purpose = resolveSignupPurpose(request.purpose());
        LocalDateTime now = LocalDateTime.now();

        mfaEmailVerificationRepository.findActiveSignupRequestByEmailAndPurpose(email, purpose, now)
                .ifPresent(existing -> {
                    throw new ApiException(ErrorCode.EMAIL_VERIFICATION_RESEND_TOO_FAST);
                });

        int expirationMinutes = mfaProperties.resolvedExpirationMinutes();
        LocalDateTime expiresAt = now.plusMinutes(expirationMinutes);
        String verificationCode = generateVerificationCode();
        MfaEmailVerification verification = MfaEmailVerification.createSignupEmailVerification(
                email,
                purpose,
                TokenHashUtil.sha256(verificationCode),
                now,
                expiresAt
        );
        MfaEmailVerification saved = mfaEmailVerificationRepository.save(verification);

        emailSender.send(
                saved.getEmail(),
                buildEmailSubject(),
                buildEmailBody(verificationCode, expirationMinutes)
        );

        return new EmailVerificationRequestResponse(
                saved.getMfaVerificationId(),
                maskEmail(saved.getEmail()),
                saved.getExpiresAt(),
                true
        );
    }

    // 이메일 인증번호 검증
    public EmailVerificationVerifyResponse verify(
            EmailVerificationVerifyRequest request // 이메일 인증번호 검증 요청
    ) {
        validateVerifyRequest(request);
        String email = normalizeEmail(request.email());
        MfaEmailVerification verification = mfaEmailVerificationRepository.findById(request.verificationId())
                .orElseThrow(() -> new ApiException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (!email.equals(verification.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_EMAIL_MISMATCH);
        }
        if (!verification.isSignupVerification()) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_PURPOSE_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        validateVerificationStatus(verification, now);

        String verificationCodeHash = TokenHashUtil.sha256(request.verificationCode().trim());
        int maxFailedAttempts = mfaProperties.resolvedMaxFailedAttempts();
        if (!verification.matchesCodeHash(verificationCodeHash)) {
            verification.increaseFailure(maxFailedAttempts);
            mfaEmailVerificationRepository.save(verification);
            if (verification.isAttemptExceeded(maxFailedAttempts)) {
                throw new ApiException(ErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED);
            }
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_CODE_INVALID);
        }

        verification.markVerified(now);
        mfaEmailVerificationRepository.save(verification);
        return new EmailVerificationVerifyResponse(true, email);
    }

    // 인증 상태 검증
    private void validateVerificationStatus(
            MfaEmailVerification verification, // 이메일 인증 요청
            LocalDateTime now // 기준 일시
    ) {
        if (verification.getMfaStatusCode() == KyvcEnums.MfaStatus.VERIFIED) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_ALREADY_VERIFIED);
        }
        if (verification.getMfaStatusCode() == KyvcEnums.MfaStatus.EXPIRED) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }
        if (verification.getMfaStatusCode() == KyvcEnums.MfaStatus.FAILED) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_ATTEMPT_EXCEEDED);
        }
        if (!verification.isRequested()) {
            throw new ApiException(ErrorCode.INVALID_STATUS);
        }
        if (verification.isExpired(now)) {
            verification.markExpired();
            mfaEmailVerificationRepository.save(verification);
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }
    }

    // 발송 요청 검증
    private void validateRequest(
            EmailVerificationRequest request // 이메일 인증번호 발송 요청
    ) {
        if (request == null || !StringUtils.hasText(request.email())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 검증 요청 검증
    private void validateVerifyRequest(
            EmailVerificationVerifyRequest request // 이메일 인증번호 검증 요청
    ) {
        if (request == null
                || request.verificationId() == null
                || !StringUtils.hasText(request.email())
                || !StringUtils.hasText(request.verificationCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 회원가입 인증 목적 산정
    private KyvcEnums.MfaPurpose resolveSignupPurpose(
            String purpose // 요청 인증 목적
    ) {
        if (!StringUtils.hasText(purpose)) {
            return KyvcEnums.MfaPurpose.SIGNUP;
        }
        try {
            KyvcEnums.MfaPurpose resolved = KyvcEnums.MfaPurpose.valueOf(purpose.trim().toUpperCase(Locale.ROOT));
            if (resolved == KyvcEnums.MfaPurpose.SIGNUP
                    || resolved == KyvcEnums.MfaPurpose.SIGNUP_EMAIL_VERIFICATION) {
                return resolved;
            }
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_PURPOSE_INVALID);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.EMAIL_VERIFICATION_PURPOSE_INVALID);
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
        int codeLength = mfaProperties.resolvedCodeLength();
        StringBuilder builder = new StringBuilder(codeLength);
        for (int index = 0; index < codeLength; index++) {
            builder.append(ThreadLocalRandom.current().nextInt(10));
        }
        return builder.toString();
    }

    // 이메일 제목 생성
    private String buildEmailSubject() {
        return "[KYvC] 회원가입 이메일 인증번호 안내";
    }

    // 이메일 본문 생성
    private String buildEmailBody(
            String verificationCode, // 인증번호 원문
            int expirationMinutes // 만료 시간
    ) {
        return """
                안녕하세요.
                KYvC 회원가입 이메일 인증번호는 아래와 같습니다.

                인증번호: %s
                유효시간: %d분

                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(verificationCode, expirationMinutes);
    }

    // 이메일 마스킹
    private String maskEmail(
            String email // 원본 이메일
    ) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        String localPart = email.substring(0, atIndex);
        return localPart.substring(0, 1) + "***" + email.substring(atIndex);
    }
}
