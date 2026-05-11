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

// 법인 대리인 엔티티
@Entity
@Table(name = "corporate_agents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorporateAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "agent_name", nullable = false, length = 100)
    private String agentName;

    @Column(name = "agent_birth_date")
    private LocalDate agentBirthDate;

    @Column(name = "agent_phone", length = 30)
    private String agentPhone;

    @Column(name = "agent_email", length = 255)
    private String agentEmail;

    @Column(name = "authority_scope", columnDefinition = "TEXT")
    private String authorityScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "authority_status_code", nullable = false, length = 30)
    private KyvcEnums.AgentAuthorityStatus authorityStatusCode;

    @Column(name = "identity_document_id")
    private Long identityDocumentId;

    @Column(name = "delegation_document_id")
    private Long delegationDocumentId;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 대리인 생성
    public static CorporateAgent create(
            Long corporateId, // 법인 ID
            String agentName, // 대리인명
            LocalDate agentBirthDate, // 대리인 생년월일
            String agentPhone, // 대리인 연락처
            String agentEmail, // 대리인 이메일
            String authorityScope, // 대리인 권한 범위
            Long delegationDocumentId // 위임장 문서 ID
    ) {
        CorporateAgent agent = new CorporateAgent();
        agent.corporateId = corporateId;
        agent.authorityStatusCode = KyvcEnums.AgentAuthorityStatus.ACTIVE;
        agent.update(agentName, agentBirthDate, agentPhone, agentEmail, authorityScope, delegationDocumentId);
        return agent;
    }

    // 대리인 정보 변경
    public void update(
            String agentName, // 대리인명
            LocalDate agentBirthDate, // 대리인 생년월일
            String agentPhone, // 대리인 연락처
            String agentEmail, // 대리인 이메일
            String authorityScope, // 대리인 권한 범위
            Long delegationDocumentId // 위임장 문서 ID
    ) {
        this.agentName = agentName;
        this.agentBirthDate = agentBirthDate;
        this.agentPhone = agentPhone;
        this.agentEmail = agentEmail;
        this.authorityScope = authorityScope;
        if (delegationDocumentId != null) {
            this.delegationDocumentId = delegationDocumentId;
        }
    }

    // 법인 소속 여부
    public boolean belongsToCorporate(
            Long corporateId // 법인 ID
    ) {
        return this.corporateId != null && this.corporateId.equals(corporateId);
    }

    // 권한 정보 변경
    public void updateAuthority(
            String authorityScope, // 권한 범위
            KyvcEnums.AgentAuthorityStatus authorityStatusCode, // 권한 상태 코드
            LocalDate validFrom, // 권한 시작일
            LocalDate validTo // 권한 종료일
    ) {
        this.authorityScope = authorityScope;
        this.authorityStatusCode = authorityStatusCode;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}
