package com.kyvc.backend.domain.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.type.YesNoConverter;

import java.time.LocalDateTime;

// 약관 동의 이력
@Entity
@Table(name = "user_consents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "consent_type_code", nullable = false, length = 100)
    private String termsCode;

    @Column(name = "version", nullable = false, length = 50)
    private String termsVersion;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "agreed_yn", nullable = false, length = 1)
    private Boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    // 약관 동의 생성
    public static TermsConsent create(
            Long userId, // 사용자 ID
            String termsCode, // 약관 코드
            String termsVersion, // 약관 버전
            Boolean agreed, // 동의 여부
            LocalDateTime agreedAt // 동의 일시
    ) {
        TermsConsent termsConsent = new TermsConsent();
        termsConsent.userId = userId;
        termsConsent.termsCode = termsCode;
        termsConsent.termsVersion = termsVersion;
        termsConsent.update(agreed, agreedAt);
        return termsConsent;
    }

    // 약관 동의 변경
    public void update(
            Boolean agreed, // 동의 여부
            LocalDateTime agreedAt // 동의 일시
    ) {
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    // 동의 여부
    public boolean isAgreed() {
        return Boolean.TRUE.equals(agreed);
    }
}
