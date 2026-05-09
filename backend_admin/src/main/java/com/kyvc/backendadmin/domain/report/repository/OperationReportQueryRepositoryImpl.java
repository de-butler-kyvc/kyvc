package com.kyvc.backendadmin.domain.report.repository;

import com.kyvc.backendadmin.domain.report.dto.AdminOperationReportDtos;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * {@link OperationReportQueryRepository}의 EntityManager 기반 구현체입니다.
 */
@Repository
@RequiredArgsConstructor
public class OperationReportQueryRepositoryImpl implements OperationReportQueryRepository {
    private final ObjectProvider<EntityManager> entityManagerProvider;

    @Override
    public AdminOperationReportDtos.Summary summary(LocalDate fromDate, LocalDate toDate) {
        return new AdminOperationReportDtos.Summary(
                count("kyc_applications", "created_at", null, fromDate, toDate),
                count("kyc_applications", "approved_at", "kyc_status_code = 'APPROVED'", fromDate, toDate),
                count("kyc_applications", "rejected_at", "kyc_status_code = 'REJECTED'", fromDate, toDate),
                count("kyc_applications", "updated_at", "kyc_status_code = 'NEED_SUPPLEMENT'", fromDate, toDate),
                count("kyc_applications", "updated_at", "ai_review_status_code = 'SUCCESS'", fromDate, toDate),
                count("kyc_applications", "updated_at", "ai_review_status_code = 'FAILED'", fromDate, toDate),
                count("kyc_applications", "updated_at", "kyc_status_code = 'MANUAL_REVIEW'", fromDate, toDate),
                count("core_requests", "requested_at", "core_request_type_code = 'VC_ISSUE'", fromDate, toDate),
                count("credentials", "issued_at", "credential_status_code = 'VALID'", fromDate, toDate),
                count("vp_verifications", "requested_at", null, fromDate, toDate),
                count("vp_verifications", "verified_at", "vp_verification_status_code = 'VALID'", fromDate, toDate),
                count("vp_verifications", "verified_at", "vp_verification_status_code in ('INVALID','FAILED','EXPIRED')", fromDate, toDate),
                count("verifier_logs", "requested_at", null, fromDate, toDate),
                count("core_requests", "requested_at", "core_request_status_code = 'FAILED'", fromDate, toDate)
        );
    }

    @Override
    public List<AdminOperationReportDtos.Row> rows(LocalDate fromDate, LocalDate toDate, String groupBy) {
        String periodExpr = switch (groupBy == null ? "DAILY" : groupBy) {
            case "WEEKLY" -> "to_char(date_trunc('week', d), 'YYYY-MM-DD')";
            case "MONTHLY" -> "to_char(date_trunc('month', d), 'YYYY-MM')";
            default -> "to_char(d, 'YYYY-MM-DD')";
        };
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em().createNativeQuery("""
                with days as (
                    select generate_series(cast(:fromDate as date), cast(:toDate as date), interval '1 day')::date d
                )
                select %s as period,
                       count(k.*), count(k.*) filter (where k.kyc_status_code='APPROVED'),
                       count(k.*) filter (where k.kyc_status_code='REJECTED'),
                       count(k.*) filter (where k.kyc_status_code='NEED_SUPPLEMENT'),
                       count(k.*) filter (where k.ai_review_status_code='SUCCESS'),
                       count(k.*) filter (where k.ai_review_status_code='FAILED'),
                       count(k.*) filter (where k.kyc_status_code='MANUAL_REVIEW'),
                       (select count(*) from core_requests cr where cr.core_request_type_code='VC_ISSUE' and cr.requested_at::date = d),
                       (select count(*) from credentials c where c.credential_status_code='VALID' and c.issued_at::date = d),
                       (select count(*) from vp_verifications vp where vp.requested_at::date = d),
                       (select count(*) from vp_verifications vp where vp.vp_verification_status_code='VALID' and vp.verified_at::date = d),
                       (select count(*) from vp_verifications vp where vp.vp_verification_status_code in ('INVALID','FAILED','EXPIRED') and vp.verified_at::date = d),
                       (select count(*) from verifier_logs vl where vl.requested_at::date = d),
                       (select count(*) from core_requests cr where cr.core_request_status_code='FAILED' and cr.requested_at::date = d)
                from days
                left join kyc_applications k on k.created_at::date = d
                group by period
                order by period
                """.formatted(periodExpr))
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .getResultList();
        return rows.stream().map(r -> new AdminOperationReportDtos.Row(str(r[0]), l(r[1]), l(r[2]), l(r[3]), l(r[4]), l(r[5]), l(r[6]), l(r[7]), l(r[8]), l(r[9]), l(r[10]), l(r[11]), l(r[12]), l(r[13]), l(r[14]))).toList();
    }

    private long count(String table, String dateColumn, String condition, LocalDate from, LocalDate to) {
        String sql = "select count(*) from " + table + " where " + dateColumn + " >= :from and " + dateColumn + " < :to";
        if (condition != null) sql += " and " + condition;
        return ((Number) em().createNativeQuery(sql).setParameter("from", from.atStartOfDay()).setParameter("to", to.plusDays(1).atStartOfDay()).getSingleResult()).longValue();
    }
    private String str(Object v) { return v == null ? null : v.toString(); }
    private long l(Object v) { return v == null ? 0L : ((Number) v).longValue(); }
    private EntityManager em() { return entityManagerProvider.getObject(); }
}
