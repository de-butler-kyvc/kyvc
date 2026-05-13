package com.kyvc.backendadmin.domain.verifier.repository;

import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;

import java.time.LocalDate;
import java.util.List;

/**
 * Verifier 조회 QueryRepository입니다.
 */
public interface VerifierQueryRepository {

    List<AdminVerifierDtos.Response> search(int page, int size, String status, String keyword);

    long count(String status, String keyword);

    List<AdminVerifierDtos.ApiKeyResponse> findApiKeys(Long verifierId);

    List<AdminVerifierDtos.CallbackResponse> findCallbacks(Long verifierId);

    List<AdminVerifierDtos.LogResponse> searchLogs(AdminVerifierDtos.LogSearchRequest request);

    long countLogs(AdminVerifierDtos.LogSearchRequest request);

    AdminVerifierDtos.UsageStatsResponse usageStats(Long verifierId, LocalDate fromDate, LocalDate toDate);
}
