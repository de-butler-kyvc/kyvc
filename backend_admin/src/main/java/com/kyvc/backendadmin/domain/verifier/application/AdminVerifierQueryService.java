package com.kyvc.backendadmin.domain.verifier.application;

import com.kyvc.backendadmin.domain.verifier.dto.AdminVerifierDtos;
import com.kyvc.backendadmin.domain.verifier.repository.VerifierQueryRepository;
import com.kyvc.backendadmin.domain.verifier.repository.VerifierRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Verifier 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminVerifierQueryService {

    private final VerifierRepository verifierRepository;
    private final VerifierQueryRepository verifierQueryRepository;

    /**
     * Verifier 목록을 조회합니다.
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param status 상태 코드
     * @param keyword 키워드
     * @return Verifier 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminVerifierDtos.PageResponse search(Integer page, Integer size, String status, String keyword) {
        int normalizedPage = page == null || page < 0 ? 0 : page;
        int normalizedSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        List<AdminVerifierDtos.Response> items = verifierQueryRepository.search(normalizedPage, normalizedSize, status, keyword);
        long total = verifierQueryRepository.count(status, keyword);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / normalizedSize);
        return new AdminVerifierDtos.PageResponse(items, normalizedPage, normalizedSize, total, totalPages);
    }

    /**
     * Verifier 상세를 조회합니다.
     *
     * @param verifierId Verifier ID
     * @return Verifier 상세 응답
     */
    @Transactional(readOnly = true)
    public AdminVerifierDtos.DetailResponse getDetail(Long verifierId) {
        AdminVerifierDtos.Response verifier = toResponse(findVerifier(verifierId));
        return new AdminVerifierDtos.DetailResponse(
                verifier,
                verifierQueryRepository.findCallbacks(verifierId),
                verifierQueryRepository.findApiKeys(verifierId),
                verifierQueryRepository.usageStats(verifierId, LocalDate.now().minusDays(30), LocalDate.now())
        );
    }

    VerifierRepository.VerifierRow findVerifier(Long verifierId) {
        return verifierRepository.findById(verifierId)
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_NOT_FOUND));
    }

    AdminVerifierDtos.Response toResponse(VerifierRepository.VerifierRow row) {
        return new AdminVerifierDtos.Response(row.verifierId(), row.name(), row.status(), row.contactEmail(),
                row.approvedAt(), row.suspendedAt(), row.createdAt(), row.updatedAt());
    }
}
