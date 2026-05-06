package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.AuthToken;
import com.kyvc.backend.global.util.KyvcEnums;

import java.util.Optional;

/**
 * 인증 토큰 Repository
 */
public interface AuthTokenRepository {

    /**
     * 인증 토큰 저장
     *
     * @param authToken 저장 대상 인증 토큰
     * @return 저장된 인증 토큰
     */
    AuthToken save(
            AuthToken authToken // 저장 대상 인증 토큰
    );

    /**
     * 활성 Refresh Token 조회
     *
     * @param tokenHash SHA-256 토큰 해시
     * @return 활성 Refresh Token 조회 결과
     */
    Optional<AuthToken> findActiveRefreshTokenByHash(
            String tokenHash // SHA-256 토큰 해시
    );

    /**
     * Refresh Token 조회
     *
     * @param tokenHash SHA-256 토큰 해시
     * @return Refresh Token 조회 결과
     */
    Optional<AuthToken> findRefreshTokenByHash(
            String tokenHash // SHA-256 토큰 해시
    );

    /**
     * 토큰 해시와 유형 기준 토큰 조회
     *
     * @param tokenHash SHA-256 토큰 해시
     * @param tokenType 토큰 유형
     * @return 토큰 조회 결과
     */
    Optional<AuthToken> findByHashAndType(
            String tokenHash, // SHA-256 토큰 해시
            KyvcEnums.TokenType tokenType // 토큰 유형
    );

    /**
     * 활성 토큰 해시와 유형 기준 토큰 조회
     *
     * @param tokenHash SHA-256 토큰 해시
     * @param tokenType 토큰 유형
     * @return 활성 토큰 조회 결과
     */
    Optional<AuthToken> findActiveByHashAndType(
            String tokenHash, // SHA-256 토큰 해시
            KyvcEnums.TokenType tokenType // 토큰 유형
    );

    /**
     * 인증 토큰 폐기
     *
     * @param authToken 폐기 대상 인증 토큰
     */
    void revoke(
            AuthToken authToken // 폐기 대상 인증 토큰
    );
}
