package com.kyvc.backendadmin.domain.dashboard.application;

import com.kyvc.backendadmin.domain.dashboard.dto.AdminDashboardResponse;
import com.kyvc.backendadmin.domain.dashboard.repository.DashboardQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backend Admin 대시보드 집계 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final DashboardQueryRepository dashboardQueryRepository;

    /**
     * Backend Admin 대시보드 집계 정보를 조회한다.
     *
     * @return KYC, AI 심사, VC 발급, Core 요청 상태 집계 정보
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        return dashboardQueryRepository.getDashboard();
    }
}
