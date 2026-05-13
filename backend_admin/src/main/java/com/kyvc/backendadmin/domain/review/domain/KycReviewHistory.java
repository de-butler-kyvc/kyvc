package com.kyvc.backendadmin.domain.review.domain;

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

import java.time.LocalDateTime;

/**
 * kyc_review_histories 테이블과 매핑되는 심사 이력 엔티티입니다.
 */
@Entity
@Table(name = "kyc_review_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KycReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_history_id")
    private Long reviewHistoryId;

    @Column(name = "kyc_id", nullable = false)
    private Long kycId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_action_type_code", nullable = false, length = 50)
    private KyvcEnums.ReviewActionType reviewActionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "before_kyc_status_code", nullable = false, length = 50)
    private KyvcEnums.KycStatus beforeKycStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_kyc_status_code", nullable = false, length = 50)
    private KyvcEnums.KycStatus afterKycStatus;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static KycReviewHistory create(
            Long kycId,
            Long adminId,
            KyvcEnums.ReviewActionType reviewActionType,
            KyvcEnums.KycStatus beforeKycStatus,
            KyvcEnums.KycStatus afterKycStatus,
            String comment
    ) {
        KycReviewHistory history = new KycReviewHistory();
        history.kycId = kycId;
        history.adminId = adminId;
        history.reviewActionType = reviewActionType;
        history.beforeKycStatus = beforeKycStatus;
        history.afterKycStatus = afterKycStatus;
        history.comment = comment;
        return history;
    }
}
