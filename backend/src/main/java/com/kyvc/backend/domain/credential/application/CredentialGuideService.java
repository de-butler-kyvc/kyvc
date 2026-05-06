package com.kyvc.backend.domain.credential.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.dto.CredentialIssueGuideResponse;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Credential 발급 안내 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CredentialGuideService {

    private final KycApplicationRepository kycApplicationRepository;
    private final CorporateRepository corporateRepository;
    private final CredentialRepository credentialRepository;

    // VC 발급 안내 조회
    public CredentialIssueGuideResponse getIssueGuide(
            Long userId // 사용자 ID
    ) {
        validateUserId(userId);

        Corporate corporate = corporateRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        KycApplication latestKyc = kycApplicationRepository.findLatestByApplicantUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_GUIDE_NOT_AVAILABLE));
        Credential credential = credentialRepository.findLatestByKycId(latestKyc.getKycId()).orElse(null);

        return switch (latestKyc.getKycStatus()) {
            case DRAFT -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    false,
                    KyvcEnums.KycCompletionAction.SUBMIT_KYC.name(),
                    "KYC 신청이 필요합니다.",
                    "VC 발급을 위해 KYC 신청을 제출해 주세요."
            );
            case SUBMITTED -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    false,
                    KyvcEnums.KycCompletionAction.WAIT_REVIEW.name(),
                    "KYC 심사 대기 중입니다.",
                    "KYC 신청이 접수되었습니다. 심사 결과를 기다려 주세요."
            );
            case AI_REVIEWING -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    false,
                    KyvcEnums.KycCompletionAction.WAIT_AI_REVIEW.name(),
                    "AI 심사 진행 중입니다.",
                    "AI 심사가 진행 중입니다. 완료 후 결과를 확인할 수 있습니다."
            );
            case NEED_SUPPLEMENT -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    false,
                    KyvcEnums.KycCompletionAction.CHECK_SUPPLEMENT.name(),
                    "보완서류 제출이 필요합니다.",
                    "보완요청 내용을 확인하고 필요한 서류를 제출해 주세요."
            );
            case MANUAL_REVIEW -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    false,
                    KyvcEnums.KycCompletionAction.WAIT_MANUAL_REVIEW.name(),
                    "수동심사 진행 중입니다.",
                    "관리자 수동심사가 진행 중입니다."
            );
            case REJECTED -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    false,
                    KyvcEnums.KycCompletionAction.CONTACT_SUPPORT.name(),
                    "KYC 심사가 반려되었습니다.",
                    "반려 사유를 확인하고 재신청하거나 고객지원에 문의해 주세요."
            );
            case APPROVED -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    true,
                    KyvcEnums.KycCompletionAction.ISSUE_CREDENTIAL.name(),
                    "VC 발급이 가능합니다.",
                    "KYC 심사가 승인되었습니다. VC 발급을 진행할 수 있습니다."
            );
            case VC_ISSUED -> buildResponse(
                    corporate,
                    latestKyc,
                    credential,
                    false,
                    KyvcEnums.KycCompletionAction.OPEN_WALLET.name(),
                    "VC 발급이 완료되었습니다.",
                    "발급된 VC를 모바일 Wallet에서 확인할 수 있습니다."
            );
        };
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // VC 발급 안내 응답 생성
    private CredentialIssueGuideResponse buildResponse(
            Corporate corporate, // 법인 엔티티
            KycApplication latestKyc, // 최신 KYC 엔티티
            Credential credential, // 최신 Credential 엔티티
            boolean issueAvailable, // VC 발급 가능 여부
            String nextActionCode, // 다음 행동 코드
            String guideTitle, // 안내 제목
            String guideMessage // 안내 메시지
    ) {
        return new CredentialIssueGuideResponse(
                corporate.getCorporateId(),
                latestKyc.getKycId(),
                latestKyc.getKycStatus().name(),
                credential != null && credential.isIssued(),
                credential == null ? null : credential.getCredentialStatus().name(),
                issueAvailable,
                nextActionCode,
                guideTitle,
                guideMessage
        );
    }
}
