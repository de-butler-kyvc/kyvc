package com.kyvc.backendadmin.domain.issuer.application;

import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigDetailResponse;
import com.kyvc.backendadmin.domain.issuer.dto.IssuerConfigSummaryResponse;
import com.kyvc.backendadmin.domain.issuer.repository.IssuerConfigQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Issuer 발급 설정 조회 유스케이스를 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class IssuerConfigQueryService {

    private final IssuerConfigQueryRepository issuerConfigQueryRepository;

    /** Issuer 발급 설정 목록을 조회합니다. */
    @Transactional(readOnly = true)
    public IssuerConfigSummaryResponse search(IssuerConfigSummaryResponse.SearchRequest request) {
        List<IssuerConfigSummaryResponse.Item> items = issuerConfigQueryRepository.search(request);
        long totalElements = issuerConfigQueryRepository.count(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new IssuerConfigSummaryResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    /** Issuer 발급 설정 상세 정보를 조회합니다. */
    @Transactional(readOnly = true)
    public IssuerConfigDetailResponse getDetail(Long issuerConfigId) {
        // 없는 issuerConfigId 예외 처리: 조회 결과가 없으면 도메인 전용 ISSUER_CONFIG_NOT_FOUND로 응답한다.
        return issuerConfigQueryRepository.findDetailById(issuerConfigId)
                .orElseThrow(() -> new ApiException(ErrorCode.ISSUER_CONFIG_NOT_FOUND));
    }
}
