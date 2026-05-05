package com.kyvc.backendadmin.domain.auth.application;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRepository;
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
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

// Backend Admin MFA 이메일 challenge 생성 및 인증번호 검증 서비스
/**
 * Backend Admin MFA 유스케이스를 처리하는 서비스입니다.
 *
 * <p>현재 인증된 관리자에게 이메일 MFA challenge를 발급하고, 인증번호 검증 성공 시
 * MFA_SESSION 토큰을 생성합니다. 인증번호와 MFA 토큰 원문은 저장하지 않고 해시만
 * 저장하며, 실패 횟수와 만료 상태를 트랜잭션 안에서 갱신합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMfaService {

    // 1차 구현에서 허용하는 MFA 채널
    private static final String EMAIL_CHANNEL = "EMAIL";
    // 6자리 숫자 인증번호 생성을 위한 난수 상한
    private static final int MFA_CODE_BOUND = 1_000_000;
    // MFA 인증번호 만료 시간
    private static final int MFA_EXPIRATION_MINUTES = 5;
    // MFA_SESSION 토큰 만료 시간
    private static final int MFA_SESSION_EXPIRATION_MINUTES = 10;
    // 인증번호 최대 실패 횟수
    private static final int MAX_FAILED_COUNT = 5;

    // 현재 관리자 이메일 조회 저장소
    private final AdminUserRepository adminUserRepository;
    // MFA challenge 저장/조회 저장소
    private final MfaEmailVerificationRepository mfaEmailVerificationRepository;
    // MFA_SESSION 토큰 저장소
    private final AuthTokenRepository authTokenRepository;
    // 인증번호와 MFA token 생성을 위한 보안 난수
    private final SecureRandom secureRandom = new SecureRandom();

    // 이메일 MFA challenge 생성, 인증번호 원문은 저장하지 않고 해시만 저장
    /**
     * MFA challenge를 생성합니다.
     *
     * <p>채널은 EMAIL만 허용하고, MFA 목적 enum을 검증합니다. 6자리 인증번호를 생성한 뒤
     * 해시를 mfa_email_verifications에 저장하고 발송 대상 이메일을 마스킹해 반환합니다.</p>
     *
     * @param request MFA 채널과 목적
     * @return challenge ID, 만료 시각, 마스킹된 이메일
     */
    @Transactional
    public AdminMfaChallengeResponse challenge(AdminMfaChallengeRequest request) {
        if (!EMAIL_CHANNEL.equalsIgnoreCase(request.channel())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "EMAIL 채널만 사용할 수 있습니다.");
        }

        KyvcEnums.MfaPurpose purpose = parsePurpose(request.purpose());
        Long adminId = SecurityUtil.getCurrentAdminId();
        AdminUser adminUser = adminUserRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));

        String verificationCode = String.format("%06d", secureRandom.nextInt(MFA_CODE_BOUND));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(MFA_EXPIRATION_MINUTES);
        MfaEmailVerification verification = MfaEmailVerification.create(
                adminId,
                purpose,
                adminUser.getEmail(),
                TokenHashUtil.sha256(verificationCode),
                expiresAt
        );
        mfaEmailVerificationRepository.save(verification);
        log.info("admin MFA email verification generated. adminId={} challengeId={}", adminId, verification.getChallengeId());

        return new AdminMfaChallengeResponse(
                verification.getChallengeId(),
                expiresAt,
                maskEmail(adminUser.getEmail())
        );
    }

    // challengeId와 인증번호를 검증하고 MFA_SESSION token 발급
    /**
     * MFA 인증번호를 검증합니다.
     *
     * <p>challenge 소유자, actorType, 만료 여부, 이미 사용된 상태, 실패 횟수를 검증합니다.
     * 인증번호 해시가 일치하면 challenge를 VERIFIED로 전이하고 MFA_SESSION 토큰 해시를
     * auth_tokens에 저장합니다.</p>
     *
     * @param request challenge ID와 인증번호
     * @return MFA 세션 토큰 원문과 만료 시각
     */
    @Transactional
    public AdminMfaVerifyResponse verify(AdminMfaVerifyRequest request) {
        Long adminId = SecurityUtil.getCurrentAdminId();
        MfaEmailVerification verification = mfaEmailVerificationRepository.findByChallengeId(request.challengeId())
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_NOT_FOUND));

        if (KyvcEnums.ActorType.ADMIN != verification.getActorType() || !adminId.equals(verification.getActorId())) {
            throw new ApiException(ErrorCode.MFA_NOT_FOUND);
        }
        if (verification.isVerified()) {
            throw new ApiException(ErrorCode.MFA_ALREADY_USED);
        }
        if (verification.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.MFA_EXPIRED);
        }
        if (verification.getFailedCount() >= MAX_FAILED_COUNT) {
            throw new ApiException(ErrorCode.MFA_INVALID_CODE);
        }
        if (!TokenHashUtil.sha256(request.verificationCode()).equals(verification.getVerificationCodeHash())) {
            verification.increaseFailedCount();
            throw new ApiException(ErrorCode.MFA_INVALID_CODE);
        }

        verification.verify(LocalDateTime.now());
        String mfaToken = randomToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(MFA_SESSION_EXPIRATION_MINUTES);
        authTokenRepository.save(AuthToken.create(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                KyvcEnums.TokenType.MFA_SESSION,
                TokenHashUtil.sha256(mfaToken),
                expiresAt
        ));
        return new AdminMfaVerifyResponse(mfaToken, expiresAt);
    }

    // 요청 문자열을 MFA 목적 enum으로 변환
    private KyvcEnums.MfaPurpose parsePurpose(String purpose) {
        try {
            return KyvcEnums.MfaPurpose.valueOf(purpose);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 MFA 목적입니다.");
        }
    }

    // 응답에 노출할 이메일 주소 마스킹
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(Math.max(atIndex, 0));
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    // DB에는 해시만 저장할 MFA_SESSION 원문 토큰 생성
    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
