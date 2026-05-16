package com.kyvc.backend.domain.vp.domain;

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
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

// VP 검증 Entity
@Entity
@Table(name = "vp_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vp_verification_id")
    private Long vpVerificationId;

    @Column(name = "credential_id")
    private Long credentialId;

    @Column(name = "corporate_id")
    private Long corporateId;

    @Column(name = "request_nonce", nullable = false, length = 255)
    private String requestNonce;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "vp_verification_status_code", nullable = false, length = 50)
    private KyvcEnums.VpVerificationStatus vpVerificationStatus;

    @Column(name = "replay_suspected_yn", nullable = false, length = 1)
    private String replaySuspectedYn;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "presented_at")
    private LocalDateTime presentedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "vp_request_id", length = 255)
    private String vpRequestId;

    @Column(name = "requester_name", length = 255)
    private String requesterName;

    @Column(name = "required_claims_json", columnDefinition = "TEXT")
    private String requiredClaimsJson;

    @Column(name = "challenge", length = 255)
    private String challenge;

    @Column(name = "vp_jwt_hash", length = 255)
    private String vpJwtHash;

    @Column(name = "qr_token_hash", length = 255)
    private String qrTokenHash;

    @Column(name = "browser_session_hash", length = 255)
    private String browserSessionHash;

    @Column(name = "login_completed_at")
    private LocalDateTime loginCompletedAt;

    @Column(name = "core_request_id", length = 255)
    private String coreRequestId;

    @Column(name = "verifier_id")
    private Long verifierId;

    @Column(name = "finance_institution_code", length = 50)
    private String financeInstitutionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type_code", length = 30)
    private KyvcEnums.VpRequestType requestTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_yn", length = 1)
    private KyvcEnums.Yn testYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "re_auth_yn", length = 1)
    private KyvcEnums.Yn reAuthYn;

    @Column(name = "permission_result_json", columnDefinition = "TEXT")
    private String permissionResultJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_status_code", length = 30)
    private KyvcEnums.CallbackDeliveryStatus callbackStatusCode;

    @Column(name = "callback_sent_at")
    private LocalDateTime callbackSentAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // VP 요청 생성
    public static VpVerification createRequest(
            Long credentialId, // Credential ID
            Long corporateId, // 법인 ID
            String vpRequestId, // VP 요청 ID
            String requestNonce, // 요청 nonce
            String challenge, // 요청 challenge
            String purpose, // 제출 목적
            String requesterName, // 요청 기관명
            String requiredClaimsJson, // 요구 Claim JSON
            LocalDateTime expiresAt, // 만료 일시
            Long verifierId, // Verifier ID
            String financeInstitutionCode, // 금융기관 코드
            KyvcEnums.VpRequestType requestTypeCode, // VP 요청 유형
            KyvcEnums.Yn testYn, // 테스트 여부
            KyvcEnums.Yn reAuthYn, // 재인증 여부
            String permissionResultJson // 부가 결과 JSON
    ) {
        VpVerification vpVerification = new VpVerification();
        vpVerification.credentialId = credentialId;
        vpVerification.corporateId = corporateId;
        vpVerification.vpRequestId = vpRequestId;
        vpVerification.requestNonce = requestNonce;
        vpVerification.challenge = challenge;
        vpVerification.purpose = purpose;
        vpVerification.requesterName = requesterName;
        vpVerification.requiredClaimsJson = requiredClaimsJson;
        vpVerification.expiresAt = expiresAt;
        vpVerification.verifierId = verifierId;
        vpVerification.financeInstitutionCode = financeInstitutionCode;
        vpVerification.requestTypeCode = requestTypeCode;
        vpVerification.testYn = testYn == null ? KyvcEnums.Yn.N : testYn;
        vpVerification.reAuthYn = reAuthYn == null ? KyvcEnums.Yn.N : reAuthYn;
        vpVerification.permissionResultJson = permissionResultJson;
        vpVerification.vpVerificationStatus = KyvcEnums.VpVerificationStatus.REQUESTED;
        vpVerification.replaySuspectedYn = KyvcEnums.Yn.N.name();
        vpVerification.requestedAt = LocalDateTime.now();
        return vpVerification;
    }

    // 웹 VP 로그인 요청 생성
    public static VpVerification createWebVpLoginRequest(
            String vpRequestId, // VP 로그인 요청 ID
            String qrTokenHash, // QR 토큰 해시
            String browserSessionHash, // 브라우저 세션 해시
            String requestNonce, // Core nonce
            String challenge, // Core challenge
            String domain, // Core domain
            String requiredClaimsJson, // 필수 disclosure JSON
            LocalDateTime expiresAt, // 만료 일시
            String metadataJson // Core challenge 메타데이터 JSON
    ) {
        VpVerification vpVerification = new VpVerification();
        vpVerification.vpRequestId = vpRequestId;
        vpVerification.qrTokenHash = qrTokenHash;
        vpVerification.browserSessionHash = browserSessionHash;
        vpVerification.requestNonce = requestNonce;
        vpVerification.challenge = challenge;
        vpVerification.purpose = "WEB_VP_LOGIN";
        vpVerification.requesterName = domain;
        vpVerification.requiredClaimsJson = requiredClaimsJson;
        vpVerification.expiresAt = expiresAt;
        vpVerification.permissionResultJson = metadataJson;
        vpVerification.requestTypeCode = KyvcEnums.VpRequestType.VP_LOGIN;
        vpVerification.testYn = KyvcEnums.Yn.N;
        vpVerification.reAuthYn = KyvcEnums.Yn.N;
        vpVerification.vpVerificationStatus = KyvcEnums.VpVerificationStatus.REQUESTED;
        vpVerification.replaySuspectedYn = KyvcEnums.Yn.N.name();
        vpVerification.requestedAt = LocalDateTime.now();
        return vpVerification;
    }

    // 요청 만료 여부
    public boolean isExpired(
            LocalDateTime now // 기준 일시
    ) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    // 요청 상태 여부
    public boolean isRequested() {
        return KyvcEnums.VpVerificationStatus.REQUESTED == vpVerificationStatus;
    }

    // 제출 상태 여부
    public boolean isPresented() {
        return KyvcEnums.VpVerificationStatus.PRESENTED == vpVerificationStatus;
    }

    // 완료 상태 여부
    public boolean isCompleted() {
        return KyvcEnums.VpVerificationStatus.VALID == vpVerificationStatus
                || KyvcEnums.VpVerificationStatus.INVALID == vpVerificationStatus
                || KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED == vpVerificationStatus
                || KyvcEnums.VpVerificationStatus.EXPIRED == vpVerificationStatus
                || KyvcEnums.VpVerificationStatus.FAILED == vpVerificationStatus;
    }

    // nonce 일치 여부
    public boolean matchesNonce(
            String nonce // nonce
    ) {
        return requestNonce != null && requestNonce.equals(nonce);
    }

    // challenge 일치 여부
    public boolean matchesChallenge(
            String challenge // challenge
    ) {
        return this.challenge != null && this.challenge.equals(challenge);
    }

    // QR 토큰 해시 일치 여부
    public boolean matchesQrTokenHash(
            String qrTokenHash // QR 토큰 해시
    ) {
        return this.qrTokenHash != null && this.qrTokenHash.equals(qrTokenHash);
    }

    // QR 토큰 해시 저장
    public void applyQrTokenHash(
            String qrTokenHash // QR 토큰 해시
    ) {
        this.qrTokenHash = qrTokenHash;
    }

    // 브라우저 세션 해시 일치 여부
    public boolean matchesBrowserSessionHash(
            String browserSessionHash // 브라우저 세션 해시
    ) {
        return this.browserSessionHash != null && this.browserSessionHash.equals(browserSessionHash);
    }

    // VP 제출 처리
    public void markPresented(
            Long credentialId, // Credential ID
            String vpJwtHash, // VP JWT 해시
            String coreRequestId, // Core 요청 ID
            LocalDateTime presentedAt // 제출 일시
    ) {
        markPresentedForCorporate(this.corporateId, credentialId, vpJwtHash, coreRequestId, presentedAt);
    }

    // VP 제출 법인 반영 처리
    public void markPresentedForCorporate(
            Long corporateId, // 제출 법인 ID
            Long credentialId, // Credential ID
            String vpJwtHash, // VP JWT 해시
            String coreRequestId, // Core 요청 ID
            LocalDateTime presentedAt // 제출 일시
    ) {
        this.corporateId = corporateId;
        this.credentialId = credentialId;
        this.vpJwtHash = vpJwtHash;
        this.coreRequestId = coreRequestId;
        this.presentedAt = presentedAt;
        this.vpVerificationStatus = KyvcEnums.VpVerificationStatus.PRESENTED;
        this.replaySuspectedYn = KyvcEnums.Yn.N.name();
        this.resultSummary = null;
        this.verifiedAt = null;
    }

    // 웹 VP 로그인 제출 처리
    public void markWebVpLoginPresented(
            Long corporateId, // 법인 ID
            Long credentialId, // Credential ID
            String vpHash, // VP 객체 해시
            LocalDateTime presentedAt // 제출 일시
    ) {
        this.corporateId = corporateId;
        this.credentialId = credentialId;
        this.vpJwtHash = vpHash;
        this.presentedAt = presentedAt;
        this.vpVerificationStatus = KyvcEnums.VpVerificationStatus.PRESENTED;
        this.replaySuspectedYn = KyvcEnums.Yn.N.name();
        this.resultSummary = null;
        this.verifiedAt = null;
    }

    // 검증 성공 처리
    public void markValid(
            String resultSummary, // 결과 요약
            LocalDateTime verifiedAt // 검증 일시
    ) {
        applyVerificationResult(
                KyvcEnums.VpVerificationStatus.VALID,
                resultSummary,
                verifiedAt,
                KyvcEnums.Yn.N
        );
    }

    // 검증 실패 처리
    public void markInvalid(
            String resultSummary, // 결과 요약
            LocalDateTime verifiedAt // 검증 일시
    ) {
        applyVerificationResult(
                KyvcEnums.VpVerificationStatus.INVALID,
                resultSummary,
                verifiedAt,
                KyvcEnums.Yn.N
        );
    }

    // Replay 의심 처리
    public void markReplaySuspected(
            String resultSummary, // 결과 요약
            LocalDateTime verifiedAt // 검증 일시
    ) {
        applyVerificationResult(
                KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED,
                resultSummary,
                verifiedAt,
                KyvcEnums.Yn.Y
        );
    }

    // 검증 실패 처리
    public void markFailed(
            String resultSummary, // 결과 요약
            LocalDateTime verifiedAt // 검증 일시
    ) {
        applyVerificationResult(
                KyvcEnums.VpVerificationStatus.FAILED,
                resultSummary,
                verifiedAt,
                KyvcEnums.Yn.N
        );
    }

    // 요청 만료 처리
    public void markExpired(
            String resultSummary, // 결과 요약
            LocalDateTime verifiedAt // 처리 일시
    ) {
        applyVerificationResult(
                KyvcEnums.VpVerificationStatus.EXPIRED,
                resultSummary,
                verifiedAt,
                KyvcEnums.Yn.N
        );
    }

    // 요청 취소 처리
    public void markCancelled(
            LocalDateTime cancelledAt // 취소 일시
    ) {
        applyVerificationResult(
                KyvcEnums.VpVerificationStatus.CANCELLED,
                "VP 요청 취소",
                cancelledAt,
                KyvcEnums.Yn.N
        );
    }

    // 웹 VP 로그인 완료 처리
    public void markLoginCompleted(
            LocalDateTime loginCompletedAt // 로그인 완료 일시
    ) {
        this.loginCompletedAt = loginCompletedAt == null ? LocalDateTime.now() : loginCompletedAt;
    }

    // 검증 결과 반영
    private void applyVerificationResult(
            KyvcEnums.VpVerificationStatus status, // VP 검증 상태
            String resultSummary, // 결과 요약
            LocalDateTime verifiedAt, // 검증 일시
            KyvcEnums.Yn replaySuspected // Replay 의심 여부
    ) {
        this.vpVerificationStatus = status;
        this.resultSummary = resultSummary;
        this.verifiedAt = verifiedAt == null ? LocalDateTime.now() : verifiedAt;
        this.replaySuspectedYn = replaySuspected.name();
    }
}
