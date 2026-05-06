package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.dto.DocumentStoreOptionRequest;
import com.kyvc.backend.domain.kyc.dto.KycApplicationResponse;
import com.kyvc.backend.domain.kyc.dto.KycCorporateTypeRequest;
import com.kyvc.backend.domain.kyc.dto.KycStartRequest;
import com.kyvc.backend.domain.kyc.dto.KycStatusResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// KYC 신청 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class KycApplicationService {

    private static final String CORPORATE_TYPE_GROUP = "CORPORATE_TYPE"; // 법인 유형 공통코드 그룹

    private final KycApplicationRepository kycApplicationRepository;
    private final CorporateRepository corporateRepository;
    private final CommonCodeProvider commonCodeProvider;

    // KYC 신청 시작
    public KycApplicationResponse startKyc(
            Long userId, // 사용자 ID
            KycStartRequest request // KYC 신청 시작 요청
    ) {
        validateUserId(userId);
        validateKycStartRequest(request);

        String corporateTypeCode = normalizeRequired(request.corporateTypeCode()); // 법인 유형 코드
        commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, corporateTypeCode);

        Corporate corporate = findMyCorporate(userId);
        if (kycApplicationRepository.existsInProgressByApplicantUserId(userId)) {
            throw new ApiException(ErrorCode.KYC_ALREADY_IN_PROGRESS);
        }

        KycApplication kycApplication = KycApplication.createDraft(
                corporate.getCorporateId(),
                userId,
                corporateTypeCode
        );
        return toResponse(kycApplicationRepository.save(kycApplication));
    }

    // KYC 법인 유형 변경
    public KycApplicationResponse changeCorporateType(
            Long userId, // 사용자 ID
            Long kycId, // KYC 신청 ID
            KycCorporateTypeRequest request // KYC 법인 유형 변경 요청
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateCorporateTypeRequest(request);

        String corporateTypeCode = normalizeRequired(request.corporateTypeCode()); // 법인 유형 코드
        commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, corporateTypeCode);

        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }

        kycApplication.changeCorporateType(corporateTypeCode);
        return toResponse(kycApplicationRepository.save(kycApplication));
    }

    // KYC 신청 상세 조회
    @Transactional(readOnly = true)
    public KycApplicationResponse getKycApplication(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        return toResponse(findOwnedKyc(userId, kycId));
    }

    // KYC 진행상태 조회
    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        return toStatusResponse(findOwnedKyc(userId, kycId));
    }

    // 원본서류 저장 옵션 변경
    public KycApplicationResponse changeDocumentStoreOption(
            Long userId, // 사용자 ID
            Long kycId, // KYC 신청 ID
            DocumentStoreOptionRequest request // 원본서류 저장 옵션 변경 요청
    ) {
        validateUserId(userId);
        validateKycId(kycId);
        validateDocumentStoreOptionRequest(request);

        KyvcEnums.OriginalDocumentStoreOption option = parseDocumentStoreOption(request.storeOption()); // 원본서류 저장 옵션
        KycApplication kycApplication = findOwnedKyc(userId, kycId);
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.KYC_INVALID_STATUS);
        }

        kycApplication.changeDocumentStoreOption(option);
        return toResponse(kycApplicationRepository.save(kycApplication));
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

    // 사용자 법인정보 조회
    private Corporate findMyCorporate(
            Long userId // 사용자 ID
    ) {
        return corporateRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_CORPORATE_REQUIRED));
    }

    // KYC 신청 시작 요청 검증
    private void validateKycStartRequest(
            KycStartRequest request // KYC 신청 시작 요청
    ) {
        if (request == null || !StringUtils.hasText(request.corporateTypeCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // KYC 법인 유형 변경 요청 검증
    private void validateCorporateTypeRequest(
            KycCorporateTypeRequest request // KYC 법인 유형 변경 요청
    ) {
        if (request == null || !StringUtils.hasText(request.corporateTypeCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 원본서류 저장 옵션 변경 요청 검증
    private void validateDocumentStoreOptionRequest(
            DocumentStoreOptionRequest request // 원본서류 저장 옵션 변경 요청
    ) {
        if (request == null || !StringUtils.hasText(request.storeOption())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 원본서류 저장 옵션 변환
    private KyvcEnums.OriginalDocumentStoreOption parseDocumentStoreOption(
            String storeOption // 원본서류 저장 옵션
    ) {
        try {
            return KyvcEnums.OriginalDocumentStoreOption.valueOf(normalizeRequired(storeOption));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // KYC 신청 응답 변환
    private KycApplicationResponse toResponse(
            KycApplication kycApplication // KYC 신청
    ) {
        return new KycApplicationResponse(
                kycApplication.getKycId(),
                kycApplication.getCorporateId(),
                kycApplication.getApplicantUserId(),
                kycApplication.getCorporateTypeCode(),
                kycApplication.getKycStatus().name(),
                enumName(kycApplication.getOriginalDocumentStoreOption()),
                kycApplication.getSubmittedAt(),
                kycApplication.getCreatedAt(),
                kycApplication.getUpdatedAt()
        );
    }

    // KYC 진행상태 응답 변환
    private KycStatusResponse toStatusResponse(
            KycApplication kycApplication // KYC 신청
    ) {
        return new KycStatusResponse(
                kycApplication.getKycId(),
                kycApplication.getKycStatus().name(),
                kycApplication.getCorporateTypeCode(),
                enumName(kycApplication.getOriginalDocumentStoreOption()),
                kycApplication.getSubmittedAt()
        );
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
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
