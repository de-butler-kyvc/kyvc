package com.kyvc.backendadmin.domain.report.controller;

import com.kyvc.backendadmin.domain.report.application.AdminOperationReportQueryService;
import com.kyvc.backendadmin.domain.report.dto.AdminOperationReportDtos;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** 운영 리포트 API입니다. */
@Tag(name = "Backend Admin Operations Report", description = "운영 리포트 조회/Export API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/reports/operations")
public class AdminOperationReportController {
    private final AdminOperationReportQueryService service;

    @Operation(summary = "운영 리포트 조회", description = "KYC, VC, VP, Verifier, Core 요청 운영 지표를 집계한다.")
    @GetMapping
    public CommonResponse<AdminOperationReportDtos.Response> get(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate fromDate, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String groupBy) {
        return CommonResponseFactory.success(service.get(fromDate, toDate, groupBy));
    }

    @Operation(summary = "운영 리포트 Export", description = "운영 리포트를 CSV 컨텐츠 DTO로 반환하고 감사로그를 기록한다.")
    @GetMapping("/export")
    public CommonResponse<AdminOperationReportDtos.ExportResponse> export(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate fromDate, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String groupBy) {
        return CommonResponseFactory.success(service.export(fromDate, toDate, groupBy));
    }
}
