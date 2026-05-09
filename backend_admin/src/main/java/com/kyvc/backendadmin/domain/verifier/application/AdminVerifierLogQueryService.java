package com.kyvc.backendadmin.domain.verifier.application;

import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.domain.verifier.repository.VerifierQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Verifier 로그와 사용량 통계 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminVerifierLogQueryService {

    private final VerifierQueryRepository verifierQueryRepository;
    private final AdminVerifierQueryService queryService;

    /**
     * Verifier 로그를 조회합니다.
     *
     * @param request 검색 조건
     * @return 로그 페이지 응답
     */
    @Transactional(readOnly = true)
    public AdminVerifierDtos.LogPageResponse searchLogs(AdminVerifierDtos.LogSearchRequest request) {
        List<AdminVerifierDtos.LogResponse> items = verifierQueryRepository.searchLogs(request);
        long total = verifierQueryRepository.countLogs(request);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / request.size());
        return new AdminVerifierDtos.LogPageResponse(items, request.page(), request.size(), total, totalPages);
    }

    /**
     * Verifier 사용량 통계를 조회합니다.
     *
     * @param verifierId Verifier ID
     * @param fromDate 조회 시작일
     * @param toDate 조회 종료일
     * @return 사용량 통계
     */
    @Transactional(readOnly = true)
    public AdminVerifierDtos.UsageStatsResponse usageStats(Long verifierId, LocalDate fromDate, LocalDate toDate) {
        queryService.findVerifier(verifierId);
        return verifierQueryRepository.usageStats(verifierId, fromDate, toDate);
    }
}
