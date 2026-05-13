package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// MFA 이메일 인증 JPA Repository
public interface MfaEmailVerificationJpaRepository extends JpaRepository<MfaEmailVerification, Long> {

    List<MfaEmailVerification> findAllByActorTypeCodeAndEmailAndMfaPurposeCodeAndMfaStatusCodeAndExpiresAtAfterOrderByRequestedAtDesc(
            KyvcEnums.ActorType actorTypeCode, // 행위자 유형
            String email, // 정규화 이메일
            KyvcEnums.MfaPurpose purpose, // 인증 목적
            KyvcEnums.MfaStatus status, // 인증 상태
            LocalDateTime now // 기준 일시
    );
}
