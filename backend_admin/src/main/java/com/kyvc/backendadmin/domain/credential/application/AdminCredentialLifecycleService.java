package com.kyvc.backendadmin.domain.credential.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialActionResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backendadmin.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backendadmin.domain.credential.repository.CredentialQueryRepository;
import com.kyvc.backendadmin.domain.credential.repository.CredentialRepository;
import com.kyvc.backendadmin.domain.credential.repository.CredentialRepository.CredentialRequestSaveResult;
import com.kyvc.backendadmin.domain.verifier.application.AdminVerifierSecuritySupport;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 관리자 VC 재발급과 폐기 요청 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class AdminCredentialLifecycleService {

    private static final Set<String> REISSUE_ALLOWED_STATUSES = Set.of(
            KyvcEnums.CredentialStatus.VALID.name(),
            KyvcEnums.CredentialStatus.EXPIRED.name()
    );
    private static final Set<String> REVOKE_ALLOWED_STATUSES = Set.of(
            KyvcEnums.CredentialStatus.VALID.name(),
            KyvcEnums.CredentialStatus.SUSPENDED.name()
    );
    private static final String REQUEST_TYPE_REISSUE = "REISSUE";
    private static final String REQUEST_TYPE_REVOKE = "REVOKE";
    private static final String REQUEST_STATUS_REQUESTED = "REQUESTED";
    private static final String REQUESTED_BY_TYPE_ADMIN = "ADMIN";

    private final CredentialQueryRepository credentialQueryRepository;
    private final CredentialRepository credentialRepository;
    private final AdminVerifierSecuritySupport securitySupport;
    private final AuditLogWriter auditLogWriter;

    /**
     * VC 재발급 요청 생성
     *
     * @param credentialId Credential ID
     * @param request VC 재발급 요청 정보
     * @return VC 재발급 요청 결과
     */
    @Transactional
    public CredentialActionResponse reissue(Long credentialId, CredentialReissueRequest request) {
        validateReissueRequest(request);
        AdminCredentialDetailResponse credential = findCredential(credentialId);
        validateReissueStatus(credential.credentialStatusCode());
        Long adminId = SecurityUtil.getCurrentAdminId();
        validateNoInProgressRequest(credentialId, REQUEST_TYPE_REISSUE);
        securitySupport.validateAndUseMfa(request.mfaToken(), adminId);
        CredentialRequestSaveResult credentialRequest = createCredentialRequest(
                credentialId,
                REQUEST_TYPE_REISSUE,
                adminId,
                request.reason()
        );
        writeAudit(
                "VC_REISSUE_REQUESTED",
                credentialId,
                credentialRequest.credentialRequestId(),
                credential.credentialStatusCode(),
                request.reason()
        );
        return CredentialActionResponse.accepted(
                credentialId,
                credentialRequest.credentialRequestId(),
                REQUEST_TYPE_REISSUE,
                REQUEST_STATUS_REQUESTED,
                credentialRequest.requestedAt(),
                "VC 재발급 요청이 접수되었습니다."
        );
    }

    /**
     * VC 폐기 요청 생성
     *
     * @param credentialId Credential ID
     * @param request VC 폐기 요청 정보
     * @return VC 폐기 요청 결과
     */
    @Transactional
    public CredentialActionResponse revoke(Long credentialId, CredentialRevokeRequest request) {
        validateRevokeRequest(request);
        AdminCredentialDetailResponse credential = findCredential(credentialId);
        validateRevokeStatus(credential.credentialStatusCode());
        Long adminId = SecurityUtil.getCurrentAdminId();
        validateNoInProgressRequest(credentialId, REQUEST_TYPE_REVOKE);
        securitySupport.validateAndUseMfa(request.mfaToken(), adminId);
        CredentialRequestSaveResult credentialRequest = createCredentialRequest(
                credentialId,
                REQUEST_TYPE_REVOKE,
                adminId,
                request.reason()
        );
        writeAudit(
                "VC_REVOKE_REQUESTED",
                credentialId,
                credentialRequest.credentialRequestId(),
                credential.credentialStatusCode(),
                request.reason()
        );
        return CredentialActionResponse.accepted(
                credentialId,
                credentialRequest.credentialRequestId(),
                REQUEST_TYPE_REVOKE,
                REQUEST_STATUS_REQUESTED,
                credentialRequest.requestedAt(),
                "VC 폐기 요청이 접수되었습니다."
        );
    }

    private AdminCredentialDetailResponse findCredential(Long credentialId) {
        return credentialQueryRepository.findDetailById(credentialId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND));
    }

    private void validateReissueStatus(String status) {
        if (!REISSUE_ALLOWED_STATUSES.contains(status)) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIAL_STATUS);
        }
    }

    private void validateRevokeStatus(String status) {
        if (KyvcEnums.CredentialStatus.REVOKED.name().equals(status)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ALREADY_REVOKED);
        }
        if (!REVOKE_ALLOWED_STATUSES.contains(status)) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIAL_STATUS);
        }
    }

    private void validateReissueRequest(CredentialReissueRequest request) {
        if (request == null || !StringUtils.hasText(request.mfaToken()) || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRevokeRequest(CredentialRevokeRequest request) {
        if (request == null || !StringUtils.hasText(request.mfaToken()) || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateNoInProgressRequest(Long credentialId, String requestType) {
        if (credentialRepository.existsInProgressCredentialRequest(credentialId, requestType)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "진행 중인 Credential 요청이 이미 존재합니다.");
        }
    }

    private CredentialRequestSaveResult createCredentialRequest(
            Long credentialId,
            String requestType,
            Long adminId,
            String reason
    ) {
        return credentialRepository.saveCredentialRequest(
                credentialId,
                requestType,
                REQUEST_STATUS_REQUESTED,
                REQUESTED_BY_TYPE_ADMIN,
                adminId,
                null,
                reason,
                null,
                LocalDateTime.now()
        );
    }

    private void writeAudit(String action, Long credentialId, Long credentialRequestId, String beforeStatus, String reason) {
        auditLogWriter.write(
                KyvcEnums.ActorType.ADMIN,
                SecurityUtil.getCurrentAdminId(),
                action,
                KyvcEnums.AuditTargetType.CREDENTIAL,
                credentialId,
                "%s. credentialId=%d, credentialRequestId=%d, reason=%s".formatted(action, credentialId, credentialRequestId, reason),
                beforeStatus,
                REQUEST_STATUS_REQUESTED
        );
    }
}
