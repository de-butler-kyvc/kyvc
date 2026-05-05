package com.kyvc.backendadmin.domain.auth.repository;

import com.kyvc.backendadmin.domain.auth.domain.MfaEmailVerification;

import java.util.Optional;

// 이메일 MFA challenge Repository 계약
/**
 * 이메일 MFA challenge Repository입니다.
 *
 * <p>mfa_email_verifications 테이블에 대한 challenge 저장과 challengeId 기반 조회를 담당합니다.</p>
 */
public interface MfaEmailVerificationRepository {

    // 신규 MFA challenge 저장
    /**
     * 신규 MFA 이메일 challenge를 저장합니다.
     *
     * @param verification 저장할 MFA 이메일 인증 엔티티
     * @return 저장된 MFA 이메일 인증 엔티티
     */
    MfaEmailVerification save(MfaEmailVerification verification);

    // challengeId로 MFA challenge 조회
    /**
     * challengeId로 MFA 이메일 challenge를 조회합니다.
     *
     * @param challengeId 조회할 MFA challenge 식별자
     * @return MFA 이메일 인증 엔티티 Optional
     */
    Optional<MfaEmailVerification> findByChallengeId(String challengeId);
}
