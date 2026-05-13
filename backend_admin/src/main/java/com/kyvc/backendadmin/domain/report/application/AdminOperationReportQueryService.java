package com.kyvc.backendadmin.domain.report.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.report.dto.AdminOperationReportDtos;
import com.kyvc.backendadmin.domain.report.repository.OperationReportQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
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
    private final OperationReportPdfExporter pdfExporter;

    /** 운영 리포트를 조회합니다. */
    @Transactional(readOnly = true)
    public AdminOperationReportDtos.Response get(LocalDate fromDate, LocalDate toDate, String groupBy) {
        LocalDate from = resolveFrom(fromDate);
        LocalDate to = resolveTo(toDate);
        validateDateRange(from, to);
        String group = groupBy == null ? "DAILY" : groupBy;
        return new AdminOperationReportDtos.Response(from, to, group, repository.summary(from, to), repository.rows(from, to, group));
    }

    /** 운영 리포트를 CSV 형태 DTO로 Export합니다. */
    @Transactional
    public AdminOperationReportDtos.ExportResponse export(LocalDate fromDate, LocalDate toDate, String groupBy, String format) {
        validateExportPermission();
        ReportFormat reportFormat = parseFormat(format);
        if (ReportFormat.PDF == reportFormat) {
            throw new ApiException(ErrorCode.INVALID_REPORT_FORMAT, "PDF는 파일 다운로드 응답으로 요청해야 합니다.");
        }
        if (ReportFormat.XLSX == reportFormat) {
            throw new ApiException(ErrorCode.REPORT_EXPORT_FAILED, "XLSX 내보내기는 아직 지원하지 않습니다.");
        }
        AdminOperationReportDtos.Response report = get(fromDate, toDate, groupBy);
        String header = "period,kycApplications,kycApproved,kycRejected,supplementRequested,aiReviewSuccess,aiReviewFailed,manualReviewPending,vcIssueRequested,vcIssueSuccess,vpVerificationRequested,vpVerificationSuccess,vpVerificationFailed,verifierApiCalls,coreRequestFailed";
        String body = report.rows().stream()
                .map(row -> String.join(",", row.period(), String.valueOf(row.kycApplications()), String.valueOf(row.kycApproved()), String.valueOf(row.kycRejected()), String.valueOf(row.supplementRequested()), String.valueOf(row.aiReviewSuccess()), String.valueOf(row.aiReviewFailed()), String.valueOf(row.manualReviewPending()), String.valueOf(row.vcIssueRequested()), String.valueOf(row.vcIssueSuccess()), String.valueOf(row.vpVerificationRequested()), String.valueOf(row.vpVerificationSuccess()), String.valueOf(row.vpVerificationFailed()), String.valueOf(row.verifierApiCalls()), String.valueOf(row.coreRequestFailed())))
                .collect(Collectors.joining("\n"));
        auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), "OPERATIONS_REPORT_EXPORTED", KyvcEnums.AuditTargetType.REPORT, 0L, "운영 리포트 export format=CSV", null, null);
        return new AdminOperationReportDtos.ExportResponse("operations-report.csv", header + "\n" + body);
    }

    /**
     * 운영 리포트를 PDF 바이트 배열로 Export합니다.
     *
     * @param fromDate 조회 시작일
     * @param toDate 조회 종료일
     * @return PDF 바이트 배열
     */
    @Transactional
    public byte[] exportPdf(LocalDate fromDate, LocalDate toDate) {
        validateExportPermission();
        LocalDate from = resolveFrom(fromDate);
        LocalDate to = resolveTo(toDate);
        validateDateRange(from, to);
        AdminOperationReportDtos.Response report = get(from, to, "DAILY");
        try {
            byte[] pdf = pdfExporter.export(
                    report,
                    repository.kycStatusCounts(from, to),
                    repository.credentialStatusCounts(from, to),
                    repository.vpStatusCounts(from, to),
                    repository.aiReviewStatusCounts(from, to),
                    repository.auditActionCounts(from, to)
            );
            auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), "OPERATIONS_REPORT_EXPORTED", KyvcEnums.AuditTargetType.REPORT, 0L, "운영 리포트 export format=PDF from=%s to=%s".formatted(from, to), null, null);
            return pdf;
        } catch (IllegalStateException exception) {
            throw new ApiException(ErrorCode.REPORT_EXPORT_FAILED, exception);
        }
    }

    public ReportFormat parseFormat(String format) {
        String value = StringUtils.hasText(format) ? format.trim().toUpperCase() : "CSV";
        try {
            return ReportFormat.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REPORT_FORMAT);
        }
    }

    private void validateExportPermission() {
        if (!SecurityUtil.hasRole(KyvcEnums.RoleCode.SYSTEM_ADMIN.name())
                && !SecurityUtil.hasRole(KyvcEnums.RoleCode.AUDITOR.name())) {
            throw new ApiException(ErrorCode.ADMIN_PERMISSION_DENIED);
        }
    }

    private LocalDate resolveFrom(LocalDate fromDate) {
        return fromDate == null ? LocalDate.now().minusDays(30) : fromDate;
    }

    private LocalDate resolveTo(LocalDate toDate) {
        return toDate == null ? LocalDate.now() : toDate;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ApiException(ErrorCode.INVALID_REPORT_DATE_RANGE);
        }
    }

    public enum ReportFormat {
        CSV,
        XLSX,
        PDF
    }
}
