package com.kyvc.backend.domain.vp.application;

import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.dto.UserVpPresentationListResponse;
import com.kyvc.backend.domain.vp.repository.UserVpPresentationQueryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

// 사용자 VP 제출 이력 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserVpPresentationService {

    private static final int DEFAULT_PAGE = 0; // 기본 페이지 번호
    private static final int DEFAULT_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_SIZE = 100; // 최대 페이지 크기

    private final UserVpPresentationQueryRepository userVpPresentationQueryRepository;

    // 사용자 VP 제출 이력 목록 조회
    public UserVpPresentationListResponse getPresentations(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Integer page, // 페이지 번호
            Integer size, // 페이지 크기
            String status, // 검증 상태 필터
            String verifierName // Verifier명 필터
    ) {
        Long userId = resolveUserId(userDetails);
        PageRequest pageRequest = normalizePageRequest(page, size);
        KyvcEnums.VpVerificationStatus statusFilter = parseStatus(status);
        String normalizedVerifierName = normalizeOptionalText(verifierName);
        long totalElements = userVpPresentationQueryRepository.countByUserId(
                userId,
                statusFilter,
                normalizedVerifierName
        );
        List<UserVpPresentationListResponse.Item> items = userVpPresentationQueryRepository.findByUserId(
                        userId,
                        statusFilter,
                        normalizedVerifierName,
                        pageRequest.page(),
                        pageRequest.size()
                )
                .stream()
                .map(this::toItem)
                .toList();
        return new UserVpPresentationListResponse(
                items,
                new UserVpPresentationListResponse.PageInfo(
                        pageRequest.page(),
                        pageRequest.size(),
                        totalElements,
                        totalPages(totalElements, pageRequest.size())
                )
        );
    }

    private UserVpPresentationListResponse.Item toItem(
            VpVerification vpVerification // VP 제출 이력
    ) {
        return new UserVpPresentationListResponse.Item(
                vpVerification.getVpVerificationId(),
                vpVerification.getVpRequestId(),
                vpVerification.getRequesterName(),
                vpVerification.getPurpose(),
                enumName(vpVerification.getVpVerificationStatus()),
                vpVerification.getPresentedAt()
        );
    }

    private Long resolveUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }

    private KyvcEnums.VpVerificationStatus parseStatus(
            String status // 검증 상태 문자열
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return KyvcEnums.VpVerificationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String normalizeOptionalText(
            String value // 선택 문자열
    ) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    private record PageRequest(
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
    }
}
