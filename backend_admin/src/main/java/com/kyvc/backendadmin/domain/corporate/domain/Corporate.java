package com.kyvc.backendadmin.domain.corporate.domain;

import com.kyvc.backendadmin.global.util.KyvcEnums;
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

import java.time.LocalDateTime;

/**
 * corporates 테이블과 매핑되는 법인 엔티티입니다.
 *
 * <p>법인 사용자 계정과 연결된 법인명, 사업자등록번호, 대표자/대리인 정보,
 * 주소, 업종, 법인 상태를 보관합니다.</p>
 */
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

    @Column(name = "corporate_name", nullable = false)
    private String corporateName;

    @Column(name = "business_registration_no", nullable = false, length = 50)
    private String businessRegistrationNo;

    @Column(name = "corporate_registration_no", length = 50)
    private String corporateRegistrationNo;

    @Column(name = "representative_name", nullable = false, length = 100)
    private String representativeName;

    @Column(name = "representative_phone", length = 50)
    private String representativePhone;

    @Column(name = "representative_email")
    private String representativeEmail;

    @Column(name = "agent_name", length = 100)
    private String agentName;

    @Column(name = "agent_phone", length = 50)
    private String agentPhone;

    @Column(name = "agent_email")
    private String agentEmail;

    @Column(name = "agent_authority_scope")
    private String agentAuthorityScope;

    @Column(name = "address", length = 500)
    private String address;

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
}
