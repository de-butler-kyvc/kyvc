package com.kyvc.backendadmin.domain.credential.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.credential.dto.AdminCredentialDetailResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialActionResponse;
import com.kyvc.backendadmin.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backendadmin.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backendadmin.domain.credential.repository.CredentialRepository;
import com.kyvc.backendadmin.domain.credential.repository.CredentialQueryRepository;
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
import java.util.Set;

/**
 * 관리자 VC 재발급과 폐기 요청 위임을 처리합니다.
 *
 * <p>상태와 MFA를 검증한 뒤 Backend API에 요청을 위임하고, 성공한 관리자 행위만 감사 로그로 기록합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminCredentialLifecycleService {

    private static final Set<String> REISSUE_ALLOWED_STATUSES = Set.of(
            KyvcEnums.CredentialStatus.VALID.name(),
            KyvcEnums.CredentialStatus.EXPIRED.name(),
            KyvcEnums.CredentialStatus.SUSPENDED.name()
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
    private final AuthTokenRepository authTokenRepository;
    private final AuditLogWriter auditLogWriter;

    /**
     * Backend API로 VC 재발급을 요청합니다.
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
        AuthToken mfaToken = validateMfaToken(request.mfaToken(), adminId);
        Long credentialRequestId = createCredentialRequest(
                credentialId,
                REQUEST_TYPE_REISSUE,
                adminId,
                request.reason(),
                request.comment()
        );
        mfaToken.markUsed(LocalDateTime.now());
        writeAudit("VC_REISSUE_REQUESTED", credentialId, credentialRequestId, credential.credentialStatusCode(), request.reason());
        return CredentialActionResponse.accepted(
                credentialId,
                REQUEST_TYPE_REISSUE,
                credentialRequestId.toString(),
                "VC 재발급 요청이 접수되었습니다."
        );
    }

    /**
     * Backend API로 VC 폐기를 요청합니다.
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
        AuthToken mfaToken = validateMfaToken(request.mfaToken(), adminId);
        Long credentialRequestId = createCredentialRequest(
                credentialId,
                REQUEST_TYPE_REVOKE,
                adminId,
                request.reason(),
                request.comment()
        );
        mfaToken.markUsed(LocalDateTime.now());
        writeAudit("VC_REVOKE_REQUESTED", credentialId, credentialRequestId, credential.credentialStatusCode(), request.reason());
        return CredentialActionResponse.accepted(
                credentialId,
                REQUEST_TYPE_REVOKE,
                credentialRequestId.toString(),
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

    private AuthToken validateMfaToken(String rawMfaToken, Long adminId) {
        AuthToken authToken = authTokenRepository
                .findByTokenHashAndTokenType(TokenHashUtil.sha256(rawMfaToken), KyvcEnums.TokenType.MFA_SESSION)
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_TOKEN_INVALID));
        if (KyvcEnums.ActorType.ADMIN != authToken.getActorType()
                || !adminId.equals(authToken.getActorId())
                || !authToken.isActive()
                || authToken.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.MFA_TOKEN_INVALID);
        }
        return authToken;
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

    private Long createCredentialRequest(
            Long credentialId,
            String requestType,
            Long adminId,
            String reason,
            String comment
    ) {
        if (credentialRepository.existsInProgressCredentialRequest(credentialId, requestType)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "진행 중인 Credential 요청이 이미 존재합니다.");
        }
        return credentialRepository.saveCredentialRequest(
                credentialId,
                requestType,
                REQUEST_STATUS_REQUESTED,
                REQUESTED_BY_TYPE_ADMIN,
                adminId,
                null,
                buildReason(reason, comment),
                null
        );
    }

    private String buildReason(String reason, String comment) {
        if (!StringUtils.hasText(comment)) {
            return reason;
        }
        return "%s | comment=%s".formatted(reason, comment);
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
