package com.kyvc.backend.domain.finance.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.application.CorporateTypeCodeNormalizer;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.finance.domain.FinanceCorporateCustomer;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationCreateRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationCreateResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationDetailResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycApplicationListResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycCorporateUpdateRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycCorporateUpdateResponse;
import com.kyvc.backend.domain.finance.repository.FinanceCorporateCustomerRepository;
import com.kyvc.backend.domain.finance.repository.FinanceKycApplicationQueryRepository;
import com.kyvc.backend.domain.finance.repository.FinanceKycApplicationRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

// 금융사 방문 KYC 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class FinanceKycApplicationService {

    private static final int DEFAULT_PAGE = 0; // 기본 페이지 번호
    private static final int DEFAULT_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_SIZE = 100; // 최대 페이지 크기
    private static final String CORPORATE_TYPE_GROUP = "CORPORATE_TYPE"; // 법인 유형 공통코드 그룹

    private final FinanceContextService financeContextService;
    private final FinanceCorporateCustomerRepository financeCorporateCustomerRepository;
    private final FinanceKycApplicationRepository financeKycApplicationRepository;
    private final FinanceKycApplicationQueryRepository financeKycApplicationQueryRepository;
    private final CorporateRepository corporateRepository;
    private final CommonCodeProvider commonCodeProvider;
    private final AuditLogService auditLogService;

    // 금융사 방문 KYC 생성
    public FinanceKycApplicationCreateResponse createApplication(
            CustomUserDetails userDetails, // 인증 사용자 정보
            FinanceKycApplicationCreateRequest request // 금융사 방문 KYC 생성 요청
    ) {
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        validateCreateRequest(request);

        String financeCustomerNo = normalizeRequired(request.financeCustomerNo()); // 금융사 고객번호
        String financeBranchCode = normalizeRequired(request.financeBranchCode()); // 금융사 지점 코드
        String corporateTypeCode = normalizeCorporateTypeCode(request.corporateTypeCode()); // 법인 유형 코드
        commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, corporateTypeCode);

        Corporate corporate = corporateRepository.findById(request.corporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        FinanceCorporateCustomer customerLink = financeCorporateCustomerRepository
                .findActiveByCorporateCustomerAndStaff(
                        corporate.getCorporateId(),
                        financeCustomerNo,
                        financeBranchCode,
                        context.userId()
                )
                .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_CUSTOMER_LINK_NOT_FOUND));

        KycApplication saved = financeKycApplicationRepository.save(KycApplication.createFinanceVisit(
                corporate.getCorporateId(),
                corporate.getUserId(),
                corporateTypeCode,
                customerLink.getFinanceInstitutionCode(),
                financeBranchCode,
                context.userId(),
                financeCustomerNo,
                LocalDateTime.now()
        ));
        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                context.userId(),
                "FINANCE_KYC_APPLICATION_CREATE",
                KyvcEnums.AuditTargetType.KYC_APPLICATION.name(),
                saved.getKycId(),
                "금융사 방문 KYC 신청 생성",
                null
        ));
        return toCreateResponse(saved);
    }

    // 금융사 방문 KYC 목록 조회
    @Transactional(readOnly = true)
    public FinanceKycApplicationListResponse getApplications(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String status, // KYC 상태 코드
            String keyword, // 검색어
            Integer page, // 페이지 번호
            Integer size // 페이지 크기
    ) {
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        String normalizedStatus = normalizeKycStatus(status); // 정규화 KYC 상태 코드
        String normalizedKeyword = normalizeOptional(keyword); // 정규화 검색어
        int normalizedPage = normalizePage(page); // 정규화 페이지 번호
        int normalizedSize = normalizeSize(size); // 정규화 페이지 크기
        long totalElements = financeKycApplicationQueryRepository.countApplications(
                context.userId(),
                normalizedStatus,
                normalizedKeyword
        );
        return new FinanceKycApplicationListResponse(
                financeKycApplicationQueryRepository.searchApplications(
                        context.userId(),
                        normalizedStatus,
                        normalizedKeyword,
                        normalizedPage,
                        normalizedSize
                ),
                new FinanceKycApplicationListResponse.PageInfo(
                        normalizedPage,
                        normalizedSize,
                        totalElements,
                        totalPages(totalElements, normalizedSize)
                )
        );
    }

    // 금융사 방문 KYC 상세 조회
    @Transactional(readOnly = true)
    public FinanceKycApplicationDetailResponse getApplication(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId // KYC 신청 ID
    ) {
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        validateKycId(kycId);
        return financeKycApplicationQueryRepository.findDetail(context.userId(), kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_KYC_NOT_FOUND));
    }

    // 금융사 방문 KYC 법인정보 수정
    public FinanceKycCorporateUpdateResponse updateCorporate(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId, // KYC 신청 ID
            FinanceKycCorporateUpdateRequest request // 금융사 방문 KYC 법인정보 수정 요청
    ) {
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        validateKycId(kycId);
        validateCorporateUpdateRequest(request);

        KycApplication kycApplication = financeKycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_KYC_NOT_FOUND));
        if (!kycApplication.isFinanceVisit()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        if (!kycApplication.isFinanceVisitByStaff(context.userId())) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        if (!kycApplication.isDraft()) {
            throw new ApiException(ErrorCode.INVALID_FINANCE_KYC_STATUS);
        }

        Corporate corporate = corporateRepository.findById(kycApplication.getCorporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        String businessRegistrationNo = normalizeRequired(request.businessRegistrationNo()); // 사업자등록번호
        if (corporateRepository.existsByBusinessRegistrationNoAndCorporateIdNot(
                businessRegistrationNo,
                corporate.getCorporateId()
        )) {
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE);
        }

        String corporateTypeCode = normalizeCorporateTypeCode(request.corporateTypeCode()); // 법인 유형 코드
        commonCodeProvider.validateEnabledCode(CORPORATE_TYPE_GROUP, corporateTypeCode);
        corporate.updateFinanceVisitInfo(
                normalizeRequired(request.corporateName()),
                businessRegistrationNo,
                normalizeOptional(request.corporateRegistrationNo()),
                normalizeRequired(request.representativeName()),
                corporateTypeCode,
                normalizeOptional(request.address())
        );
        corporateRepository.save(corporate);
        kycApplication.changeCorporateType(corporateTypeCode);
        financeKycApplicationRepository.save(kycApplication);
        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                context.userId(),
                "FINANCE_KYC_CORPORATE_UPDATE",
                KyvcEnums.AuditTargetType.CORPORATE.name(),
                corporate.getCorporateId(),
                "금융사 방문 KYC 법인정보 수정",
                null
        ));
        return new FinanceKycCorporateUpdateResponse(true, corporate.getCorporateId(), kycId);
    }

    // 생성 응답 변환
    private FinanceKycApplicationCreateResponse toCreateResponse(
            KycApplication kycApplication // KYC 신청
    ) {
        return new FinanceKycApplicationCreateResponse(
                kycApplication.getKycId(),
                kycApplication.getKycStatus().name(),
                kycApplication.getApplicationChannelCode().name(),
                kycApplication.getCorporateId()
        );
    }

    // 생성 요청 검증
    private void validateCreateRequest(
            FinanceKycApplicationCreateRequest request // 금융사 방문 KYC 생성 요청
    ) {
        if (request == null
                || request.corporateId() == null
                || request.corporateId() <= 0
                || !StringUtils.hasText(request.financeCustomerNo())
                || !StringUtils.hasText(request.financeBranchCode())
                || !StringUtils.hasText(request.corporateTypeCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 법인정보 수정 요청 검증
    private void validateCorporateUpdateRequest(
            FinanceKycCorporateUpdateRequest request // 금융사 방문 KYC 법인정보 수정 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.corporateName())
                || !StringUtils.hasText(request.businessRegistrationNo())
                || !StringUtils.hasText(request.representativeName())
                || !StringUtils.hasText(request.corporateTypeCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // KYC ID 검증
    private void validateKycId(
            Long kycId // KYC 신청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // KYC 상태 코드 정규화
    private String normalizeKycStatus(
            String status // KYC 상태 코드
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT); // 정규화 상태 코드
        try {
            return KyvcEnums.KycStatus.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
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

    private String normalizeCorporateTypeCode(
            String value // 원본 회사 유형 코드
    ) {
        return CorporateTypeCodeNormalizer.normalize(value);
    }

    // 페이지 번호 정규화
    private int normalizePage(
            Integer page // 페이지 번호
    ) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    // 페이지 크기 정규화
    private int normalizeSize(
            Integer size // 페이지 크기
    ) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    // 전체 페이지 수 계산
    private int totalPages(
            long totalElements, // 전체 건수
            int size // 페이지 크기
    ) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
