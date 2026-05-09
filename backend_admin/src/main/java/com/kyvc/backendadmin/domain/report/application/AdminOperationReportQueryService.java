package com.kyvc.backendadmin.domain.report.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.report.dto.AdminOperationReportDtos;
import com.kyvc.backendadmin.domain.report.repository.OperationReportQueryRepository;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * 운영 리포트 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminOperationReportQueryService {
    private final OperationReportQueryRepository repository;
    private final AuditLogWriter auditLogWriter;

    /** 운영 리포트를 조회합니다. */
    @Transactional(readOnly = true)
    public AdminOperationReportDtos.Response get(LocalDate fromDate, LocalDate toDate, String groupBy) {
        LocalDate from = fromDate == null ? LocalDate.now().minusDays(30) : fromDate;
        LocalDate to = toDate == null ? LocalDate.now() : toDate;
        String group = groupBy == null ? "DAILY" : groupBy;
        return new AdminOperationReportDtos.Response(from, to, group, repository.summary(from, to), repository.rows(from, to, group));
    }

    /** 운영 리포트를 CSV 형태 DTO로 Export합니다. */
    @Transactional
    public AdminOperationReportDtos.ExportResponse export(LocalDate fromDate, LocalDate toDate, String groupBy) {
        AdminOperationReportDtos.Response report = get(fromDate, toDate, groupBy);
        String header = "period,kycApplications,kycApproved,kycRejected,supplementRequested,aiReviewSuccess,aiReviewFailed,manualReviewPending,vcIssueRequested,vcIssueSuccess,vpVerificationRequested,vpVerificationSuccess,vpVerificationFailed,verifierApiCalls,coreRequestFailed";
        String body = report.rows().stream()
                .map(row -> String.join(",", row.period(), String.valueOf(row.kycApplications()), String.valueOf(row.kycApproved()), String.valueOf(row.kycRejected()), String.valueOf(row.supplementRequested()), String.valueOf(row.aiReviewSuccess()), String.valueOf(row.aiReviewFailed()), String.valueOf(row.manualReviewPending()), String.valueOf(row.vcIssueRequested()), String.valueOf(row.vcIssueSuccess()), String.valueOf(row.vpVerificationRequested()), String.valueOf(row.vpVerificationSuccess()), String.valueOf(row.vpVerificationFailed()), String.valueOf(row.verifierApiCalls()), String.valueOf(row.coreRequestFailed())))
                .collect(Collectors.joining("\n"));
        auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), "OPERATIONS_REPORT_EXPORTED", KyvcEnums.AuditTargetType.REPORT, 0L, "운영 리포트 export", null, null);
        return new AdminOperationReportDtos.ExportResponse("operations-report.csv", header + "\n" + body);
    }
}
