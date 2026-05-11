package com.kyvc.backend.domain.finance.application;

import com.kyvc.backend.domain.auth.repository.RoleRepository;
import com.kyvc.backend.domain.auth.repository.UserRoleRepository;
import com.kyvc.backend.domain.finance.domain.FinanceCorporateCustomer;
import com.kyvc.backend.domain.finance.dto.FinanceMeResponse;
import com.kyvc.backend.domain.finance.repository.FinanceCorporateCustomerRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 금융사 직원 컨텍스트 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceContextService {

    private static final String FINANCE_STAFF_ROLE_CODE = "FINANCE_STAFF"; // 금융사 직원 역할 코드
    private static final String SPRING_FINANCE_STAFF_ROLE_CODE = "ROLE_FINANCE_STAFF"; // Spring 권한 역할 코드
    private static final String FINANCE_CODE_PREFIX = "FINANCE_USER_"; // 임시 금융기관 코드 접두어
    private static final String SPRING_ROLE_PREFIX = "ROLE_"; // Spring 권한 접두어

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final FinanceCorporateCustomerRepository financeCorporateCustomerRepository;

    // 금융사 직원 컨텍스트 조회
    public FinanceMeResponse getMe(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        FinanceContext context = requireFinanceStaff(userDetails);
        return new FinanceMeResponse(
                context.financeInstitutionCode(),
                context.branchCode(),
                context.roles()
        );
    }

    // 금융사 직원 권한 검증
    public FinanceContext requireFinanceStaff(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        Long userId = resolveUserId(userDetails); // 인증 사용자 ID
        List<RoleRepository.RoleRow> roles = roleRepository.findEnabledRolesByUserId(userId); // 보유 역할 목록
        boolean hasFinanceStaffRole = userRoleRepository.existsEnabledUserRole(userId, FINANCE_STAFF_ROLE_CODE)
                || userRoleRepository.existsEnabledUserRole(userId, SPRING_FINANCE_STAFF_ROLE_CODE)
                || roles.stream().anyMatch(role -> isFinanceStaffRole(role.roleCode()));
        if (!hasFinanceStaffRole) {
            throw new ApiException(ErrorCode.FINANCE_STAFF_ROLE_REQUIRED);
        }

        FinanceCorporateCustomer latestLink = financeCorporateCustomerRepository
                .findLatestByLinkedByUserId(userId)
                .orElse(null); // 최신 금융사 연결
        String financeInstitutionCode = latestLink == null
                ? FINANCE_CODE_PREFIX + userId
                : latestLink.getFinanceInstitutionCode(); // 금융기관 코드
        String branchCode = latestLink == null ? null : latestLink.getFinanceBranchCode(); // 지점 코드
        return new FinanceContext(
                userId,
                financeInstitutionCode,
                branchCode,
                roles.stream()
                        .map(RoleRepository.RoleRow::roleCode)
                        .map(this::toSpringRoleCode)
                        .toList()
        );
    }

    // 금융기관 코드 접근 가능 여부 검증
    public void validateFinanceInstitutionAccess(
            FinanceContext context, // 금융사 직원 컨텍스트
            String financeInstitutionCode // 요청 금융기관 코드
    ) {
        if (!context.financeInstitutionCode().equals(financeInstitutionCode)) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
    }

    // 인증 사용자 ID 조회
    private Long resolveUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }

    // 금융사 직원 역할 여부
    private boolean isFinanceStaffRole(
            String roleCode // 역할 코드
    ) {
        return FINANCE_STAFF_ROLE_CODE.equals(roleCode) || SPRING_FINANCE_STAFF_ROLE_CODE.equals(roleCode);
    }

    // Spring 역할 코드 변환
    private String toSpringRoleCode(
            String roleCode // DB 역할 코드
    ) {
        if (roleCode == null || roleCode.startsWith(SPRING_ROLE_PREFIX)) {
            return roleCode;
        }
        return SPRING_ROLE_PREFIX + roleCode;
    }

    /**
     * 금융사 직원 컨텍스트
     *
     * @param userId 사용자 ID
     * @param financeInstitutionCode 금융기관 코드
     * @param branchCode 지점 코드
     * @param roles 보유 역할 코드 목록
     */
    public record FinanceContext(
            Long userId, // 사용자 ID
            String financeInstitutionCode, // 금융기관 코드
            String branchCode, // 지점 코드
            List<String> roles // 보유 역할 코드 목록
    ) {
    }
}
