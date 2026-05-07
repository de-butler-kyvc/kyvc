package com.kyvc.backendadmin.domain.credential.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.core.application.CoreRequestService;
import com.kyvc.backendadmin.domain.credential.dto.CredentialIssueRequest;
import com.kyvc.backendadmin.domain.credential.dto.CredentialIssueResponse;
import com.kyvc.backendadmin.domain.credential.repository.CredentialRepository;
import com.kyvc.backendadmin.domain.kyc.domain.KycApplication;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.domain.review.domain.KycReviewHistory;
import com.kyvc.backendadmin.domain.review.repository.AdminReviewRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Backend Admin VC 발급 요청 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminCredentialIssueService {

    private final KycApplicationRepository kycApplicationRepository;
    private final CredentialRepository credentialRepository;
    private final CoreRequestService coreRequestService;
    private final AuthTokenRepository authTokenRepository;
    private final AdminReviewRepository adminReviewRepository;
    private final AuditLogWriter auditLogWriter;

    /**
     * 승인된 KYC 신청 건에 대해 VC 발급 요청을 생성합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request VC 발급 요청 정보
     * @return VC 발급 요청 결과
     */
    @Transactional
    public CredentialIssueResponse issue(Long kycId, CredentialIssueRequest request) {
        KycApplication application = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        validateApproved(application);
        KyvcEnums.CredentialType credentialType = parseCredentialType(request.credentialType());
        validateDuplicateCredential(kycId);
        AuthToken mfaToken = validateMfaToken(request.mfaToken());

        // credentials row 생성: 실제 VC 발급 전까지 ISSUING 상태로 발급 대기 row를 만든다.
        Long credentialId = credentialRepository.createIssuing(
                application.getCorporateId(),
                kycId,
                "kyvc-credential-%s".formatted(UUID.randomUUID()),
                credentialType,
                null,
                KyvcEnums.CredentialStatus.ISSUING
        );

        // core_requests row 생성: 실제 VC 발급은 Core에서 처리하므로 Credential 대상 VC_ISSUE 요청만 생성한다.
        String coreRequestId = coreRequestService.createVcIssueRequest(credentialId, kycId, credentialType);

        Long adminId = SecurityUtil.getCurrentAdminId();
        // kyc_review_histories 기록: VC 발급 요청 이벤트를 KYC 심사 이력에 남긴다.
        adminReviewRepository.saveReviewHistory(KycReviewHistory.create(
                kycId,
                adminId,
                KyvcEnums.ReviewActionType.ISSUE_VC,
                application.getKycStatusCode(),
                KyvcEnums.KycStatus.VC_ISSUED,
                "VC 발급 요청 생성. credentialId=%d, coreRequestId=%s".formatted(credentialId, coreRequestId)
        ));

        // audit_logs 기록: 관리자에 의한 VC 발급 요청과 Core 요청 ID를 감사 로그에 남긴다.
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                "VC_ISSUE_REQUESTED",
                KyvcEnums.AuditTargetType.CREDENTIAL,
                credentialId,
                "VC 발급 요청 생성. kycId=%d, coreRequestId=%s".formatted(kycId, coreRequestId),
                null,
                KyvcEnums.CredentialStatus.ISSUING.name()
        );

        mfaToken.markUsed(LocalDateTime.now());
        return new CredentialIssueResponse(
                credentialId,
                coreRequestId,
                credentialType.name(),
                KyvcEnums.CredentialStatus.ISSUING.name()
        );
    }

    private void validateApproved(KycApplication application) {
        // KYC APPROVED 상태 검증: 승인된 KYC 신청 건만 VC 발급을 요청할 수 있다.
        if (KyvcEnums.KycStatus.APPROVED != application.getKycStatusCode()) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIAL_ISSUE_STATUS);
        }
    }

    private void validateDuplicateCredential(Long kycId) {
        // 중복 VC 발급 요청 방지: 이미 발급 중이거나 유효한 Credential이 있으면 새 요청을 막는다.
        if (credentialRepository.existsIssuingOrValidByKycId(kycId)) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIAL_ISSUE_STATUS, "이미 발급 중이거나 유효한 VC가 있습니다.");
        }
    }

    private AuthToken validateMfaToken(String rawMfaToken) {
        // MFA 토큰 검증: 현재 관리자 소유의 ACTIVE MFA_SESSION 토큰만 중요한 VC 발급 요청에 사용할 수 있다.
        Long adminId = SecurityUtil.getCurrentAdminId();
        AuthToken authToken = authTokenRepository
                .findByTokenHashAndTokenType(TokenHashUtil.sha256(rawMfaToken), KyvcEnums.TokenType.MFA_SESSION)
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_TOKEN_INVALID));
        if (KyvcEnums.ActorType.ADMIN != authToken.getActorType() || !adminId.equals(authToken.getActorId())) {
            throw new ApiException(ErrorCode.MFA_TOKEN_INVALID);
        }
        if (!authToken.isActive() || authToken.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.MFA_TOKEN_INVALID);
        }
        return authToken;
    }

    private KyvcEnums.CredentialType parseCredentialType(String credentialType) {
        if (credentialType == null || credentialType.isBlank()) {
            return KyvcEnums.CredentialType.KYC_CREDENTIAL;
        }
        try {
            return KyvcEnums.CredentialType.valueOf(credentialType);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "credentialType이 유효하지 않습니다.");
        }
    }
}
