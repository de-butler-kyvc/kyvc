package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.TermsConsent;

import java.util.List;
import java.util.Optional;

/**
 * 약관 동의 Repository
 */
public interface TermsConsentRepository {

    /**
     * 사용자 기준 약관 동의 목록 조회
     *
     * @param userId 사용자 ID
     * @return 약관 동의 목록
     */
    List<TermsConsent> findByUserId(
            Long userId // 사용자 ID
    );

    /**
     * 사용자 기준 약관 동의 단건 조회
     *
     * @param userId 사용자 ID
     * @param termsCode 약관 코드
     * @param termsVersion 약관 버전
     * @return 약관 동의 조회 결과
     */
    Optional<TermsConsent> findByUserIdAndTermsCodeAndTermsVersion(
            Long userId, // 사용자 ID
            String termsCode, // 약관 코드
            String termsVersion // 약관 버전
    );

    /**
     * 약관 동의 저장
     *
     * @param termsConsent 저장 대상 약관 동의
     * @return 저장 결과
     */
    TermsConsent save(
            TermsConsent termsConsent // 저장 대상 약관 동의
    );
}
