package com.kyvc.backend.domain.finance.application;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.commoncode.application.CommonCodeProvider;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.finance.domain.FinanceCorporateCustomer;
import com.kyvc.backend.domain.finance.dto.FinanceCorporateCustomerLinkRequest;
import com.kyvc.backend.domain.finance.dto.FinanceCorporateCustomerLinkResponse;
import com.kyvc.backend.domain.finance.repository.FinanceCorporateCustomerRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

// 금융사 법인 고객 연결 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class FinanceCorporateCustomerService {

    private static final String FINANCE_CUSTOMER_LINK_STATUS_GROUP = "FINANCE_CUSTOMER_LINK_STATUS"; // 금융사 고객 연결 상태 공통코드 그룹

    private final FinanceContextService financeContextService;
    private final FinanceCorporateCustomerRepository financeCorporateCustomerRepository;
    private final CorporateRepository corporateRepository;
    private final CommonCodeProvider commonCodeProvider;
    private final AuditLogService auditLogService;

    // 금융사 법인 고객 연결 생성
    public FinanceCorporateCustomerLinkResponse linkCorporateCustomer(
            CustomUserDetails userDetails, // 인증 사용자 정보
            FinanceCorporateCustomerLinkRequest request // 금융사 법인 고객 연결 요청
    ) {
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        validateRequest(request);

        String financeInstitutionCode = normalizeRequired(request.financeInstitutionCode()); // 금융기관 코드
        financeContextService.validateFinanceInstitutionAccess(context, financeInstitutionCode);
        String financeCustomerNo = normalizeRequired(request.financeCustomerNo()); // 금융사 고객번호
        Long corporateId = request.corporateId(); // 법인 ID
        corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        commonCodeProvider.validateEnabledCode(
                FINANCE_CUSTOMER_LINK_STATUS_GROUP,
                KyvcEnums.FinanceCustomerLinkStatus.ACTIVE.name()
        );

        if (financeCorporateCustomerRepository.existsByFinanceInstitutionAndCustomerNo(
                financeInstitutionCode,
                financeCustomerNo
        )) {
            throw new ApiException(ErrorCode.FINANCE_CUSTOMER_LINK_ALREADY_EXISTS);
        }

        FinanceCorporateCustomer saved = financeCorporateCustomerRepository.save(
                FinanceCorporateCustomer.createActive(
                        financeInstitutionCode,
                        normalizeOptional(request.financeBranchCode()),
                        financeCustomerNo,
                        corporateId,
                        context.userId()
                )
        );
        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                context.userId(),
                "FINANCE_CUSTOMER_LINK_CREATE",
                KyvcEnums.AuditTargetType.CORPORATE.name(),
                corporateId,
                "금융사 고객 연결 생성",
                null
        ));
        return new FinanceCorporateCustomerLinkResponse(
                true,
                saved.getFinanceCustomerId(),
                saved.getCorporateId(),
                saved.getFinanceCustomerNo(),
                saved.getStatusCode().name()
        );
    }

    // 연결 요청 검증
    private void validateRequest(
            FinanceCorporateCustomerLinkRequest request // 금융사 법인 고객 연결 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.financeInstitutionCode())
                || !StringUtils.hasText(request.financeCustomerNo())
                || request.corporateId() == null
                || request.corporateId() <= 0) {
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
}
