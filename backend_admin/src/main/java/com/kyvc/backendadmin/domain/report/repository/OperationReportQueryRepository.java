package com.kyvc.backendadmin.domain.report.repository;

import com.kyvc.backendadmin.domain.report.dto.AdminOperationReportDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * 운영 리포트 집계 QueryRepository입니다.
 */
public interface OperationReportQueryRepository {
    AdminOperationReportDtos.Summary summary(LocalDate fromDate, LocalDate toDate);
    List<AdminOperationReportDtos.Row> rows(LocalDate fromDate, LocalDate toDate, String groupBy);
}
