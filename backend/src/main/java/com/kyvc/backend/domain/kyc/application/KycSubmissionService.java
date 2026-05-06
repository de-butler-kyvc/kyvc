package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.document.application.RequiredDocumentPolicyProvider;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.dto.KycDocumentResponse;
import com.kyvc.backend.domain.document.dto.RequiredDocumentResponse;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.KycApplicationSummaryResponse;
import com.kyvc.backend.domain.kyc.dto.KycMissingItemResponse;
import com.kyvc.backend.domain.kyc.dto.KycSubmitResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// KYC 제출 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class KycSubmissionService {

    private static final String CORPORATE_NAME_REQUIRED = "CORPORATE_NAME_REQUIRED"; // 법인명 누락 코드
    private static final String BUSINESS_REGISTRATION_NO_REQUIRED = "BUSINESS_REGISTRATION_NO_REQUIRED"; // 사업자등록번호 누락 코드
    private static final String REPRESENTATIVE_REQUIRED = "REPRESENTATIVE_REQUIRED"; // 대표자 정보 누락 코드
    private static final String CORPORATE_TYPE_REQUIRED = "CORPORATE_TYPE_REQUIRED"; // 법인 유형 누락 코드
    private static final String DOCUMENT_STORE_OPTION_REQUIRED = "DOCUMENT_STORE_OPTION_REQUIRED"; // 원본서류 저장 옵션 누락 코드
    private static final String DOCUMENT_REQUIRED = "DOCUMENT_REQUIRED"; // 필수서류 누락 코드

    private final KycApplicationRepository kycApplicationRepository;
    private final CorporateRepository corporateRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final RequiredDocumentPolicyProvider requiredDocumentPolicyProvider;

    // KYC 제출 전 요약 조회
    @Transactional(readOnly = true)
    public KycApplicationSummaryResponse getSummary(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        return buildSummary(userId, findOwnedKyc(userId, kycId));
    }

    // KYC 제출
    public KycSubmitResponse submit(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId); // 사용자 소유 KYC
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }

        KycApplicationSummaryResponse summary = buildSummary(userId, kycApplication); // 제출 전 요약 정보
        validateSubmittable(summary);

        LocalDateTime submittedAt = LocalDateTime.now(); // 제출 일시
        kycApplication.submit(submittedAt);
        KycApplication savedApplication = kycApplicationRepository.save(kycApplication); // 저장 완료 KYC

        return new KycSubmitResponse(
                savedApplication.getKycId(),
                savedApplication.getKycStatus().name(),
                savedApplication.getSubmittedAt(),
                true,
                "KYC 신청이 제출되었습니다."
        );
    }

    // 제출 전 요약 생성
    private KycApplicationSummaryResponse buildSummary(
            Long userId, // 사용자 ID
            KycApplication kycApplication // KYC 신청 정보
    ) {
        Corporate corporate = findOwnedCorporate(userId, kycApplication.getCorporateId()); // 소유 법인 정보
        List<KycDocument> documents = kycDocumentRepository.findByKycId(kycApplication.getKycId()); // 업로드 문서 목록
        List<KycDocumentResponse> documentResponses = documents.stream()
                .map(this::toDocumentResponse)
                .toList();
        List<RequiredDocumentResponse> requiredDocuments = buildRequiredDocuments(kycApplication, documents); // 필수서류 충족 여부 목록
        List<KycMissingItemResponse> missingItems = buildMissingItems(corporate, kycApplication, documents); // 누락 항목 목록
        boolean submittable = kycApplication.isDraft() && isSubmittable(missingItems); // 제출 가능 여부

        return new KycApplicationSummaryResponse(
                kycApplication.getKycId(),
                kycApplication.getKycStatus().name(),
                corporate.getCorporateId(),
                corporate.getCorporateName(),
                corporate.getBusinessRegistrationNo(),
                corporate.getCorporateRegistrationNo(),
                corporate.getRepresentativeName(),
                corporate.getRepresentativePhone(),
                corporate.getRepresentativeEmail(),
                corporate.getAgentName(),
                corporate.getAgentPhone(),
                corporate.getAgentEmail(),
                corporate.getAgentAuthorityScope(),
                kycApplication.getCorporateTypeCode(),
                enumName(kycApplication.getOriginalDocumentStoreOption()),
                documentResponses,
                requiredDocuments,
                submittable,
                missingItems,
                kycApplication.getCreatedAt(),
                kycApplication.getUpdatedAt(),
                kycApplication.getSubmittedAt()
        );
    }

    // 필수서류 충족 여부 목록 생성
    private List<RequiredDocumentResponse> buildRequiredDocuments(
            KycApplication kycApplication, // KYC 신청 정보
            List<KycDocument> documents // 업로드 문서 목록
    ) {
        Set<String> uploadedDocumentTypeCodes = getUploadedDocumentTypeCodes(documents); // 업로드 문서 유형 코드 목록
        return requiredDocumentPolicyProvider.getRequiredDocuments(kycApplication.getCorporateTypeCode()).stream()
                .map(policy -> new RequiredDocumentResponse(
                        policy.documentTypeCode(),
                        policy.documentTypeName(),
                        policy.required(),
                        uploadedDocumentTypeCodes.contains(policy.documentTypeCode()),
                        policy.description(),
                        policy.allowedExtensions(),
                        policy.maxFileSizeMb()
                ))
                .toList();
    }

    // 누락 항목 목록 생성
    private List<KycMissingItemResponse> buildMissingItems(
            Corporate corporate, // 법인 정보
            KycApplication kycApplication, // KYC 신청 정보
            List<KycDocument> documents // 업로드 문서 목록
    ) {
        Set<KycMissingItemResponse> missingItems = new LinkedHashSet<>(); // 누락 항목 목록

        if (!StringUtils.hasText(corporate.getCorporateName())) {
            missingItems.add(new KycMissingItemResponse(
                    CORPORATE_NAME_REQUIRED,
                    "법인명 입력 필요",
                    "corporateName"
            ));
        }
        if (!StringUtils.hasText(corporate.getBusinessRegistrationNo())) {
            missingItems.add(new KycMissingItemResponse(
                    BUSINESS_REGISTRATION_NO_REQUIRED,
                    "사업자등록번호 입력 필요",
                    "businessRegistrationNo"
            ));
        }
        if (!StringUtils.hasText(corporate.getRepresentativeName())) {
            missingItems.add(new KycMissingItemResponse(
                    REPRESENTATIVE_REQUIRED,
                    "대표자 정보 입력 필요",
                    "representativeName"
            ));
        }
        if (!StringUtils.hasText(kycApplication.getCorporateTypeCode())) {
            missingItems.add(new KycMissingItemResponse(
                    CORPORATE_TYPE_REQUIRED,
                    "법인 유형 선택 필요",
                    "corporateTypeCode"
            ));
        }
        if (kycApplication.getOriginalDocumentStoreOption() == null) {
            missingItems.add(new KycMissingItemResponse(
                    DOCUMENT_STORE_OPTION_REQUIRED,
                    "원본서류 저장 옵션 선택 필요",
                    "documentStoreOption"
            ));
        }

        Set<String> uploadedDocumentTypeCodes = getUploadedDocumentTypeCodes(documents); // 업로드 문서 유형 코드 목록
        for (RequiredDocumentPolicyProvider.RequiredDocumentPolicy policy
                : requiredDocumentPolicyProvider.getRequiredDocuments(kycApplication.getCorporateTypeCode())) {
            if (!uploadedDocumentTypeCodes.contains(policy.documentTypeCode())) {
                missingItems.add(new KycMissingItemResponse(
                        DOCUMENT_REQUIRED,
                        policy.documentTypeName() + " 업로드가 필요합니다.",
                        policy.documentTypeCode()
                ));
            }
        }

        return List.copyOf(missingItems);
    }

    // 제출 가능 여부 판단
    private boolean isSubmittable(
            List<KycMissingItemResponse> missingItems // 누락 항목 목록
    ) {
        return missingItems == null || missingItems.isEmpty();
    }

    // 제출 가능 여부 검증
    private void validateSubmittable(
            KycApplicationSummaryResponse summary // 제출 전 요약 정보
    ) {
        if (!summary.submittable()) {
            boolean documentMissing = summary.missingItems().stream()
                    .anyMatch(item -> DOCUMENT_REQUIRED.equals(item.code())); // 필수서류 누락 여부
            throw new ApiException(documentMissing ? ErrorCode.DOCUMENT_REQUIRED_MISSING : ErrorCode.INVALID_REQUEST);
        }
    }

    // 업로드 문서 유형 코드 목록 생성
    private Set<String> getUploadedDocumentTypeCodes(
            List<KycDocument> documents // 업로드 문서 목록
    ) {
        return documents == null ? Set.of() : documents.stream()
                .map(KycDocument::getDocumentTypeCode)
                .collect(Collectors.toSet());
    }

    // 사용자 소유 KYC 조회
    private KycApplication findOwnedKyc(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!kycApplication.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // 사용자 소유 법인 조회
    private Corporate findOwnedCorporate(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        if (!corporate.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.CORPORATE_ACCESS_DENIED);
        }
        return corporate;
    }

    // KYC 문서 응답 변환
    private KycDocumentResponse toDocumentResponse(
            KycDocument kycDocument // KYC 문서
    ) {
        return new KycDocumentResponse(
                kycDocument.getDocumentId(),
                kycDocument.getKycId(),
                kycDocument.getDocumentTypeCode(),
                kycDocument.getFileName(),
                kycDocument.getMimeType(),
                kycDocument.getFileSize(),
                kycDocument.getDocumentHash(),
                kycDocument.getUploadStatus().name(),
                kycDocument.getUploadedAt()
        );
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // KYC 신청 ID 검증
    private void validateKycId(
            Long kycId // KYC 신청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
