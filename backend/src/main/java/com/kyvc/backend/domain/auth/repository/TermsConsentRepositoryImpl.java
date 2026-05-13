package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.TermsConsent;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// 약관 동의 Repository 구현체
@Repository
@RequiredArgsConstructor
public class TermsConsentRepositoryImpl implements TermsConsentRepository {

    private final EntityManager entityManager;

    // 사용자 기준 약관 동의 목록 조회
    @Override
    public List<TermsConsent> findByUserId(
            Long userId // 사용자 ID
    ) {
        return entityManager.createQuery("""
                        select termsConsent
                        from TermsConsent termsConsent
                        where termsConsent.userId = :userId
                        order by termsConsent.termsCode asc, termsConsent.termsVersion asc, termsConsent.id asc
                        """, TermsConsent.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    // 사용자 기준 약관 동의 단건 조회
    @Override
    public Optional<TermsConsent> findByUserIdAndTermsCodeAndTermsVersion(
            Long userId, // 사용자 ID
            String termsCode, // 약관 코드
            String termsVersion // 약관 버전
    ) {
        return entityManager.createQuery("""
                        select termsConsent
                        from TermsConsent termsConsent
                        where termsConsent.userId = :userId
                          and termsConsent.termsCode = :termsCode
                          and termsConsent.termsVersion = :termsVersion
                        """, TermsConsent.class)
                .setParameter("userId", userId)
                .setParameter("termsCode", termsCode)
                .setParameter("termsVersion", termsVersion)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    // 약관 동의 저장
    @Override
    public TermsConsent save(
            TermsConsent termsConsent // 저장 대상 약관 동의
    ) {
        if (termsConsent.getId() == null) {
            entityManager.persist(termsConsent);
            return termsConsent;
        }

        return entityManager.merge(termsConsent);
    }
}
