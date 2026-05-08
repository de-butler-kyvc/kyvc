package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.dto.CorporateBasicInfoRequest;
import com.kyvc.backend.domain.corporate.dto.CorporateCreateRequest;
import com.kyvc.backend.domain.corporate.dto.CorporateResponse;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 법인 기본정보 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CorporateService {

    private static final String CORPORATE_TYPE_GROUP = "CORPORATE_TYPE"; // 법인 유형 공통코드 그룹

    private final CorporateRepository corporateRepository;
    private final CommonCodeProvider commonCodeProvider;

    // 법인 기본정보 최초 등록
    public CorporateResponse createCorporate(
            Long userId, // 사용자 ID
            CorporateCreateRequest request // 법인 기본정보 최초 등록 요청
    ) {
        validateUserId(userId);
        validateCorporateCreateRequest(request);

        String businessRegistrationNo = normalizeRequired(request.businessRegistrationNo()); // 사업자등록번호
        if (corporateRepository.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE);
        }
        if (corporateRepository.existsByBusinessRegistrationNo(businessRegistrationNo)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE);
        }

        String corporateTypeCode = normalizeOptional(request.corporateTypeCode()); // 법인 유형 코드
        validateCorporateTypeCode(corporateTypeCode);
        Corporate corporate = Corporate.create(
                userId,
                normalizeRequired(request.corporateName()),
                businessRegistrationNo,
                normalizeOptional(request.corporateRegistrationNo()),
                corporateTypeCode,
                request.establishedDate(),
                null,
                null,
                null,
                normalizeOptional(request.address()),
                normalizeOptional(request.website()),
                null,
                KyvcEnums.CorporateStatus.ACTIVE
        );
        return toResponse(corporateRepository.save(corporate));
    }

    // 내 법인정보 조회
    @Transactional(readOnly = true)
    public CorporateResponse getMyCorporate(
            Long userId // 사용자 ID
    ) {
        validateUserId(userId);
        Corporate corporate = corporateRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        return toResponse(corporate);
    }

    // 법인 기본정보 수정
    public CorporateResponse updateBasicInfo(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            CorporateBasicInfoRequest request // 법인 기본정보 수정 요청
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);
        validateCorporateBasicInfoRequest(request);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        String businessRegistrationNo = normalizeRequired(request.businessRegistrationNo()); // 사업자등록번호
        if (corporateRepository.existsByBusinessRegistrationNoAndCorporateIdNot(businessRegistrationNo, corporateId)) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE);
        }

        String corporateTypeCode = normalizeOptional(request.corporateTypeCode()); // 법인 유형 코드
        validateCorporateTypeCode(corporateTypeCode);
        corporate.updateBasicInfo(
                normalizeRequired(request.corporateName()),
                businessRegistrationNo,
                normalizeOptional(request.corporateRegistrationNo()),
                corporateTypeCode,
                request.establishedDate(),
                normalizeRequired(request.representativeName()),
                normalizeOptional(request.representativePhone()),
                normalizeOptional(request.representativeEmail()),
                normalizeOptional(request.address()),
                normalizeOptional(request.website()),
                corporate.getBusinessType()
        );
        return toResponse(corporateRepository.save(corporate));
    }

    // 법인 기본정보 최초 등록 요청 검증
    private void validateCorporateCreateRequest(
            CorporateCreateRequest request // 법인 기본정보 최초 등록 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.corporateName())
                || !StringUtils.hasText(request.businessRegistrationNo())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 법인 기본정보 수정 요청 검증
    private void validateCorporateBasicInfoRequest(
            CorporateBasicInfoRequest request // 법인 기본정보 수정 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.corporateName())
                || !StringUtils.hasText(request.businessRegistrationNo())
                || !StringUtils.hasText(request.representativeName())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 소유 법인 조회
    private Corporate getOwnedCorporate(
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

    // 법인 응답 변환
    private CorporateResponse toResponse(
            Corporate corporate // 법인 엔티티
    ) {
        return new CorporateResponse(
                corporate.getCorporateId(),
                corporate.getUserId(),
                corporate.getCorporateName(),
                corporate.getBusinessRegistrationNo(),
                corporate.getCorporateRegistrationNo(),
                corporate.getCorporateTypeCode(),
                corporate.getEstablishedDate(),
                corporate.getRepresentativeName(),
                corporate.getRepresentativePhone(),
                corporate.getRepresentativeEmail(),
                corporate.getAddress(),
                corporate.getWebsite(),
                corporate.getBusinessType(),
                corporate.getCorporateStatusCode().name(),
                corporate.getCreatedAt(),
                corporate.getUpdatedAt()
        );
    }

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
    }

    // 선택 문자열 정규화
    private String normalizeOptional(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
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

    // 법인 ID 검증
    private void validateCorporateId(
            Long corporateId // 법인 ID
    ) {
        if (corporateId == null || corporateId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 법인 유형 코드 검증
    private void validateCorporateTypeCode(
            String corporateTypeCode // 법인 유형 코드
    ) {
        if (StringUtils.hasText(corporateTypeCode)) {
            commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, corporateTypeCode);
        }
    }
}
