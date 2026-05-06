package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.MfaEmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

// MFA 이메일 인증 JPA Repository
public interface MfaEmailVerificationJpaRepository extends JpaRepository<MfaEmailVerification, Long> {
}
