package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.finance.domain.FinanceCorporateCustomer;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 금융사 법인 고객 연결 JPA Repository
 */
public interface FinanceCorporateCustomerJpaRepository extends JpaRepository<FinanceCorporateCustomer, Long> {

    /**
     * 금융기관 고객번호 존재 여부
     *
     * @param financeInstitutionCode 금융기관 코드
     * @param financeCustomerNo 금융사 고객번호
     * @return 존재 여부
     */
    boolean existsByFinanceInstitutionCodeAndFinanceCustomerNo(
            String financeInstitutionCode, // 금융기관 코드
            String financeCustomerNo // 금융사 고객번호
    );

    /**
     * 사용자 기준 최신 연결 조회
     *
     * @param linkedByUserId 연결 처리 사용자 ID
     * @return 최신 연결 조회 결과
     */
    Optional<FinanceCorporateCustomer> findFirstByLinkedByUserIdOrderByLinkedAtDescFinanceCustomerIdDesc(
            Long linkedByUserId // 연결 처리 사용자 ID
    );

    /**
     * 금융사 직원 기준 활성 연결 조회
     *
     * @param corporateId 법인 ID
     * @param financeCustomerNo 금융사 고객번호
     * @param financeBranchCode 금융사 지점 코드
     * @param linkedByUserId 연결 처리 사용자 ID
     * @param statusCode 연결 상태 코드
     * @return 활성 연결 조회 결과
     */
    Optional<FinanceCorporateCustomer> findFirstByCorporateIdAndFinanceCustomerNoAndFinanceBranchCodeAndLinkedByUserIdAndStatusCode(
            Long corporateId, // 법인 ID
            String financeCustomerNo, // 금융사 고객번호
            String financeBranchCode, // 금융사 지점 코드
            Long linkedByUserId, // 연결 처리 사용자 ID
            KyvcEnums.FinanceCustomerLinkStatus statusCode // 연결 상태 코드
    );
}
