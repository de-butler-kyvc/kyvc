package com.kyvc.backend.domain.corporate.domain;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 법인 엔티티
@Entity
@Table(name = "corporates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Corporate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "corporate_id")
    private Long corporateId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "corporate_name", nullable = false, length = 255)
    private String corporateName;

    @Column(name = "business_registration_no", nullable = false, length = 50)
    private String businessRegistrationNo;

    @Column(name = "corporate_registration_no", length = 50)
    private String corporateRegistrationNo;

    @Column(name = "corporate_type_code", length = 50)
    private String corporateTypeCode;

    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "corporate_phone", length = 50)
    private String corporatePhone;

    @Column(name = "representative_name", length = 100)
    private String representativeName;

    @Column(name = "representative_phone", length = 50)
    private String representativePhone;

    @Column(name = "representative_email", length = 255)
    private String representativeEmail;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(name = "agent_phone", length = 50)
    private String agentPhone;

    @Column(name = "agent_email", length = 255)
    private String agentEmail;

    @Column(name = "agent_authority_scope", length = 255)
    private String agentAuthorityScope;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "corporate_status_code", nullable = false, length = 50)
    private KyvcEnums.CorporateStatus corporateStatusCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 법인 기본정보 생성
    public static Corporate create(
            Long userId, // 사용자 ID
            String corporateName, // 법인명
            String businessRegistrationNo, // 사업자등록번호
            String corporateRegistrationNo, // 법인등록번호
            String corporateTypeCode, // 법인 유형 코드
            LocalDate establishedDate, // 설립일
            String corporatePhone, // 법인 대표전화
            String representativeName, // 대표자명
            String representativePhone, // 대표자 연락처
            String representativeEmail, // 대표자 이메일
            String address, // 법인 주소
            String website, // 웹사이트 주소
            String businessType, // 업종
            KyvcEnums.CorporateStatus corporateStatusCode // 법인 상태 코드
    ) {
        Corporate corporate = new Corporate();
        corporate.userId = userId;
        corporate.corporateStatusCode = corporateStatusCode;
        corporate.updateBasicInfo(
                corporateName,
                businessRegistrationNo,
                corporateRegistrationNo,
                corporateTypeCode,
                establishedDate,
                corporatePhone,
                representativeName,
                representativePhone,
                representativeEmail,
                address,
                website,
                businessType
        );
        return corporate;
    }

    // 법인 기본정보 변경
    public void updateBasicInfo(
            String corporateName, // 법인명
            String businessRegistrationNo, // 사업자등록번호
            String corporateRegistrationNo, // 법인등록번호
            String corporateTypeCode, // 법인 유형 코드
            LocalDate establishedDate, // 설립일
            String corporatePhone, // 법인 대표전화
            String representativeName, // 대표자명
            String representativePhone, // 대표자 연락처
            String representativeEmail, // 대표자 이메일
            String address, // 법인 주소
            String website, // 웹사이트 주소
            String businessType // 업종
    ) {
        this.corporateName = corporateName;
        this.businessRegistrationNo = businessRegistrationNo;
        this.corporateRegistrationNo = corporateRegistrationNo;
        this.corporateTypeCode = corporateTypeCode;
        this.establishedDate = establishedDate;
        this.corporatePhone = corporatePhone;
        this.representativeName = representativeName;
        this.representativePhone = representativePhone;
        this.representativeEmail = representativeEmail;
        this.address = address;
        this.website = website;
        this.businessType = businessType;
    }

    // 대표자 정보 변경
    public void updateRepresentative(
            String representativeName, // 대표자명
            String representativePhone, // 대표자 연락처
            String representativeEmail // 대표자 이메일
    ) {
        this.representativeName = representativeName;
        this.representativePhone = representativePhone;
        this.representativeEmail = representativeEmail;
    }

    // 대리인 정보 변경
    public void updateAgent(
            String agentName, // 대리인명
            String agentPhone, // 대리인 연락처
            String agentEmail, // 대리인 이메일
            String agentAuthorityScope // 대리인 권한 범위
    ) {
        this.agentName = agentName;
        this.agentPhone = agentPhone;
        this.agentEmail = agentEmail;
        this.agentAuthorityScope = agentAuthorityScope;
    }

    // 금융사 방문 KYC 법인정보 변경
    public void updateFinanceVisitInfo(
            String corporateName, // 법인명
            String businessRegistrationNo, // 사업자등록번호
            String corporateRegistrationNo, // 법인등록번호
            String representativeName, // 대표자명
            String corporateTypeCode, // 법인 유형 코드
            String address // 법인 주소
    ) {
        this.corporateName = corporateName;
        this.businessRegistrationNo = businessRegistrationNo;
        this.corporateRegistrationNo = corporateRegistrationNo;
        this.representativeName = representativeName;
        this.corporateTypeCode = corporateTypeCode;
        this.address = address;
    }

    // 소유자 여부
    public boolean isOwnedBy(
            Long userId // 사용자 ID
    ) {
        return this.userId != null && this.userId.equals(userId);
    }
}
