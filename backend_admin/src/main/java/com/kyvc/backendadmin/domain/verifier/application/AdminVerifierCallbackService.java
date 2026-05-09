package com.kyvc.backendadmin.domain.verifier.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.domain.verifier.repository.VerifierQueryRepository;
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
import java.util.List;

/**
 * Verifier Callback 설정 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminVerifierCallbackService {

    private final AdminVerifierQueryService queryService;
    private final VerifierRepository verifierRepository;
    private final VerifierQueryRepository verifierQueryRepository;
    private final AuditLogWriter auditLogWriter;

    /**
     * Callback 목록을 조회합니다.
     *
     * @param verifierId Verifier ID
     * @return Callback 목록
     */
    @Transactional(readOnly = true)
    public List<AdminVerifierDtos.CallbackResponse> getCallbacks(Long verifierId) {
        queryService.findVerifier(verifierId);
        return verifierQueryRepository.findCallbacks(verifierId);
    }

    /**
     * Callback URL을 등록 또는 변경합니다.
     *
     * @param verifierId Verifier ID
     * @param request Callback 변경 요청
     * @return Callback 목록
     */
    @Transactional
    public List<AdminVerifierDtos.CallbackResponse> update(Long verifierId, AdminVerifierDtos.CallbackUpdateRequest request) {
        queryService.findVerifier(verifierId);
        validateUrl(request.callbackUrl());
        verifierRepository.saveCallback(verifierId, request.callbackUrl(),
                StringUtils.hasText(request.callbackStatusCode()) ? request.callbackStatusCode() : "ACTIVE");
        auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), "VERIFIER_CALLBACK_UPDATED",
                KyvcEnums.AuditTargetType.VERIFIER, verifierId, "Callback 변경", null, request.callbackUrl());
        return verifierQueryRepository.findCallbacks(verifierId);
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
}
