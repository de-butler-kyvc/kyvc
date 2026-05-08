package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.credential.application.CredentialIssuanceService;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.DevKycApproveRequest;
import com.kyvc.backend.domain.kyc.dto.DevKycApproveResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// 개발/E2E 테스트용 KYC 임시 승인 서비스, 정식 관리자 승인 API 구현 시 제거 대상
@Service
@Transactional
@RequiredArgsConstructor
public class DevKycApprovalService {

    private static final String APPROVED_ONLY_MESSAGE = "개발/E2E 테스트용 KYC 임시 승인이 완료되었습니다.";
    private static final String APPROVED_AND_ISSUED_MESSAGE = "개발/E2E 테스트용 KYC 임시 승인 및 VC 발급 요청이 완료되었습니다.";
    private static final String ALREADY_APPROVED_MESSAGE = "이미 승인된 KYC 신청입니다. VC 발급 상태를 확인했습니다.";
    private static final String ALREADY_VC_ISSUED_MESSAGE = "이미 VC 발급이 완료된 KYC 신청입니다.";

    private final KycApplicationRepository kycApplicationRepository;
    private final CredentialRepository credentialRepository;
    private final CredentialIssuanceService credentialIssuanceService;
    private final CoreRequestService coreRequestService;
    private final LogEventLogger logEventLogger;

    // 개발/E2E 테스트용 KYC 임시 승인 처리
    public DevKycApproveResponse approveForE2eTest(
            Long kycId, // KYC 신청 ID
            DevKycApproveRequest request // 임시 승인 요청
    ) {
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        boolean shouldIssueCredential = request == null || request.shouldIssueCredential();
        KyvcEnums.KycStatus beforeStatus = kycApplication.getKycStatus();

        if (KyvcEnums.KycStatus.REJECTED == beforeStatus) {
            throw new ApiException(ErrorCode.KYC_ALREADY_REJECTED);
        }
        if (KyvcEnums.KycStatus.VC_ISSUED == beforeStatus) {
            Credential credential = findLatestCredential(kycId);
            return buildResponse(kycApplication, credential, false, ALREADY_VC_ISSUED_MESSAGE);
        }
        if (KyvcEnums.KycStatus.APPROVED != beforeStatus) {
            if (!kycApplication.canApproveForDevTest()) {
                throw new ApiException(ErrorCode.KYC_APPROVAL_NOT_ALLOWED);
            }
            kycApplication.approveForDevTest(LocalDateTime.now());
            kycApplicationRepository.save(kycApplication);
        }

        Credential credential = null;
        boolean credentialIssued = false;
        String message = KyvcEnums.KycStatus.APPROVED == beforeStatus ? ALREADY_APPROVED_MESSAGE : APPROVED_ONLY_MESSAGE;
        if (shouldIssueCredential) {
            credential = credentialIssuanceService.issueKycCredentialIfRequired(kycApplication);
            credentialIssued = credential != null;
            message = KyvcEnums.KycStatus.APPROVED == beforeStatus ? ALREADY_APPROVED_MESSAGE : APPROVED_AND_ISSUED_MESSAGE;
        } else {
            credential = findLatestCredential(kycId);
        }

        logDevApproval(kycApplication, credential, request, credentialIssued);
        return buildResponse(kycApplication, credential, credentialIssued, message);
    }

    private DevKycApproveResponse buildResponse(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // Credential
            boolean credentialIssued, // VC 발급 요청 여부
            String message // 처리 메시지
    ) {
        return new DevKycApproveResponse(
                kycApplication.getKycId(),
                kycApplication.getKycStatus().name(),
                credentialIssued,
                credential == null ? null : credential.getCredentialId(),
                credential == null || credential.getCredentialStatus() == null ? null : credential.getCredentialStatus().name(),
                resolveLatestCoreRequestId(credential),
                message
        );
    }

    private Credential findLatestCredential(
            Long kycId // KYC 신청 ID
    ) {
        return credentialRepository.findLatestByKycId(kycId).orElse(null);
    }

    private String resolveLatestCoreRequestId(
            Credential credential // Credential
    ) {
        if (credential == null || credential.getCredentialId() == null) {
            return null;
        }
        return coreRequestService.findLatestVcIssuanceRequest(credential.getCredentialId())
                .map(CoreRequest::getCoreRequestId)
                .orElse(null);
    }

    private void logDevApproval(
            KycApplication kycApplication, // KYC 신청
            Credential credential, // Credential
            DevKycApproveRequest request, // 임시 승인 요청
            boolean credentialIssued // VC 발급 요청 여부
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("kycId", kycApplication.getKycId());
        fields.put("corporateId", kycApplication.getCorporateId());
        fields.put("adminId", request == null ? null : request.adminId());
        fields.put("credentialId", credential == null ? null : credential.getCredentialId());
        fields.put("status", kycApplication.getKycStatus().name());
        fields.put("credentialIssued", credentialIssued);
        logEventLogger.info("dev.kyc.approved", "Dev KYC approval completed", fields);
    }
}
