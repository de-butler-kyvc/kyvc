package com.kyvc.backendadmin.domain.vp.application;

import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationDetailResponse;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationListResponse;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationSearchRequest;
import com.kyvc.backendadmin.domain.vp.dto.AdminVpVerificationSummaryResponse;
import com.kyvc.backendadmin.domain.vp.repository.VpVerificationQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backend Admin VP 검증 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminVpVerificationQueryService {

    private final VpVerificationQueryRepository vpVerificationQueryRepository;

    /**
     * VP 검증 목록을 조회합니다.
     *
     * @param request 검색 조건
     * @return VP 검증 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminVpVerificationListResponse search(AdminVpVerificationSearchRequest request) {
        List<AdminVpVerificationSummaryResponse> items = vpVerificationQueryRepository.search(request);
        long totalElements = vpVerificationQueryRepository.count(request);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / request.size());
        return new AdminVpVerificationListResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    /**
     * VP 검증 상세 정보를 조회합니다.
     *
     * @param verificationId VP 검증 ID
     * @return VP 검증 상세 응답
     */
    @Transactional(readOnly = true)
    public AdminVpVerificationDetailResponse getDetail(Long verificationId) {
        return vpVerificationQueryRepository.findDetailById(verificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.VP_VERIFICATION_NOT_FOUND));
    }
}
