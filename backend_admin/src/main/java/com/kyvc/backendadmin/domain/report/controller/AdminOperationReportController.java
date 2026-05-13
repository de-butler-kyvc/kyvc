package com.kyvc.backendadmin.domain.report.controller;

import com.kyvc.backendadmin.domain.report.application.AdminOperationReportQueryService;
import com.kyvc.backendadmin.domain.report.dto.AdminOperationReportDtos;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    /**
     * 운영 리포트를 파일 형식으로 내보냅니다.
     *
     * <p>format=PDF인 경우 PDF 파일 바이트를 attachment 응답으로 반환합니다.
     * CSV 요청은 기존 호환성을 위해 CommonResponse 구조로 CSV 컨텐츠 DTO를 반환합니다.</p>
     *
     * @param from 조회 시작일
     * @param to 조회 종료일
     * @param fromDate 기존 호환용 조회 시작일 파라미터
     * @param toDate 기존 호환용 조회 종료일 파라미터
     * @param groupBy 그룹 기준
     * @param format 내보내기 형식
     * @return PDF 파일 응답 또는 CSV DTO 응답
     */
    @Operation(summary = "운영 리포트 내보내기", description = "운영 리포트를 CSV 또는 PDF로 내보내며, PDF는 application/pdf attachment로 반환합니다.")
    @GetMapping("/export")
    public ResponseEntity<?> export(
            @Parameter(description = "조회 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(name = "from", required = false) LocalDate from,
            @Parameter(description = "조회 종료일", example = "2026-05-12")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(name = "to", required = false) LocalDate to,
            @Parameter(description = "기존 호환용 조회 시작일", example = "2026-05-01")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "기존 호환용 조회 종료일", example = "2026-05-12")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "그룹 기준", example = "DAILY")
            @RequestParam(required = false) String groupBy,
            @Parameter(description = "내보내기 형식", example = "PDF")
            @RequestParam(required = false) String format
    ) {
        LocalDate resolvedFrom = from == null ? fromDate : from;
        LocalDate resolvedTo = to == null ? toDate : to;
        if (AdminOperationReportQueryService.ReportFormat.PDF == service.parseFormat(format)) {
            byte[] pdf = service.exportPdf(resolvedFrom, resolvedTo);
            String filename = "operations-report-%s.pdf".formatted(
                    (resolvedTo == null ? LocalDate.now() : resolvedTo).format(DateTimeFormatter.BASIC_ISO_DATE)
            );
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                    .body(pdf);
        }
        CommonResponse<AdminOperationReportDtos.ExportResponse> response = CommonResponseFactory.success(
                service.export(resolvedFrom, resolvedTo, groupBy, format)
        );
        return ResponseEntity.ok(response);
    }
}
