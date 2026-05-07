package com.kyvc.backendadmin.domain.dashboard.repository;

import com.kyvc.backendadmin.domain.dashboard.dto.AdminDashboardResponse;

/**
 * Backend Admin 대시보드 집계 QueryRepository입니다.
 */
public interface DashboardQueryRepository {

    /**
     * KYC, AI 심사, VC 발급, Core 요청 상태를 실제 DB count 기반으로 집계합니다.
     *
     * @return 대시보드 집계 응답
     */
    AdminDashboardResponse getDashboard();
}
