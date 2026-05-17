package com.kyvc.backend.domain.document.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.application.CorporateTypeCodeNormalizer;
import com.kyvc.backend.domain.document.domain.KycDocument;
import com.kyvc.backend.domain.document.dto.RequiredDocumentResponse;
import com.kyvc.backend.domain.document.repository.KycDocumentRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 필수서류 안내 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequiredDocumentService {

    private static final String CORPORATE_TYPE_GROUP = "CORPORATE_TYPE"; // 법인 유형 공통코드 그룹
    private static final String DOCUMENT_TYPE_GROUP = "DOCUMENT_TYPE"; // 문서 유형 공통코드 그룹

    private final KycApplicationRepository kycApplicationRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final CommonCodeProvider commonCodeProvider;
    private final RequiredDocumentPolicyProvider requiredDocumentPolicyProvider;

    // 법인 유형 기준 필수서류 안내 조회
    public List<RequiredDocumentResponse> getRequiredDocuments(
            Long userId, // 사용자 ID
            String corporateTypeCode // 법인 유형 코드
    ) {
        validateUserId(userId);

        String normalizedCorporateTypeCode = validateCorporateTypeCode(corporateTypeCode); // 정규화 법인 유형 코드
        commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, normalizedCorporateTypeCode);

        return buildRequiredDocumentResponses(normalizedCorporateTypeCode, Set.of());
    }

    // 신청 건 기준 필수서류 안내 조회
    public List<RequiredDocumentResponse> getRequiredDocumentsByKyc(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);

        KycApplication kycApplication = findOwnedKyc(userId, kycId); // 사용자 소유 KYC
        String normalizedCorporateTypeCode = validateCorporateTypeCode(kycApplication.getCorporateTypeCode()); // 정규화 법인 유형 코드
        commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, normalizedCorporateTypeCode);

        Set<String> uploadedDocumentTypes = kycDocumentRepository.findByKycId(kycId).stream()
                .map(KycDocument::getDocumentTypeCode)
                .map(DocumentTypeCodeNormalizer::normalize)
                .collect(Collectors.toSet());

        return buildRequiredDocumentResponses(normalizedCorporateTypeCode, uploadedDocumentTypes);
    }

    // 법인 유형과 업로드 문서 기준 필수서류 응답 목록 생성
    public List<RequiredDocumentResponse> buildRequiredDocumentResponses(
            String corporateTypeCode, // 법인 유형 코드
            Set<String> uploadedDocumentTypes // 업로드 문서 유형 목록
    ) {
        String normalizedCorporateTypeCode = validateCorporateTypeCode(corporateTypeCode); // 정규화 법인 유형 코드
        Set<String> normalizedUploadedDocumentTypes = normalizeDocumentTypes(uploadedDocumentTypes); // 정규화 업로드 문서 유형 목록
        return buildResponses(
                requiredDocumentPolicyProvider.getRequiredDocuments(normalizedCorporateTypeCode),
                normalizedUploadedDocumentTypes
        );
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

    // 필수서류 응답 목록 생성
    private List<RequiredDocumentResponse> buildResponses(
            List<RequiredDocumentPolicyProvider.RequiredDocumentPolicy> policies, // 필수서류 정책 목록
            Set<String> uploadedDocumentTypes // 업로드 문서 유형 목록
    ) {
        validateDocumentPolicies(policies);

        return policies.stream()
                .map(policy -> new RequiredDocumentResponse(
                        policy.documentTypeCode(),
                        policy.documentTypeName(),
                        policy.required(),
                        uploadedDocumentTypes.contains(policy.documentTypeCode()),
                        policy.description(),
                        policy.allowedExtensions(),
                        policy.maxFileSizeMb(),
                        policy.groupCode(),
                        policy.groupName(),
                        policy.minRequiredCount(),
                        policy.groupCandidate()
                ))
                .toList();
    }

    // 필수서류 정책 공통코드 검증
    private void validateDocumentPolicies(
            List<RequiredDocumentPolicyProvider.RequiredDocumentPolicy> policies // 필수서류 정책 목록
    ) {
        for (RequiredDocumentPolicyProvider.RequiredDocumentPolicy policy : policies) {
            commonCodeProvider.validateEnabledCode(DOCUMENT_TYPE_GROUP, policy.documentTypeCode());
        }
    }

    private Set<String> normalizeDocumentTypes(
            Set<String> documentTypeCodes // 문서 유형 코드 목록
    ) {
        if (documentTypeCodes == null || documentTypeCodes.isEmpty()) {
            return Set.of();
        }
        return documentTypeCodes.stream()
                .map(DocumentTypeCodeNormalizer::normalize)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    // 법인 유형 코드 검증
    private String validateCorporateTypeCode(
            String corporateTypeCode // 법인 유형 코드
    ) {
        if (!StringUtils.hasText(corporateTypeCode)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return CorporateTypeCodeNormalizer.normalize(corporateTypeCode);
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
