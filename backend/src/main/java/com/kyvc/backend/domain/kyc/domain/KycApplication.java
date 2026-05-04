package com.kyvc.backend.domain.kyc.domain;

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

import java.time.LocalDateTime;
import java.util.Set;

// KYC 신청 엔티티
@Entity
@Table(name = "kyc_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KycApplication {

    private static final Set<KyvcEnums.KycStatus> IN_PROGRESS_STATUSES = Set.of(
            KyvcEnums.KycStatus.DRAFT,
            KyvcEnums.KycStatus.SUBMITTED,
            KyvcEnums.KycStatus.AI_REVIEWING,
            KyvcEnums.KycStatus.NEED_SUPPLEMENT,
            KyvcEnums.KycStatus.MANUAL_REVIEW
    ); // 진행 중 KYC 상태 목록

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kyc_id")
    private Long kycId;

    @Column(name = "corporate_id", nullable = false)
    private Long corporateId;

    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    @Column(name = "corporate_type_code", nullable = false, length = 50)
    private String corporateTypeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status_code", nullable = false, length = 50)
    private KyvcEnums.KycStatus kycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_document_store_option_code", length = 50)
    private KyvcEnums.OriginalDocumentStoreOption originalDocumentStoreOption;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // KYC 초안 생성
    public static KycApplication createDraft(
            Long corporateId, // 법인 ID
            Long applicantUserId, // 신청 사용자 ID
            String corporateTypeCode // 법인 유형 코드
    ) {
        KycApplication kycApplication = new KycApplication();
        kycApplication.corporateId = corporateId;
        kycApplication.applicantUserId = applicantUserId;
        kycApplication.corporateTypeCode = corporateTypeCode;
        kycApplication.kycStatus = KyvcEnums.KycStatus.DRAFT;
        return kycApplication;
    }

    // 법인 유형 변경
    public void changeCorporateType(
            String corporateTypeCode // 법인 유형 코드
    ) {
        this.corporateTypeCode = corporateTypeCode;
    }

    // 원본서류 저장 옵션 변경
    public void changeDocumentStoreOption(
            KyvcEnums.OriginalDocumentStoreOption option // 원본서류 저장 옵션
    ) {
        this.originalDocumentStoreOption = option;
    }

    // KYC 제출 처리
    public void submit(
            LocalDateTime submittedAt // 제출 일시
    ) {
        this.kycStatus = KyvcEnums.KycStatus.SUBMITTED;
        this.submittedAt = submittedAt;
    }

    // 소유 여부
    public boolean isOwnedBy(
            Long userId // 사용자 ID
    ) {
        return applicantUserId != null && applicantUserId.equals(userId);
    }

    // 초안 상태 여부
    public boolean isDraft() {
        return KyvcEnums.KycStatus.DRAFT == kycStatus;
    }

    // 문서 업로드 가능 여부
    public boolean isDocumentUploadAllowed() {
        return KyvcEnums.KycStatus.DRAFT == kycStatus
                || KyvcEnums.KycStatus.NEED_SUPPLEMENT == kycStatus;
    }

    // 진행 중 KYC 여부
    public boolean isInProgress() {
        return IN_PROGRESS_STATUSES.contains(kycStatus);
    }
}
