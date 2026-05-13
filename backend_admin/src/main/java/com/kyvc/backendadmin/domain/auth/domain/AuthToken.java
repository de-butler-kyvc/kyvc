package com.kyvc.backendadmin.domain.auth.domain;

import com.kyvc.backendadmin.global.util.KyvcEnums;
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

import java.time.LocalDateTime;

// refresh, password reset, MFA session 등 서버에서 상태를 관리해야 하는 토큰 저장 엔티티
/**
 * auth_tokens 테이블과 매핑되는 서버 저장형 인증 토큰 엔티티입니다.
 *
 * <p>Refresh Token, Password Reset Token, MFA Session Token처럼 서버에서 상태를
 * 관리해야 하는 토큰의 해시, 타입, 상태, 만료 시각을 저장합니다.</p>
 */
@Entity
@Table(name = "auth_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthToken {

    // auth_tokens 기본 키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_token_id")
    private Long authTokenId;

    // 토큰 소유자 유형, Backend Admin에서는 ADMIN 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type_code", nullable = false, length = 50)
    private KyvcEnums.ActorType actorType;

    // 토큰 소유자 ID, ADMIN이면 admin_id
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    // 토큰 용도, REFRESH/PASSWORD_RESET/MFA_SESSION 등
    @Enumerated(EnumType.STRING)
    @Column(name = "token_type_code", nullable = false, length = 50)
    private KyvcEnums.TokenType tokenType;

    // 원문 토큰이 아닌 SHA-256 해시 값
    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    // 토큰 상태, ACTIVE/REVOKED/USED/EXPIRED
    @Enumerated(EnumType.STRING)
    @Column(name = "token_status_code", nullable = false, length = 50)
    private KyvcEnums.TokenStatus status;

    // JWT ID, JWT가 아닌 임의 토큰은 null 허용
    @Column(name = "token_jti")
    private String tokenJti;

    // 토큰 발급 시각
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    // 서버 기준 토큰 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 토큰 폐기 시각
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // 1회성 토큰 사용 완료 시각
    // 토큰 레코드 생성 시각
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 토큰 수정 시각
    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ACTIVE 상태의 신규 토큰 레코드 생성
    /**
     * ACTIVE 상태의 신규 인증 토큰 엔티티를 생성합니다.
     *
     * @param actorType 토큰 소유자 유형
     * @param actorId 토큰 소유자 ID
     * @param tokenType 토큰 용도
     * @param tokenHash 원문 토큰의 SHA-256 해시
     * @param expiresAt 토큰 만료 시각
     * @return 신규 인증 토큰 엔티티
     */
    public static AuthToken create(
            KyvcEnums.ActorType actorType,
            Long actorId,
            KyvcEnums.TokenType tokenType,
            String tokenHash,
            LocalDateTime expiresAt
    ) {
        AuthToken authToken = new AuthToken();
        authToken.actorType = actorType;
        authToken.actorId = actorId;
        authToken.tokenType = tokenType;
        authToken.tokenHash = tokenHash;
        authToken.status = KyvcEnums.TokenStatus.ACTIVE;
        authToken.issuedAt = LocalDateTime.now();
        authToken.expiresAt = expiresAt;
        return authToken;
    }

    // 토큰이 현재 사용 가능한 ACTIVE 상태인지 확인
    public boolean isActive() {
        return KyvcEnums.TokenStatus.ACTIVE == status;
    }

    // 전달받은 기준 시각으로 만료 여부 확인
    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    // refresh token 등 재사용 가능한 토큰 폐기 처리
    public void revoke(LocalDateTime revokedAt) {
        this.status = KyvcEnums.TokenStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

    // password reset token 등 1회성 토큰 사용 완료 처리
    public void markUsed(LocalDateTime usedAt) {
        this.status = KyvcEnums.TokenStatus.USED;
    }
}
