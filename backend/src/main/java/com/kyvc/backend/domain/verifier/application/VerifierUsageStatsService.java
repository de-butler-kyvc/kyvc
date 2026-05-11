package com.kyvc.backend.domain.verifier.application;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import com.kyvc.backend.domain.verifier.dto.VerifierUsageStatsResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierLogQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.VerifierPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Verifier 사용량 통계 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerifierUsageStatsService {

    private static final String UNIT_DAY = "DAY"; // 일 단위
    private static final String UNIT_MONTH = "MONTH"; // 월 단위
    private static final String EXPORT_FORMAT_CSV = "CSV"; // CSV format
    private static final String SUCCESS_RESULT = "SUCCESS"; // 성공 결과
    private static final String CSV_HEADER = "date,actionTypeCode,totalCount,successCount,failureCount\n"; // CSV 헤더

    private final VerifierLogQueryRepository verifierLogQueryRepository;

    // Verifier 사용량 통계 조회
    public VerifierUsageStatsResponse getStats(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            LocalDate from, // 시작 일자
            LocalDate to, // 종료 일자
            String unit // 집계 단위
    ) {
        DateRange dateRange = validateRange(from, to);
        String normalizedUnit = normalizeUnit(unit);
        List<VerifierLog> logs = verifierLogQueryRepository.findLogsForStats(
                principal.verifierId(),
                dateRange.from(),
                dateRange.to()
        );
        Map<String, Counter> counters = new LinkedHashMap<>();
        logs.stream()
                .sorted(Comparator.comparing(VerifierLog::getRequestedAt))
                .forEach(log -> counters.computeIfAbsent(toBucket(log.getRequestedAt(), normalizedUnit), key -> new Counter())
                        .add(isSuccess(log)));
        List<VerifierUsageStatsResponse.Item> items = counters.entrySet().stream()
                .map(entry -> entry.getValue().toItem(entry.getKey()))
                .toList();
        Counter total = new Counter();
        counters.values().forEach(total::add);
        return new VerifierUsageStatsResponse(
                total.totalCount,
                total.successCount,
                total.failureCount,
                items
        );
    }

    // Verifier 사용량 CSV export
    public byte[] exportCsv(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            LocalDate from, // 시작 일자
            LocalDate to, // 종료 일자
            String format // export format
    ) {
        if (!EXPORT_FORMAT_CSV.equalsIgnoreCase(StringUtils.hasText(format) ? format.trim() : null)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        DateRange dateRange = validateRange(from, to);
        List<VerifierLog> logs = verifierLogQueryRepository.findLogsForStats(
                principal.verifierId(),
                dateRange.from(),
                dateRange.to()
        );
        Map<String, Map<String, Counter>> counters = new LinkedHashMap<>();
        logs.stream()
                .sorted(Comparator.comparing(VerifierLog::getRequestedAt))
                .forEach(log -> {
                    String bucket = toBucket(log.getRequestedAt(), UNIT_DAY); // CSV 기본 일 단위
                    String actionType = log.getActionTypeCode() == null ? "" : log.getActionTypeCode().name();
                    counters.computeIfAbsent(bucket, key -> new LinkedHashMap<>())
                            .computeIfAbsent(actionType, key -> new Counter())
                            .add(isSuccess(log));
                });
        StringBuilder builder = new StringBuilder(CSV_HEADER);
        counters.forEach((date, actionCounters) -> actionCounters.forEach((actionType, counter) -> builder
                .append(date)
                .append(',')
                .append(actionType)
                .append(',')
                .append(counter.totalCount)
                .append(',')
                .append(counter.successCount)
                .append(',')
                .append(counter.failureCount)
                .append('\n')));
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private DateRange validateRange(
            LocalDate from, // 시작 일자
            LocalDate to // 종료 일자
    ) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ApiException(ErrorCode.VERIFIER_USAGE_STATS_INVALID_RANGE);
        }
        return new DateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX));
    }

    private String normalizeUnit(
            String unit // 집계 단위
    ) {
        String normalized = StringUtils.hasText(unit) ? unit.trim().toUpperCase(Locale.ROOT) : UNIT_DAY;
        if (!UNIT_DAY.equals(normalized) && !UNIT_MONTH.equals(normalized)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private String toBucket(
            LocalDateTime requestedAt, // 요청 일시
            String unit // 집계 단위
    ) {
        if (requestedAt == null) {
            return "";
        }
        if (UNIT_MONTH.equals(unit)) {
            return YearMonth.from(requestedAt).toString();
        }
        return requestedAt.toLocalDate().toString();
    }

    private boolean isSuccess(
            VerifierLog log // Verifier 로그
    ) {
        return SUCCESS_RESULT.equalsIgnoreCase(log.getResultCode())
                || (log.getStatusCode() != null && log.getStatusCode() >= 200 && log.getStatusCode() < 400);
    }

    private record DateRange(
            LocalDateTime from, // 시작 일시
            LocalDateTime to // 종료 일시
    ) {
    }

    private static class Counter {

        private long totalCount; // 전체 건수
        private long successCount; // 성공 건수
        private long failureCount; // 실패 건수

        private void add(
                boolean success // 성공 여부
        ) {
            totalCount++;
            if (success) {
                successCount++;
                return;
            }
            failureCount++;
        }

        private void add(
                Counter counter // 병합 대상 counter
        ) {
            totalCount += counter.totalCount;
            successCount += counter.successCount;
            failureCount += counter.failureCount;
        }

        private VerifierUsageStatsResponse.Item toItem(
                String date // 집계 기준일
        ) {
            return new VerifierUsageStatsResponse.Item(date, totalCount, successCount, failureCount);
        }
    }
}
