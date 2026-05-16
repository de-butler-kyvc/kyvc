package com.kyvc.backend.domain.kyc.repository;

import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

// KYC 심사 이력 Repository 구현체
@Repository
@RequiredArgsConstructor
public class KycReviewHistoryRepositoryImpl implements KycReviewHistoryRepository {

    private static final String SYSTEM_ADMIN_EMAIL = "system-admin@kyvc.local"; // 시스템 관리자 이메일

    private final EntityManager entityManager;

    // KYC 상태 변경 이력 저장
    @Override
    public void saveStatusChange(
            Long kycId, // KYC 신청 ID
            KyvcEnums.ReviewActionType actionType, // 심사 처리 유형
            KyvcEnums.KycStatus beforeStatus, // 변경 전 KYC 상태
            KyvcEnums.KycStatus afterStatus, // 변경 후 KYC 상태
            String comment, // 처리 의견
            LocalDateTime createdAt // 생성 일시
    ) {
        if (beforeStatus == afterStatus) {
            return;
        }

        Query query = entityManager.createNativeQuery("""
                insert into kyc_review_histories (
                    kyc_id,
                    admin_id,
                    review_action_type_code,
                    before_kyc_status_code,
                    after_kyc_status_code,
                    comment,
                    created_at
                )
                select :kycId,
                       admin_user.admin_id,
                       :actionType,
                       :beforeStatus,
                       :afterStatus,
                       :comment,
                       :createdAt
                from admin_users admin_user
                where admin_user.email = :systemAdminEmail
                """);
        query.setParameter("kycId", kycId);
        query.setParameter("actionType", actionType.name());
        query.setParameter("beforeStatus", beforeStatus.name());
        query.setParameter("afterStatus", afterStatus.name());
        query.setParameter("comment", comment);
        query.setParameter("createdAt", createdAt);
        query.setParameter("systemAdminEmail", SYSTEM_ADMIN_EMAIL);
        query.executeUpdate();
    }
}
