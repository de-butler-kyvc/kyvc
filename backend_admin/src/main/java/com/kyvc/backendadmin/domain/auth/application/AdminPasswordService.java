package com.kyvc.backendadmin.domain.auth.application;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRepository;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.dto.AdminPasswordChangeRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminPasswordResetConfirmRequest;
import com.kyvc.backendadmin.domain.auth.dto.AdminPasswordResetRequest;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

// Backend Admin 비밀번호 재설정 및 내 비밀번호 변경 서비스
/**
 * Backend Admin 비밀번호 유스케이스를 처리하는 서비스입니다.
 *
 * <p>비밀번호 재설정 요청, 재설정 확정, 현재 관리자 비밀번호 변경을 담당합니다.
 * 비밀번호는 BCrypt로 저장하고, reset token 원문은 저장하지 않으며, 비밀번호 변경 이후
 * 기존 Refresh Token을 폐기하는 트랜잭션 흐름을 수행합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordService {

    // 비밀번호 재설정 토큰 원문 생성에 사용할 바이트 길이
    private static final int RESET_TOKEN_BYTES = 32;
    // 비밀번호 재설정 토큰 만료 시간
    private static final int PASSWORD_RESET_EXPIRATION_MINUTES = 30;

    // 관리자 계정 조회 저장소
    private final AdminUserRepository adminUserRepository;
    // password reset token과 refresh token 상태 변경 저장소
    private final AuthTokenRepository authTokenRepository;
    // BCrypt 비밀번호 인코더
    private final PasswordEncoder passwordEncoder;
    // reset token 생성을 위한 보안 난수
    private final SecureRandom secureRandom = new SecureRandom();

    // 비밀번호 재설정 요청, 이메일이 없어도 동일 성공 응답을 위해 예외를 던지지 않음
    /**
     * 비밀번호 재설정 요청을 처리합니다.
     *
     * <p>이메일이 존재하지 않아도 동일한 성공 응답이 가능하도록 예외를 던지지 않습니다.
     * 활성 관리자 이메일이 존재하면 reset token을 생성하고 해시만 auth_tokens에 저장합니다.</p>
     *
     * @param request 비밀번호 재설정 대상 이메일
     */
    @Transactional
    public void requestPasswordReset(AdminPasswordResetRequest request) {
        adminUserRepository.findByEmail(request.email())
                .filter(AdminUser::isActive)
                .ifPresent(this::createPasswordResetToken);
    }

    // 재설정 토큰 검증 후 새 비밀번호 저장 및 기존 refresh token 폐기
    /**
     * 비밀번호 재설정을 확정합니다.
     *
     * <p>reset token 해시를 조회하고 ACTIVE 상태와 만료 여부를 검증합니다.
     * 새 비밀번호 정책을 확인한 뒤 BCrypt 해시로 저장하고 reset token을 USED로 전이하며,
     * 기존 Refresh Token을 모두 폐기합니다.</p>
     *
     * @param request reset token 원문과 새 비밀번호
     */
    @Transactional
    public void confirmPasswordReset(AdminPasswordResetConfirmRequest request) {
        validatePasswordPolicy(request.newPassword());
        AuthToken resetToken = authTokenRepository.findByTokenHashAndTokenType(
                        TokenHashUtil.sha256(request.resetToken()),
                        KyvcEnums.TokenType.PASSWORD_RESET
                )
                .orElseThrow(() -> new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_NOT_FOUND));
        validateResetToken(resetToken);

        AdminUser adminUser = adminUserRepository.findById(resetToken.getActorId())
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));
        adminUser.changePassword(passwordEncoder.encode(request.newPassword()));
        resetToken.markUsed(LocalDateTime.now());
        authTokenRepository.revokeActiveTokens(KyvcEnums.ActorType.ADMIN, adminUser.getAdminId(), KyvcEnums.TokenType.REFRESH);
    }

    // 현재 비밀번호 검증 후 새 비밀번호 저장 및 기존 refresh token 폐기
    /**
     * 현재 인증된 관리자의 비밀번호를 변경합니다.
     *
     * <p>SecurityContext의 관리자 ID로 계정을 조회하고 현재 비밀번호를 BCrypt로 검증합니다.
     * 새 비밀번호 정책을 확인한 뒤 저장하며, 보안을 위해 기존 Refresh Token을 폐기합니다.</p>
     *
     * @param request 현재 비밀번호와 새 비밀번호
     */
    @Transactional
    public void changePassword(AdminPasswordChangeRequest request) {
        validatePasswordPolicy(request.newPassword());
        AdminUser adminUser = adminUserRepository.findById(SecurityUtil.getCurrentAdminId())
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), adminUser.getPasswordHash())) {
            throw new ApiException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        adminUser.changePassword(passwordEncoder.encode(request.newPassword()));
        authTokenRepository.revokeActiveTokens(KyvcEnums.ActorType.ADMIN, adminUser.getAdminId(), KyvcEnums.TokenType.REFRESH);
    }

    // password reset token 원문을 생성하고 해시만 DB에 저장
    private void createPasswordResetToken(AdminUser adminUser) {
        String resetToken = randomToken(RESET_TOKEN_BYTES);
        authTokenRepository.save(AuthToken.create(
                KyvcEnums.ActorType.ADMIN,
                adminUser.getAdminId(),
                KyvcEnums.TokenType.PASSWORD_RESET,
                TokenHashUtil.sha256(resetToken),
                LocalDateTime.now().plusMinutes(PASSWORD_RESET_EXPIRATION_MINUTES)
        ));
        log.info("admin password reset token generated. adminId={}", adminUser.getAdminId());
    }

    // password reset token 활성 상태와 만료 여부 검증
    private void validateResetToken(AuthToken resetToken) {
        if (!resetToken.isActive()) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        if (resetToken.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }
    }

    // 1차 비밀번호 정책 검증
    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "비밀번호는 8자 이상이어야 합니다.");
        }
    }

    // URL-safe random token 원문 생성
    private String randomToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
