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

    @Column(name = "corporate_id", nullable = false)
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
                || KyvcEnums.VpVerificationStatus.EXPIRED == vpVerificationStatus;
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

    // VP 제출 처리
    public void markPresented(
            Long credentialId, // Credential ID
            String vpJwtHash, // VP JWT 해시
            String coreRequestId, // Core 요청 ID
            LocalDateTime presentedAt // 제출 일시
    ) {
        this.credentialId = credentialId;
        this.vpJwtHash = vpJwtHash;
        this.coreRequestId = coreRequestId;
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
                KyvcEnums.VpVerificationStatus.INVALID,
                resultSummary,
                verifiedAt,
                KyvcEnums.Yn.N
        );
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
