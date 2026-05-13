package com.kyvc.backend.domain.verifier.application;

import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import com.kyvc.backend.domain.verifier.dto.VerifierPolicySyncHistoryListResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierLogQueryRepository;
import com.kyvc.backend.global.security.VerifierPrincipal;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Verifier 정책 동기화 이력 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerifierPolicySyncHistoryService {

    private static final int DEFAULT_PAGE = 0; // 기본 페이지 번호
    private static final int DEFAULT_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_SIZE = 100; // 최대 페이지 크기

    private final VerifierLogQueryRepository verifierLogQueryRepository;

    // Verifier 정책 동기화 이력 조회
    public VerifierPolicySyncHistoryListResponse getHistories(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            Integer page, // 페이지 번호
            Integer size, // 페이지 크기
            LocalDate from, // 시작 일자
            LocalDate to // 종료 일자
    ) {
        PageRequest pageRequest = normalizePageRequest(page, size);
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.atTime(LocalTime.MAX);
        List<VerifierPolicySyncHistoryListResponse.Item> items = verifierLogQueryRepository.findLogs(
                        principal.verifierId(),
                        KyvcEnums.VerifierActionType.POLICY_SYNC,
                        fromDateTime,
                        toDateTime,
                        pageRequest.page(),
                        pageRequest.size()
                )
                .stream()
                .map(this::toItem)
                .toList();
        long totalElements = verifierLogQueryRepository.countLogs(
                principal.verifierId(),
                KyvcEnums.VerifierActionType.POLICY_SYNC,
                fromDateTime,
                toDateTime
        );
        return new VerifierPolicySyncHistoryListResponse(
                items,
                new VerifierPolicySyncHistoryListResponse.PageInfo(
                        pageRequest.page(),
                        pageRequest.size(),
                        totalElements,
                        totalPages(totalElements, pageRequest.size())
                )
        );
    }

    private VerifierPolicySyncHistoryListResponse.Item toItem(
            VerifierLog verifierLog // Verifier 로그
    ) {
        return new VerifierPolicySyncHistoryListResponse.Item(
                verifierLog.getVerifierLogId(),
                verifierLog.getResultCode(),
                verifierLog.getRequestedAt(),
                verifierLog.getErrorMessage()
        );
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
}
