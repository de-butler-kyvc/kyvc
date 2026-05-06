package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;

import java.util.Optional;

// MFA 이메일 인증 Repository
public interface MfaEmailVerificationRepository {

    Optional<MfaEmailVerification> findById(
            Long mfaVerificationId // MFA 인증 ID
    );

    MfaEmailVerification save(
            MfaEmailVerification verification // 저장 대상 MFA 인증
    );
}
