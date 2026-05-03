package com.kyvc.backend.domain.auth.domain;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// 인증 토큰 엔티티
@Entity
@Table(name = "auth_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_token_id")
    private Long authTokenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type_code", nullable = false, length = 50)
    private KyvcEnums.ActorType actorTypeCode;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type_code", nullable = false, length = 50)
    private KyvcEnums.TokenType tokenTypeCode;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "token_jti", unique = true)
    private String tokenJti;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_status_code", nullable = false, length = 50)
    private KyvcEnums.TokenStatus tokenStatusCode;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Refresh Token 저장 엔티티 생성
    public static AuthToken createRefreshToken(
            Long actorId, // 사용자 ID
            String tokenHash, // SHA-256 토큰 해시
            String tokenJti, // JWT ID
            LocalDateTime issuedAt, // 발급 시각
            LocalDateTime expiresAt // 만료 시각
    ) {
        AuthToken authToken = new AuthToken();
        authToken.actorTypeCode = KyvcEnums.ActorType.USER;
        authToken.actorId = actorId;
        authToken.tokenTypeCode = KyvcEnums.TokenType.REFRESH;
        authToken.tokenHash = tokenHash;
        authToken.tokenJti = tokenJti;
        authToken.tokenStatusCode = KyvcEnums.TokenStatus.ACTIVE;
        authToken.issuedAt = issuedAt;
        authToken.expiresAt = expiresAt;
        return authToken;
    }

    // 토큰 폐기 처리
    public void revoke(LocalDateTime revokedAt // 폐기 시각
    ) {
        this.tokenStatusCode = KyvcEnums.TokenStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

    // 활성 토큰 여부
    public boolean isActive() {
        return KyvcEnums.TokenStatus.ACTIVE == tokenStatusCode;
    }

    // 만료 여부
    public boolean isExpired(LocalDateTime now // 기준 시각
    ) {
        return expiresAt.isBefore(now);
    }
}
