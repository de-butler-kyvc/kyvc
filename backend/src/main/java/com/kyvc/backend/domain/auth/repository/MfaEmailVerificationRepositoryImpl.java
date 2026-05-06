package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// MFA 이메일 인증 Repository 구현체
@Repository
@RequiredArgsConstructor
public class MfaEmailVerificationRepositoryImpl implements MfaEmailVerificationRepository {

    private final MfaEmailVerificationJpaRepository mfaEmailVerificationJpaRepository;

    @Override
    public Optional<MfaEmailVerification> findById(
            Long mfaVerificationId // MFA 인증 ID
    ) {
        return mfaEmailVerificationJpaRepository.findById(mfaVerificationId);
    }

    @Override
    public MfaEmailVerification save(
            MfaEmailVerification verification // 저장 대상 MFA 인증
    ) {
        return mfaEmailVerificationJpaRepository.save(verification);
    }
}
