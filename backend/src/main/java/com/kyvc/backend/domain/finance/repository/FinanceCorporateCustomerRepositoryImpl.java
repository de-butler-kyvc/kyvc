package com.kyvc.backend.domain.finance.repository;

import com.kyvc.backend.domain.finance.domain.FinanceCorporateCustomer;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 금융사 법인 고객 연결 Repository 구현체
@Repository
@RequiredArgsConstructor
public class FinanceCorporateCustomerRepositoryImpl implements FinanceCorporateCustomerRepository {

    private final FinanceCorporateCustomerJpaRepository financeCorporateCustomerJpaRepository;

    // 금융사 법인 고객 연결 저장
    @Override
    public FinanceCorporateCustomer save(
            FinanceCorporateCustomer customer // 저장 대상 연결
    ) {
        return financeCorporateCustomerJpaRepository.save(customer);
    }

    // 금융기관 고객번호 존재 여부
    @Override
    public boolean existsByFinanceInstitutionAndCustomerNo(
            String financeInstitutionCode, // 금융기관 코드
            String financeCustomerNo // 금융사 고객번호
    ) {
        return financeCorporateCustomerJpaRepository.existsByFinanceInstitutionCodeAndFinanceCustomerNo(
                financeInstitutionCode,
                financeCustomerNo
        );
    }

    // 사용자 기준 최신 연결 조회
    @Override
    public Optional<FinanceCorporateCustomer> findLatestByLinkedByUserId(
            Long linkedByUserId // 연결 처리 사용자 ID
    ) {
        return financeCorporateCustomerJpaRepository.findFirstByLinkedByUserIdOrderByLinkedAtDescFinanceCustomerIdDesc(
                linkedByUserId
        );
    }

    // 금융사 직원 기준 활성 연결 조회
    @Override
    public Optional<FinanceCorporateCustomer> findActiveByCorporateCustomerAndStaff(
            Long corporateId, // 법인 ID
            String financeCustomerNo, // 금융사 고객번호
            String financeBranchCode, // 금융사 지점 코드
            Long linkedByUserId // 연결 처리 사용자 ID
    ) {
        return financeCorporateCustomerJpaRepository
                .findFirstByCorporateIdAndFinanceCustomerNoAndFinanceBranchCodeAndLinkedByUserIdAndStatusCode(
                        corporateId,
                        financeCustomerNo,
                        financeBranchCode,
                        linkedByUserId,
                        KyvcEnums.FinanceCustomerLinkStatus.ACTIVE
                );
    }
}
