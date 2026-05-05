package com.kyvc.backendadmin.domain.auth.repository;

import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.global.util.KyvcEnums;

import java.util.Optional;

// 서버 저장형 인증 토큰 Repository 계약
/**
 * 서버 저장형 인증 토큰 Repository입니다.
 *
 * <p>auth_tokens 테이블에 저장되는 Refresh Token, Password Reset Token,
 * MFA Session Token의 저장, 조회, 상태 변경 책임을 가집니다.</p>
 */
public interface AuthTokenRepository {

    // 신규 토큰 저장
    /**
     * 신규 인증 토큰을 저장합니다.
     *
     * @param authToken 저장할 인증 토큰 엔티티
     * @return 저장된 인증 토큰 엔티티
     */
    AuthToken save(AuthToken authToken);

    // 토큰 해시와 용도로 단건 조회
    /**
     * 토큰 해시와 토큰 타입으로 인증 토큰을 조회합니다.
     *
     * @param tokenHash 원문 토큰의 SHA-256 해시
     * @param tokenType 조회할 토큰 타입
     * @return 인증 토큰 Optional
     */
    Optional<AuthToken> findByTokenHashAndTokenType(String tokenHash, KyvcEnums.TokenType tokenType);

    // 특정 actor의 활성 토큰 일괄 폐기
    /**
     * 특정 행위자의 활성 토큰을 일괄 폐기합니다.
     *
     * @param actorType 폐기 대상 행위자 유형
     * @param actorId 폐기 대상 행위자 ID
     * @param tokenType 폐기할 토큰 타입
     */
    void revokeActiveTokens(KyvcEnums.ActorType actorType, Long actorId, KyvcEnums.TokenType tokenType);
}
