package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;
import com.kyvc.backend.global.util.KyvcEnums;

import java.time.LocalDateTime;
import java.util.Optional;

// MFA 이메일 인증 Repository
public interface MfaEmailVerificationRepository {

    Optional<MfaEmailVerification> findById(
            Long mfaVerificationId // MFA 인증 ID
    );

    Optional<MfaEmailVerification> findActiveSignupRequestByEmailAndPurpose(
            String email, // 정규화 이메일
            KyvcEnums.MfaPurpose purpose, // 인증 목적
            LocalDateTime now // 기준 일시
    );

    MfaEmailVerification save(
            MfaEmailVerification verification // 저장 대상 MFA 인증
    );
}
