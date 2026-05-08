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

// 법인 대표자 엔티티
@Entity
@Table(name = "corporate_representatives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CorporateRepresentative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "representative_id")
    private Long representativeId;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "representative_name", nullable = false, length = 100)
    private String representativeName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "nationality_code", length = 30)
    private String nationalityCode;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "identity_document_id")
    private Long identityDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_yn", nullable = false, length = 1)
    private KyvcEnums.Yn activeYn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 대표자 생성
    public static CorporateRepresentative create(
            Long corporateId, // 법인 ID
            String representativeName, // 대표자명
            LocalDate birthDate, // 생년월일
            String phone, // 대표자 연락처
            String email, // 대표자 이메일
            Long identityDocumentId // 신분증 문서 ID
    ) {
        CorporateRepresentative representative = new CorporateRepresentative();
        representative.corporateId = corporateId;
        representative.activeYn = KyvcEnums.Yn.Y;
        representative.update(representativeName, birthDate, phone, email, identityDocumentId);
        return representative;
    }

    // 대표자 정보 변경
    public void update(
            String representativeName, // 대표자명
            LocalDate birthDate, // 생년월일
            String phone, // 대표자 연락처
            String email, // 대표자 이메일
            Long identityDocumentId // 신분증 문서 ID
    ) {
        this.representativeName = representativeName;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        if (identityDocumentId != null) {
            this.identityDocumentId = identityDocumentId;
        }
    }

    // 법인 소속 여부
    public boolean belongsToCorporate(
            Long corporateId // 법인 ID
    ) {
        return this.corporateId != null && this.corporateId.equals(corporateId);
    }
}
