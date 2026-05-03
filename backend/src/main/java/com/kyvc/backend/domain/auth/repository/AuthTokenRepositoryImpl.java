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

    // 인증 토큰 저장
    @Override
    public AuthToken save(
            AuthToken authToken // 저장 대상 인증 토큰
    ) {
        return authTokenJpaRepository.save(authToken);
    }

    // 활성 Refresh Token 조회
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

    // Refresh Token 조회
    @Override
    public Optional<AuthToken> findRefreshTokenByHash(
            String tokenHash // SHA-256 토큰 해시
    ) {
        return authTokenJpaRepository.findByTokenHashAndTokenTypeCode(
                tokenHash,
                KyvcEnums.TokenType.REFRESH
        );
    }

    // 인증 토큰 폐기
    @Override
    public void revoke(
            AuthToken authToken // 폐기 대상 인증 토큰
    ) {
        authToken.revoke(LocalDateTime.now());
        authTokenJpaRepository.save(authToken);
    }
}
