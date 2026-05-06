package com.kyvc.backend.domain.auth.repository;

import com.kyvc.backend.domain.auth.domain.AuthToken;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

// 인증 토큰 Repository 구현체
@Repository
@RequiredArgsConstructor
public class AuthTokenRepositoryImpl implements AuthTokenRepository {

    private final AuthTokenJpaRepository authTokenJpaRepository;

    @Override
    public AuthToken save(
            AuthToken authToken // 저장 대상 인증 토큰
    ) {
        return authTokenJpaRepository.save(authToken);
    }

    @Override
    public Optional<AuthToken> findActiveRefreshTokenByHash(
            String tokenHash // SHA-256 토큰 해시
    ) {
        return authTokenJpaRepository.findByTokenHashAndTokenTypeCodeAndTokenStatusCode(
                tokenHash,
                KyvcEnums.TokenType.REFRESH,
                KyvcEnums.TokenStatus.ACTIVE
        );
    }

    @Override
    public Optional<AuthToken> findRefreshTokenByHash(
            String tokenHash // SHA-256 토큰 해시
    ) {
        return authTokenJpaRepository.findByTokenHashAndTokenTypeCode(
                tokenHash,
                KyvcEnums.TokenType.REFRESH
        );
    }

    @Override
    public Optional<AuthToken> findByHashAndType(
            String tokenHash, // SHA-256 토큰 해시
            KyvcEnums.TokenType tokenType // 토큰 유형
    ) {
        return authTokenJpaRepository.findByTokenHashAndTokenTypeCode(tokenHash, tokenType);
    }

    @Override
    public Optional<AuthToken> findActiveByHashAndType(
            String tokenHash, // SHA-256 토큰 해시
            KyvcEnums.TokenType tokenType // 토큰 유형
    ) {
        return authTokenJpaRepository.findByTokenHashAndTokenTypeCodeAndTokenStatusCode(
                tokenHash,
                tokenType,
                KyvcEnums.TokenStatus.ACTIVE
        );
    }

    @Override
    public void revoke(
            AuthToken authToken // 폐기 대상 인증 토큰
    ) {
        authToken.revoke(LocalDateTime.now());
        authTokenJpaRepository.save(authToken);
    }
}
