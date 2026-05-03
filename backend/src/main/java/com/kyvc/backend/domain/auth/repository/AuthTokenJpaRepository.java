package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.AuthToken;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 인증 토큰 JPA Repository
 */
public interface AuthTokenJpaRepository extends JpaRepository<AuthToken, Long> {

    /**
     * 해시와 유형 기준 토큰 조회
     *
     * @param tokenHash 토큰 해시
     * @param tokenTypeCode 토큰 유형 코드
     * @return 토큰 조회 결과
     */
    Optional<AuthToken> findByTokenHashAndTokenTypeCode(
            String tokenHash, // 토큰 해시
            KyvcEnums.TokenType tokenTypeCode // 토큰 유형 코드
    );

    /**
     * 해시와 유형과 상태 기준 토큰 조회
     *
     * @param tokenHash 토큰 해시
     * @param tokenTypeCode 토큰 유형 코드
     * @param tokenStatusCode 토큰 상태 코드
     * @return 토큰 조회 결과
     */
    Optional<AuthToken> findByTokenHashAndTokenTypeCodeAndTokenStatusCode(
            String tokenHash, // 토큰 해시
            KyvcEnums.TokenType tokenTypeCode, // 토큰 유형 코드
            KyvcEnums.TokenStatus tokenStatusCode // 토큰 상태 코드
    );
}
