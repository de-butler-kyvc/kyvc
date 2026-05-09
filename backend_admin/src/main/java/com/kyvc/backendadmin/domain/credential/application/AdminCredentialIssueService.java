package com.kyvc.backendadmin.domain.credential.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.core.application.CoreRequestService;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialIssueRequest;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialIssueResponse;
import com.kyvc.backendadmin.domain.credential.repository.CredentialRepository;
import com.kyvc.backendadmin.domain.kyc.application.AdminKycAccessChecker;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Backend Admin VC 발급 요청 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminCredentialIssueService {

    private static final String ACTION_VC_ISSUE_REQUESTED = "VC_ISSUE_REQUESTED";

    private final KycApplicationRepository kycApplicationRepository;
    private final CredentialRepository credentialRepository;
    private final CoreRequestService coreRequestService;
    private final AuthTokenRepository authTokenRepository;
    private final AdminReviewRepository adminReviewRepository;
    private final AuditLogWriter auditLogWriter;
    private final AdminKycAccessChecker adminKycAccessChecker;

    /**
     * 승인된 KYC 신청 건에 대해 VC 발급 요청을 생성합니다.
     *
     * @param kycId KYC 신청 ID
     * @param request VC 발급 요청 정보
     * @return VC 발급 요청 결과
     */
    @Transactional
    public AdminCredentialIssueResponse issue(Long kycId, AdminCredentialIssueRequest request) {
        adminKycAccessChecker.validateActionAccess(kycId, ACTION_VC_ISSUE_REQUESTED);
        KycApplication application = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        validateApproved(application);
        validateDuplicateCredential(kycId);

        Long adminId = SecurityUtil.getCurrentAdminId();
        AuthToken mfaToken = validateMfaToken(request.mfaToken(), adminId);
        KyvcEnums.CredentialType credentialType = KyvcEnums.CredentialType.KYC_CREDENTIAL;
        String comment = StringUtils.hasText(request.comment()) ? request.comment() : "KYC 승인 완료에 따른 VC 발급";

        Long credentialId = credentialRepository.createIssuing(
                application.getCorporateId(),
                kycId,
                "kyvc-credential-%s".formatted(UUID.randomUUID()),
                credentialType,
                null,
                KyvcEnums.CredentialStatus.ISSUING
        );

        String coreRequestId = coreRequestService.createVcIssueRequest(credentialId, kycId, credentialType);

        // Credential 발급 요청과 상태 전이를 별도 테이블에 남겨 Core 처리 전후 추적이 가능하게 한다.
        credentialRepository.saveCredentialRequest(
                credentialId,
                KyvcEnums.CoreRequestType.VC_ISSUE.name(),
                "REQUESTED",
                KyvcEnums.ActorType.ADMIN.name(),
                adminId,
                null,
                comment,
                coreRequestId
        );
        credentialRepository.saveStatusHistory(
                credentialId,
                null,
                KyvcEnums.CredentialStatus.ISSUING.name(),
                KyvcEnums.ActorType.ADMIN.name(),
                adminId,
                null,
                comment
        );

        adminReviewRepository.saveReviewHistory(KycReviewHistory.create(
                kycId,
                adminId,
                KyvcEnums.ReviewActionType.ISSUE_VC,
                application.getKycStatusCode(),
                application.getKycStatusCode(),
                "VC 발급 요청 생성. credentialId=%d, coreRequestId=%s".formatted(credentialId, coreRequestId)
        ));

        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                adminId,
                ACTION_VC_ISSUE_REQUESTED,
                KyvcEnums.AuditTargetType.CREDENTIAL,
                credentialId,
                "VC 발급 요청 생성. kycId=%d, coreRequestId=%s".formatted(kycId, coreRequestId),
                null,
                KyvcEnums.CredentialStatus.ISSUING.name()
        );

        // MFA 세션 토큰은 중요 작업 1회 승인 용도이므로 성공적으로 요청을 생성한 뒤 사용 완료 처리한다.
        mfaToken.markUsed(LocalDateTime.now());
        return new AdminCredentialIssueResponse(
                credentialId,
                KyvcEnums.CredentialStatus.ISSUING.name(),
                true,
                coreRequestId
        );
    }

    private void validateApproved(KycApplication application) {
        // VC 발급은 최종 승인된 KYC 신청 건에서만 허용한다.
        if (KyvcEnums.KycStatus.APPROVED != application.getKycStatusCode()) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIAL_ISSUE_STATUS);
        }
    }

    private void validateDuplicateCredential(Long kycId) {
        // 이미 발급 중이거나 유효한 Credential이 있으면 중복 VC 발급 요청을 차단한다.
        if (credentialRepository.existsIssuingOrValidByKycId(kycId)) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIAL_ISSUE_STATUS, "이미 발급 중이거나 유효한 VC가 있습니다.");
        }
    }

    private AuthToken validateMfaToken(String rawMfaToken, Long adminId) {
        // MFA 토큰 원문은 저장/로그 출력하지 않고 해시로만 조회한다.
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
}
