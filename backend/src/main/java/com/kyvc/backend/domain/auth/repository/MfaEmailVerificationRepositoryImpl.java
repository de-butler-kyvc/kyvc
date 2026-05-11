package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    public Optional<MfaEmailVerification> findActiveSignupRequestByEmailAndPurpose(
            String email, // 정규화 이메일
            KyvcEnums.MfaPurpose purpose, // 인증 목적
            LocalDateTime now // 기준 일시
    ) {
        return mfaEmailVerificationJpaRepository
                .findAllByActorTypeCodeAndEmailAndMfaPurposeCodeAndMfaStatusCodeAndExpiresAtAfterOrderByRequestedAtDesc(
                        KyvcEnums.ActorType.SYSTEM,
                        email,
                        purpose,
                        KyvcEnums.MfaStatus.REQUESTED,
                        now
                )
                .stream()
                .findFirst();
    }

    @Override
    public MfaEmailVerification save(
            MfaEmailVerification verification // 저장 대상 MFA 인증
    ) {
        return mfaEmailVerificationJpaRepository.save(verification);
    }
}
