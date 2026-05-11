package com.kyvc.backend.domain.finance.domain;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 금융사 법인 고객 연결 Entity
@Entity
@Table(name = "finance_corporate_customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinanceCorporateCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "finance_customer_id")
    private Long financeCustomerId;

    @Column(name = "finance_institution_code", nullable = false, length = 50)
    private String financeInstitutionCode;

    @Column(name = "finance_branch_code", length = 50)
    private String financeBranchCode;

    @Column(name = "finance_customer_no", nullable = false, length = 100)
    private String financeCustomerNo;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "linked_by_user_id")
    private Long linkedByUserId;

    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false, length = 30)
    private KyvcEnums.FinanceCustomerLinkStatus statusCode;

    // 활성 연결 생성
    public static FinanceCorporateCustomer createActive(
            String financeInstitutionCode, // 금융기관 코드
            String financeBranchCode, // 금융사 지점 코드
            String financeCustomerNo, // 금융사 고객번호
            Long corporateId, // 법인 ID
            Long linkedByUserId // 연결 처리 사용자 ID
    ) {
        FinanceCorporateCustomer customer = new FinanceCorporateCustomer();
        customer.financeInstitutionCode = financeInstitutionCode;
        customer.financeBranchCode = financeBranchCode;
        customer.financeCustomerNo = financeCustomerNo;
        customer.corporateId = corporateId;
        customer.linkedByUserId = linkedByUserId;
        customer.statusCode = KyvcEnums.FinanceCustomerLinkStatus.ACTIVE;
        return customer;
    }
}
