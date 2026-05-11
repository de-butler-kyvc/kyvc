package com.kyvc.backend.domain.kyc.application;

import com.kyvc.backend.domain.kyc.dto.KycApplicationHistoryResponse;
import com.kyvc.backend.domain.kyc.repository.KycApplicationHistoryQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

// KYC 신청 이력 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KycApplicationHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_PAGE_SIZE = 100; // 최대 페이지 크기

    private final KycApplicationHistoryQueryRepository kycApplicationHistoryQueryRepository;

    // KYC 신청 이력 조회
    public KycApplicationHistoryResponse getHistory(
            Long userId, // 사용자 ID
            String status, // KYC 상태 코드
            String keyword, // 검색어
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
        validateUserId(userId);
        String normalizedStatus = normalizeStatus(status); // 보정 KYC 상태 코드
        String normalizedKeyword = normalizeNullable(keyword); // 보정 검색어
        int normalizedPage = normalizePage(page); // 보정 페이지 번호
        int normalizedSize = normalizeSize(size); // 보정 페이지 크기

        List<KycApplicationHistoryResponse.Item> items = kycApplicationHistoryQueryRepository.search(
                userId,
                normalizedStatus,
                normalizedKeyword,
                normalizedPage,
                normalizedSize
        );
        long totalElements = kycApplicationHistoryQueryRepository.count(
                userId,
                normalizedStatus,
                normalizedKeyword
        );
        return new KycApplicationHistoryResponse(
                items,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages(totalElements, normalizedSize)
        );
    }

    // KYC 상태 코드 정규화
    private String normalizeStatus(
            String status // 원본 KYC 상태 코드
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT); // 정규화 KYC 상태 코드
        try {
            KyvcEnums.KycStatus.valueOf(normalizedStatus);
            return normalizedStatus;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
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

    // 선택 문자열 정규화
    private String normalizeNullable(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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
