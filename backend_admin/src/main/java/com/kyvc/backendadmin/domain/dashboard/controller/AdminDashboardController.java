package com.kyvc.backendadmin.domain.dashboard.controller;

import com.kyvc.backendadmin.domain.dashboard.application.AdminDashboardService;
import com.kyvc.backendadmin.domain.dashboard.dto.AdminDashboardResponse;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backend Admin 대시보드 집계 API를 담당합니다.
 */
@Tag(name = "Backend Admin Dashboard", description = "백엔드 관리자 대시보드 집계 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Backend Admin 대시보드 집계 정보를 조회한다.
     *
     * @return KYC, AI 심사, VC 발급, Core 요청 상태 집계 정보
     */
    @Operation(
            summary = "Backend Admin 대시보드 조회",
            description = "KYC 신청 상태, AI 심사 상태, VC 발급 상태, Core 요청 상태를 실제 DB count 기반으로 집계해 조회한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대시보드 집계 조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public CommonResponse<AdminDashboardResponse> getDashboard() {
        return CommonResponseFactory.success(adminDashboardService.getDashboard());
    }
}
