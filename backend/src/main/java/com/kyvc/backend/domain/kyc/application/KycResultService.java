package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.KycCompletionResponse;
import com.kyvc.backend.domain.kyc.dto.KycReviewFindingResponse;
import com.kyvc.backend.domain.kyc.dto.KycReviewSummaryResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// KYC 결과 조회 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KycResultService {

    private final KycApplicationRepository kycApplicationRepository;
    private final CorporateRepository corporateRepository;
    private final CredentialRepository credentialRepository;

    // AI 심사 결과 요약 조회
    public KycReviewSummaryResponse getAiReviewSummary(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        if (!isAiReviewSummaryAvailable(kycApplication.getKycStatus())) {
            throw new ApiException(ErrorCode.KYC_REVIEW_RESULT_NOT_FOUND);
        }

        return new KycReviewSummaryResponse(
                kycApplication.getKycId(),
                kycApplication.getKycStatus().name(),
                enumName(kycApplication.getAiReviewStatus()),
                enumName(kycApplication.getAiReviewResult()),
                kycApplication.getAiConfidenceScore(),
                resolveSummaryMessage(kycApplication),
                buildFindings(kycApplication),
                isManualReviewRequired(kycApplication),
                resolveReviewedAt(kycApplication)
        );
    }

    // KYC 완료 화면 조회
    public KycCompletionResponse getCompletion(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        validateCompletionAvailable(kycApplication);

        Corporate corporate = findCorporate(kycApplication.getCorporateId());
        Credential credential = credentialRepository.findLatestByKycId(kycId).orElse(null);

        if (KyvcEnums.KycStatus.VC_ISSUED == kycApplication.getKycStatus()) {
            return new KycCompletionResponse(
                    kycApplication.getKycId(),
                    corporate.getCorporateId(),
                    corporate.getCorporateName(),
                    kycApplication.getKycStatus().name(),
                    kycApplication.getApprovedAt(),
                    true,
                    credential == null ? null : credential.getCredentialId(),
                    "OPEN_WALLET",
                    "VC 발급이 완료되었습니다."
            );
        }

        if (credential == null) {
            return new KycCompletionResponse(
                    kycApplication.getKycId(),
                    corporate.getCorporateId(),
                    corporate.getCorporateName(),
                    kycApplication.getKycStatus().name(),
                    kycApplication.getApprovedAt(),
                    false,
                    null,
                    "ISSUE_CREDENTIAL",
                    "KYC 심사가 완료되었습니다. VC를 발급할 수 있습니다."
            );
        }

        boolean credentialIssued = credential.isIssued(); // Credential 발급 완료 여부
        return new KycCompletionResponse(
                kycApplication.getKycId(),
                corporate.getCorporateId(),
                corporate.getCorporateName(),
                kycApplication.getKycStatus().name(),
                kycApplication.getApprovedAt(),
                credentialIssued,
                credential.getCredentialId(),
                credentialIssued ? "OPEN_WALLET" : "ISSUE_CREDENTIAL",
                credentialIssued
                        ? "VC 발급이 완료되었습니다."
                        : "KYC 심사가 완료되었습니다. VC를 발급할 수 있습니다."
        );
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // KYC 요청 ID 검증
    private void validateKycId(
            Long kycId // KYC 요청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 사용자 소유 KYC 조회
    private KycApplication findOwnedKyc(
            Long userId, // 사용자 ID
            Long kycId // KYC 요청 ID
    ) {
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!kycApplication.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // 법인 조회
    private Corporate findCorporate(
            Long corporateId // 법인 ID
    ) {
        return corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
    }

    // AI 심사 결과 조회 가능 여부
    private boolean isAiReviewSummaryAvailable(
            KyvcEnums.KycStatus kycStatus // KYC 상태
    ) {
        return KyvcEnums.KycStatus.AI_REVIEWING == kycStatus
                || KyvcEnums.KycStatus.MANUAL_REVIEW == kycStatus
                || KyvcEnums.KycStatus.APPROVED == kycStatus
                || KyvcEnums.KycStatus.REJECTED == kycStatus
                || KyvcEnums.KycStatus.NEED_SUPPLEMENT == kycStatus
                || KyvcEnums.KycStatus.VC_ISSUED == kycStatus;
    }

    // KYC 완료 화면 조회 가능 상태 검증
    private void validateCompletionAvailable(
            KycApplication kycApplication // KYC 요청 엔티티
    ) {
        if (KyvcEnums.KycStatus.APPROVED != kycApplication.getKycStatus()
                && KyvcEnums.KycStatus.VC_ISSUED != kycApplication.getKycStatus()) {
            throw new ApiException(ErrorCode.KYC_COMPLETION_NOT_AVAILABLE);
        }
    }

    // 심사 결과 항목 목록 생성
    private List<KycReviewFindingResponse> buildFindings(
            KycApplication kycApplication // KYC 요청 엔티티
    ) {
        List<KycReviewFindingResponse> findings = new ArrayList<>();

        if (StringUtils.hasText(kycApplication.getAiReviewSummary())) {
            findings.add(new KycReviewFindingResponse(
                    "SUMMARY",
                    enumName(kycApplication.getAiReviewResult()),
                    kycApplication.getAiReviewSummary(),
                    kycApplication.getAiConfidenceScore()
            ));
        }
        if (StringUtils.hasText(kycApplication.getManualReviewReason())) {
            findings.add(new KycReviewFindingResponse(
                    "MANUAL_REVIEW_REASON",
                    KyvcEnums.AiReviewResult.NEED_MANUAL_REVIEW.name(),
                    kycApplication.getManualReviewReason(),
                    kycApplication.getAiConfidenceScore()
            ));
        }
        if (StringUtils.hasText(kycApplication.getRejectReason())) {
            findings.add(new KycReviewFindingResponse(
                    "REJECT_REASON",
                    KyvcEnums.AiReviewResult.FAIL.name(),
                    kycApplication.getRejectReason(),
                    kycApplication.getAiConfidenceScore()
            ));
        }

        return List.copyOf(findings);
    }

    // 수동심사 필요 여부 계산
    private Boolean isManualReviewRequired(
            KycApplication kycApplication // KYC 요청 엔티티
    ) {
        return KyvcEnums.KycStatus.MANUAL_REVIEW == kycApplication.getKycStatus()
                || KyvcEnums.AiReviewResult.NEED_MANUAL_REVIEW == kycApplication.getAiReviewResult()
                || StringUtils.hasText(kycApplication.getManualReviewReason());
    }

    // 심사 반영 일시 계산
    private LocalDateTime resolveReviewedAt(
            KycApplication kycApplication // KYC 요청 엔티티
    ) {
        if (kycApplication.getApprovedAt() != null) {
            return kycApplication.getApprovedAt();
        }
        if (kycApplication.getRejectedAt() != null) {
            return kycApplication.getRejectedAt();
        }
        return kycApplication.getUpdatedAt();
    }

    // 요약 메시지 계산
    private String resolveSummaryMessage(
            KycApplication kycApplication // KYC 요청 엔티티
    ) {
        if (StringUtils.hasText(kycApplication.getAiReviewSummary())) {
            return kycApplication.getAiReviewSummary();
        }
        if (StringUtils.hasText(kycApplication.getManualReviewReason())) {
            return kycApplication.getManualReviewReason();
        }
        if (StringUtils.hasText(kycApplication.getRejectReason())) {
            return kycApplication.getRejectReason();
        }
        return null;
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
