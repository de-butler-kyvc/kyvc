package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.audit.repository.AuditLogQueryRepository;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.dto.CorporateChangeHistoryResponse;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 법인 변경 이력 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CorporateHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_PAGE_SIZE = 100; // 최대 페이지 크기

    private final CorporateRepository corporateRepository;
    private final AuditLogQueryRepository auditLogQueryRepository;

    // 법인 변경 이력 조회
    public CorporateChangeHistoryResponse getChangeHistories(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);
        validateCorporateAccess(userId, corporateId);

        int normalizedPage = normalizePage(page); // 보정 페이지 번호
        int normalizedSize = normalizeSize(size); // 보정 페이지 크기
        List<CorporateChangeHistoryResponse.Item> items = auditLogQueryRepository.findCorporateHistories(
                corporateId,
                normalizedPage,
                normalizedSize
        );
        long totalElements = auditLogQueryRepository.countCorporateHistories(corporateId); // 전체 건수
        return new CorporateChangeHistoryResponse(
                items,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages(totalElements, normalizedSize)
        );
    }

    // 법인 접근 검증
    private void validateCorporateAccess(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        if (!corporate.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.CORPORATE_ACCESS_DENIED);
        }
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 법인 ID 검증
    private void validateCorporateId(
            Long corporateId // 법인 ID
    ) {
        if (corporateId == null || corporateId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 페이지 번호 보정
    private int normalizePage(
            int page // 원본 페이지 번호
    ) {
        return Math.max(page, 0);
    }

    // 페이지 크기 보정
    private int normalizeSize(
            int size // 원본 페이지 크기
    ) {
        if (size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    // 전체 페이지 수 산정
    private int totalPages(
            long totalElements, // 전체 건수
            int size // 페이지 크기
    ) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
