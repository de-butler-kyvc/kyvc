package com.kyvc.backendadmin.domain.dashboard.repository;

import com.kyvc.backendadmin.domain.dashboard.dto.AdminDashboardResponse;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link DashboardQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class DashboardQueryRepositoryImpl implements DashboardQueryRepository {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public AdminDashboardResponse getDashboard() {
        // 상태별 count 조회: 각 테이블의 상태 코드별 건수를 group by로 집계한다.
        Map<String, Long> kycStatusCounts = countByStatus("kyc_applications", "kyc_status_code");
        Map<String, Long> aiReviewStatusCounts = countByStatus("kyc_applications", "ai_review_status_code");
        Map<String, Long> credentialStatusCounts = countByStatus("credentials", "credential_status_code");
        Map<String, Long> coreRequestStatusCounts = countByStatus("core_requests", "core_request_status_code");
        long totalKycCount = countAll("kyc_applications");

        // count 결과를 DTO로 변환하는 부분: 화면에서 필요한 상태만 골라 응답 구조로 조립한다.
        return new AdminDashboardResponse(
                new AdminDashboardResponse.KycSummary(
                        totalKycCount,
                        count(kycStatusCounts, KyvcEnums.KycStatus.SUBMITTED.name()),
                        count(kycStatusCounts, KyvcEnums.KycStatus.AI_REVIEWING.name()),
                        count(kycStatusCounts, KyvcEnums.KycStatus.MANUAL_REVIEW.name()),
                        count(kycStatusCounts, KyvcEnums.KycStatus.NEED_SUPPLEMENT.name()),
                        count(kycStatusCounts, KyvcEnums.KycStatus.APPROVED.name()),
                        count(kycStatusCounts, KyvcEnums.KycStatus.REJECTED.name())
                ),
                new AdminDashboardResponse.AiReviewSummary(
                        count(aiReviewStatusCounts, KyvcEnums.AiReviewStatus.QUEUED.name()),
                        count(aiReviewStatusCounts, KyvcEnums.AiReviewStatus.RUNNING.name()),
                        count(aiReviewStatusCounts, KyvcEnums.AiReviewStatus.SUCCESS.name()),
                        count(aiReviewStatusCounts, KyvcEnums.AiReviewStatus.FAILED.name()),
                        count(aiReviewStatusCounts, KyvcEnums.AiReviewStatus.LOW_CONFIDENCE.name())
                ),
                new AdminDashboardResponse.VcSummary(
                        count(credentialStatusCounts, KyvcEnums.CredentialStatus.ISSUING.name()),
                        count(credentialStatusCounts, KyvcEnums.CredentialStatus.VALID.name()),
                        count(credentialStatusCounts, "FAILED"),
                        count(credentialStatusCounts, KyvcEnums.CredentialStatus.REVOKED.name())
                ),
                new AdminDashboardResponse.CoreRequestSummary(
                        count(coreRequestStatusCounts, KyvcEnums.CoreRequestStatus.QUEUED.name()),
                        count(coreRequestStatusCounts, KyvcEnums.CoreRequestStatus.PROCESSING.name()),
                        count(coreRequestStatusCounts, KyvcEnums.CoreRequestStatus.SUCCESS.name()),
                        count(coreRequestStatusCounts, KyvcEnums.CoreRequestStatus.FAILED.name())
                )
        );
    }

    private Map<String, Long> countByStatus(String tableName, String statusColumn) {
        Query query = entityManager().createNativeQuery("""
                select %s, count(*)
                from %s
                where %s is not null
                group by %s
                """.formatted(statusColumn, tableName, statusColumn, statusColumn));
        List<?> rows = query.getResultList();
        Map<String, Long> counts = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            counts.put(columns[0].toString(), ((Number) columns[1]).longValue());
        }
        return counts;
    }

    private long countAll(String tableName) {
        return ((Number) entityManager()
                .createNativeQuery("select count(*) from " + tableName)
                .getSingleResult())
                .longValue();
    }

    private long count(Map<String, Long> counts, String status) {
        // DB에 특정 상태값이 존재하지 않더라도 프론트엔드에서 안정적으로 렌더링할 수 있도록 0으로 보정한다.
        return counts.getOrDefault(status, 0L);
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
