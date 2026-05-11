package com.kyvc.backend.domain.verifier.application;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import com.kyvc.backend.domain.verifier.dto.VerifierIntegrationLogListResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierLogQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.VerifierPrincipal;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Verifier 연동 로그 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerifierIntegrationLogService {

    private static final int DEFAULT_PAGE = 0; // 기본 페이지 번호
    private static final int DEFAULT_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_SIZE = 100; // 최대 페이지 크기

    private final VerifierLogQueryRepository verifierLogQueryRepository;

    // Verifier 연동 로그 목록 조회
    public VerifierIntegrationLogListResponse getLogs(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            Integer page, // 페이지 번호
            Integer size, // 페이지 크기
            String actionTypeCode, // 작업 유형 코드
            LocalDate from, // 시작 일자
            LocalDate to // 종료 일자
    ) {
        PageRequest pageRequest = normalizePageRequest(page, size);
        KyvcEnums.VerifierActionType actionType = parseActionType(actionTypeCode);
        DateRange dateRange = toDateRange(from, to);
        List<VerifierIntegrationLogListResponse.Item> items = verifierLogQueryRepository.findLogs(
                        principal.verifierId(),
                        actionType,
                        dateRange.from(),
                        dateRange.to(),
                        pageRequest.page(),
                        pageRequest.size()
                )
                .stream()
                .map(this::toItem)
                .toList();
        long totalElements = verifierLogQueryRepository.countLogs(
                principal.verifierId(),
                actionType,
                dateRange.from(),
                dateRange.to()
        );
        return new VerifierIntegrationLogListResponse(
                items,
                new VerifierIntegrationLogListResponse.PageInfo(
                        pageRequest.page(),
                        pageRequest.size(),
                        totalElements,
                        totalPages(totalElements, pageRequest.size())
                )
        );
    }

    private VerifierIntegrationLogListResponse.Item toItem(
            VerifierLog verifierLog // Verifier 로그
    ) {
        return new VerifierIntegrationLogListResponse.Item(
                verifierLog.getVerifierLogId(),
                verifierLog.getActionTypeCode() == null ? null : verifierLog.getActionTypeCode().name(),
                verifierLog.getResultCode(),
                verifierLog.getRequestedAt(),
                verifierLog.getRequestedAt()
        );
    }

    private KyvcEnums.VerifierActionType parseActionType(
            String actionTypeCode // 작업 유형 코드
    ) {
        if (!StringUtils.hasText(actionTypeCode)) {
            return null;
        }
        try {
            return KyvcEnums.VerifierActionType.valueOf(actionTypeCode.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private DateRange toDateRange(
            LocalDate from, // 시작 일자
            LocalDate to // 종료 일자
    ) {
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        if (fromDateTime != null && toDateTime != null && fromDateTime.isAfter(toDateTime)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return new DateRange(fromDateTime, toDateTime);
    }

    private PageRequest normalizePageRequest(
            Integer page, // 페이지 번호
            Integer size // 페이지 크기
    ) {
        int normalizedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageRequest(normalizedPage, normalizedSize);
    }

    private int totalPages(
            long totalElements, // 전체 건수
            int size // 페이지 크기
    ) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    private record PageRequest(
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
    }

    private record DateRange(
            LocalDateTime from, // 시작 일시
            LocalDateTime to // 종료 일시
    ) {
    }
}
