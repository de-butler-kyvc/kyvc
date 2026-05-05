package com.kyvc.backendadmin.domain.review.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.review.domain.KycReviewHistory;
import com.kyvc.backendadmin.domain.review.domain.KycSupplement;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class AdminReviewRepositoryImpl implements AdminReviewRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public KycReviewHistory saveReviewHistory(KycReviewHistory history) {
        entityManager().persist(history);
        return history;
    }

    @Override
    public KycSupplement saveSupplement(KycSupplement supplement) {
        entityManager().persist(supplement);
        return supplement;
    }

    @Override
    public int updateKycStatus(
            Long kycId,
            KyvcEnums.KycStatus expectedStatus,
            KyvcEnums.KycStatus targetStatus,
            String reasonCode,
            String reasonMessage
    ) {
        StringBuilder sql = new StringBuilder("""
                update kyc_applications
                set kyc_status_code = :targetStatus,
                    updated_at = :updatedAt
                """);
        if (KyvcEnums.KycStatus.APPROVED == targetStatus) {
            sql.append(", approved_at = :processedAt");
        } else if (KyvcEnums.KycStatus.REJECTED == targetStatus) {
            sql.append(", rejected_at = :processedAt, reject_reason_code = :reasonCode, reject_reason = :reasonMessage");
        } else if (KyvcEnums.KycStatus.NEED_SUPPLEMENT == targetStatus) {
            sql.append(", manual_review_reason = :reasonMessage");
        }
        sql.append(System.lineSeparator());
        sql.append("""
                where kyc_id = :kycId
                  and kyc_status_code = :expectedStatus
                """);

        var query = entityManager()
                .createNativeQuery(sql.toString())
                .setParameter("targetStatus", targetStatus.name())
                .setParameter("updatedAt", LocalDateTime.now())
                .setParameter("kycId", kycId)
                .setParameter("expectedStatus", expectedStatus.name());
        if (KyvcEnums.KycStatus.APPROVED == targetStatus) {
            query.setParameter("processedAt", LocalDateTime.now());
        } else if (KyvcEnums.KycStatus.REJECTED == targetStatus) {
            query.setParameter("processedAt", LocalDateTime.now());
            query.setParameter("reasonCode", reasonCode);
            query.setParameter("reasonMessage", reasonMessage);
        } else if (KyvcEnums.KycStatus.NEED_SUPPLEMENT == targetStatus) {
            query.setParameter("reasonMessage", reasonMessage);
        }
        return query.executeUpdate();
    }

    @Override
    public AuditLog saveAuditLog(AuditLog auditLog) {
        entityManager().persist(auditLog);
        return auditLog;
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
