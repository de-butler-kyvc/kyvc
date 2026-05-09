package com.kyvc.backendadmin.domain.verifier.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.domain.verifier.repository.VerifierRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;

/**
 * Verifier 등록, 수정, 승인, 중지 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminVerifierService {

    private final VerifierRepository verifierRepository;
    private final AdminVerifierQueryService queryService;
    private final AdminVerifierSecuritySupport securitySupport;
    private final AuditLogWriter auditLogWriter;

    /**
     * Verifier를 PENDING 상태로 등록합니다.
     *
     * @param request 등록 요청
     * @return 등록된 Verifier 응답
     */
    @Transactional
    public AdminVerifierDtos.Response create(AdminVerifierDtos.CreateRequest request) {
        if (verifierRepository.existsByName(request.name())) {
            throw new ApiException(ErrorCode.VERIFIER_ALREADY_EXISTS);
        }
        Long verifierId = verifierRepository.create(request.name(), request.managerEmail());
        if (StringUtils.hasText(request.callbackUrl())) {
            validateUrl(request.callbackUrl());
            verifierRepository.saveCallback(verifierId, request.callbackUrl(), "ACTIVE");
        }
        audit("VERIFIER_CREATED", verifierId, "Verifier 등록. name=%s".formatted(request.name()), null, "PENDING");
        return queryService.toResponse(queryService.findVerifier(verifierId));
    }

    /**
     * Verifier 기본정보와 Callback URL을 수정합니다.
     *
     * @param verifierId Verifier ID
     * @param request 수정 요청
     * @return 수정된 Verifier 응답
     */
    @Transactional
    public AdminVerifierDtos.Response update(Long verifierId, AdminVerifierDtos.UpdateRequest request) {
        queryService.findVerifier(verifierId);
        if (StringUtils.hasText(request.callbackUrl())) {
            validateUrl(request.callbackUrl());
            verifierRepository.saveCallback(verifierId, request.callbackUrl(), "ACTIVE");
        }
        verifierRepository.update(verifierId, request.name(), request.managerEmail());
        audit("VERIFIER_UPDATED", verifierId, "Verifier 수정", null, request.name());
        return queryService.toResponse(queryService.findVerifier(verifierId));
    }

    /**
     * Verifier를 승인 상태로 변경합니다.
     *
     * @param verifierId Verifier ID
     * @param request 승인 요청
     * @return 승인된 Verifier 응답
     */
    @Transactional
    public AdminVerifierDtos.Response approve(Long verifierId, AdminVerifierDtos.ApproveRequest request) {
        VerifierRepository.VerifierRow verifier = queryService.findVerifier(verifierId);
        if (!"PENDING".equals(verifier.status()) && !"REJECTED".equals(verifier.status())) {
            throw new ApiException(ErrorCode.INVALID_VERIFIER_STATUS);
        }
        Long adminId = SecurityUtil.getCurrentAdminId();
        securitySupport.validateAndUseMfa(request.mfaToken(), adminId);
        verifierRepository.updateStatus(verifierId, "APPROVED", LocalDateTime.now(), null);
        audit("VERIFIER_APPROVED", verifierId, "Verifier 승인. comment=%s".formatted(nullToBlank(request.comment())),
                verifier.status(), "APPROVED");
        return queryService.toResponse(queryService.findVerifier(verifierId));
    }

    /**
     * Verifier를 중지하고 ACTIVE API Key를 폐기합니다.
     *
     * @param verifierId Verifier ID
     * @param request 중지 요청
     * @return 중지된 Verifier 응답
     */
    @Transactional
    public AdminVerifierDtos.Response suspend(Long verifierId, AdminVerifierDtos.SuspendRequest request) {
        VerifierRepository.VerifierRow verifier = queryService.findVerifier(verifierId);
        if (!"APPROVED".equals(verifier.status())) {
            throw new ApiException(ErrorCode.INVALID_VERIFIER_STATUS);
        }
        Long adminId = SecurityUtil.getCurrentAdminId();
        securitySupport.validateAndUseMfa(request.mfaToken(), adminId);
        verifierRepository.updateStatus(verifierId, "SUSPENDED", null, LocalDateTime.now());
        verifierRepository.revokeActiveKeys(verifierId);
        audit("VERIFIER_SUSPENDED", verifierId, "Verifier 중지. reasonCode=%s".formatted(nullToBlank(request.reasonCode())),
                verifier.status(), "SUSPENDED");
        return queryService.toResponse(queryService.findVerifier(verifierId));
    }

    private void validateUrl(String callbackUrl) {
        try {
            URI uri = URI.create(callbackUrl);
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                throw new ApiException(ErrorCode.INVALID_CALLBACK_URL);
            }
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_CALLBACK_URL);
        }
    }

    private void audit(String action, Long verifierId, String summary, String beforeValue, String afterValue) {
        auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), action,
                KyvcEnums.AuditTargetType.VERIFIER, verifierId, summary, beforeValue, afterValue);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }
}
