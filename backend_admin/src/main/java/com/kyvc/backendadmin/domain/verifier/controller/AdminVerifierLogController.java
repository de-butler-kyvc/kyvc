package com.kyvc.backendadmin.domain.verifier.controller;

import com.kyvc.backendadmin.domain.verifier.application.AdminVerifierLogQueryService;
import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Verifier 로그와 사용량 조회 API를 제공합니다.
 */
@Tag(name = "Backend Admin Verifier Log", description = "Verifier 로그 및 사용량 통계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend")
public class AdminVerifierLogController {

    private final AdminVerifierLogQueryService logQueryService;

    @Operation(summary = "Verifier 로그 조회", description = "Verifier 연동 로그를 조건별로 조회한다.")
    @GetMapping("/verifier-logs")
    public CommonResponse<AdminVerifierDtos.LogPageResponse> searchLogs(
            @RequestParam(required = false) Long verifierId,
            @RequestParam(required = false) String actionTypeCode,
            @RequestParam(required = false) Integer statusCode,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate fromDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(required = false) Integer size
    ) {
        return CommonResponseFactory.success(logQueryService.searchLogs(
                AdminVerifierDtos.LogSearchRequest.of(verifierId, actionTypeCode, statusCode, fromDate, toDate, page, size)
        ));
    }

    @Operation(summary = "Verifier 사용량 통계 조회", description = "VP 검증 요청/성공/실패, callback 성공/실패, API Key 사용량을 조회한다.")
    @GetMapping("/verifiers/{verifierId}/usage-stats")
    public CommonResponse<AdminVerifierDtos.UsageStatsResponse> usageStats(
            @PathVariable Long verifierId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate fromDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false) LocalDate toDate
    ) {
        return CommonResponseFactory.success(logQueryService.usageStats(verifierId, fromDate, toDate));
    }
}
