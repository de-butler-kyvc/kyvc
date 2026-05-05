package com.kyvc.backendadmin.domain.auth.repository;

import com.kyvc.backendadmin.domain.auth.domain.MfaEmailVerification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// EntityManager 기반 이메일 MFA challenge Repository 구현체
/**
 * {@link MfaEmailVerificationRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>mfa_email_verifications 테이블에 challenge를 저장하고, challengeId로 단건 조회합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class MfaEmailVerificationRepositoryImpl implements MfaEmailVerificationRepository {

    // 테스트에서 JPA 자동 구성이 제외되어도 컨텍스트가 뜨도록 지연 조회
    private final ObjectProvider<EntityManager> entityManagerProvider;

    // mfa_email_verifications 신규 레코드 저장
    @Override
    public MfaEmailVerification save(MfaEmailVerification verification) {
        entityManager().persist(verification);
        return verification;
    }

    // 클라이언트가 전달한 challengeId로 challenge 단건 조회
    @Override
    public Optional<MfaEmailVerification> findByChallengeId(String challengeId) {
        try {
            return Optional.ofNullable(entityManager().find(MfaEmailVerification.class, Long.parseLong(challengeId)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    // 실제 DB 접근 시점에 EntityManager를 가져옴
    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
