package com.kyvc.backendadmin.domain.auth.repository;

import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// EntityManager 기반 인증 토큰 Repository 구현체
/**
 * {@link AuthTokenRepository}의 EntityManager 기반 구현체입니다.
 *
 * <p>auth_tokens 테이블에 토큰 해시를 저장하고, 토큰 타입별 조회와 활성 토큰의
 * REVOKED 상태 전이를 담당합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class AuthTokenRepositoryImpl implements AuthTokenRepository {

    // 테스트에서 JPA 자동 구성이 제외되어도 컨텍스트가 뜨도록 지연 조회
    private final ObjectProvider<EntityManager> entityManagerProvider;

    // auth_tokens 신규 레코드 저장
    @Override
    public AuthToken save(AuthToken authToken) {
        entityManager().persist(authToken);
        return authToken;
    }

    // 원문 토큰이 아닌 해시 값과 토큰 용도로 토큰 조회
    @Override
    public Optional<AuthToken> findByTokenHashAndTokenType(String tokenHash, KyvcEnums.TokenType tokenType) {
        if (tokenHash == null || tokenHash.isBlank() || tokenType == null) {
            return Optional.empty();
        }

        List<AuthToken> result = entityManager()
                .createQuery("""
                        select authToken
                        from AuthToken authToken
                        where authToken.tokenHash = :tokenHash
                          and authToken.tokenType = :tokenType
                        """, AuthToken.class)
                .setParameter("tokenHash", tokenHash)
                .setParameter("tokenType", tokenType)
                .setMaxResults(1)
                .getResultList();
        return result.stream().findFirst();
    }

    // 비밀번호 변경/재설정 시 기존 활성 refresh token 등을 일괄 폐기
    @Override
    public void revokeActiveTokens(KyvcEnums.ActorType actorType, Long actorId, KyvcEnums.TokenType tokenType) {
        entityManager()
                .createQuery("""
                        update AuthToken authToken
                        set authToken.status = :revoked,
                            authToken.revokedAt = :revokedAt
                        where authToken.actorType = :actorType
                          and authToken.actorId = :actorId
                          and authToken.tokenType = :tokenType
                          and authToken.status = :active
                        """)
                .setParameter("revoked", KyvcEnums.TokenStatus.REVOKED)
                .setParameter("revokedAt", LocalDateTime.now())
                .setParameter("actorType", actorType)
                .setParameter("actorId", actorId)
                .setParameter("tokenType", tokenType)
                .setParameter("active", KyvcEnums.TokenStatus.ACTIVE)
                .executeUpdate();
    }

    @Override
    public void revokeActiveTokens(KyvcEnums.ActorType actorType, Long actorId) {
        entityManager()
                .createQuery("""
                        update AuthToken authToken
                        set authToken.status = :revoked,
                            authToken.revokedAt = :revokedAt
                        where authToken.actorType = :actorType
                          and authToken.actorId = :actorId
                          and authToken.status = :active
                        """)
                .setParameter("revoked", KyvcEnums.TokenStatus.REVOKED)
                .setParameter("revokedAt", LocalDateTime.now())
                .setParameter("actorType", actorType)
                .setParameter("actorId", actorId)
                .setParameter("active", KyvcEnums.TokenStatus.ACTIVE)
                .executeUpdate();
    }

    // 실제 DB 접근 시점에 EntityManager를 가져옴
    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
