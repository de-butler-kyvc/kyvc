package com.kyvc.backendadmin.domain.verifier.application;

import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Verifier 중요 작업에서 사용하는 MFA 검증 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class AdminVerifierSecuritySupport {

    private final AuthTokenRepository authTokenRepository;

    /**
     * 관리자 MFA 세션 토큰을 검증하고 사용 완료 처리합니다.
     *
     * @param rawMfaToken MFA 세션 토큰 원문
     * @param adminId 관리자 ID
     */
    public void validateAndUseMfa(String rawMfaToken, Long adminId) {
        AuthToken token = authTokenRepository.findByTokenHashAndTokenType(
                        TokenHashUtil.sha256(rawMfaToken),
                        KyvcEnums.TokenType.MFA_SESSION
                )
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_TOKEN_INVALID));
        if (KyvcEnums.ActorType.ADMIN != token.getActorType() || !adminId.equals(token.getActorId())) {
            throw new ApiException(ErrorCode.MFA_TOKEN_INVALID);
        }
        if (!token.isActive() || token.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.MFA_TOKEN_INVALID);
        }
        token.markUsed(LocalDateTime.now());
    }
}
