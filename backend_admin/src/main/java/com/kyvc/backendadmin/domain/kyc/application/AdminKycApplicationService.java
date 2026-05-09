package com.kyvc.backendadmin.domain.kyc.application;

import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationCorporateResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationDetailResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationListResponse;
import com.kyvc.backendadmin.domain.kyc.dto.AdminKycApplicationSearchRequest;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationQueryRepository;
import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * KYC 신청 목록 조회와 신청 법인정보 조회 유스케이스를 담당합니다.
 *
 * <p>관리자가 KYC 신청 목록을 검색하고, KYC 신청 ID 기준으로 신청 법인정보를 조회하는
 * 읽기 전용 유스케이스를 처리합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminKycApplicationService {

    private final KycApplicationRepository kycApplicationRepository;
    private final KycApplicationQueryRepository kycApplicationQueryRepository;

    /**
     * KYC 신청 목록을 검색합니다.
     *
     * <p>검색 조건으로 page, size, status, keyword, submittedFrom, submittedTo,
     * aiReviewStatus, supplementYn을 지원합니다. status와 aiReviewStatus는 enum으로
     * 검증하고 supplementYn은 Y/N 값인지 확인합니다. 목록 조회이므로 개별 KYC 존재 검증은
     * 수행하지 않습니다.</p>
     *
     * @param request KYC 신청 목록 검색 조건
     * @return KYC 신청 목록 응답
     */
    @Transactional(readOnly = true)
    public AdminKycApplicationListResponse searchApplications(AdminKycApplicationSearchRequest request) {
        validateSearchRequest(request);
        List<AdminKycApplicationListResponse.Item> items = kycApplicationQueryRepository.search(request);
        long totalElements = kycApplicationQueryRepository.count(request);
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / request.size());
        return new AdminKycApplicationListResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages
        );
    }

    /**
     * KYC 신청 법인정보를 조회합니다.
     *
     * <p>kycId로 kyc_applications 단건 존재 여부를 먼저 검증하고,
     * 존재하지 않으면 KYC_NOT_FOUND 예외를 던집니다. 존재하는 신청에 대해서는
     * corporates와 users를 조인하여 신청 법인정보를 조회합니다.</p>
     *
     * @param kycId 조회할 KYC 신청 ID
     * @return KYC 신청 법인정보 응답
     */
    @Transactional(readOnly = true)
    public AdminKycApplicationCorporateResponse getApplicationCorporate(Long kycId) {
        kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        return kycApplicationQueryRepository.findCorporateByKycId(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    /**
     * KYC 신청 ID를 기준으로 관리자 심사용 상세 정보를 조회합니다.
     *
     * <p>KYC 신청 존재 여부를 먼저 확인한 뒤, 법인 정보, 제출 문서 수, 최근 Core 요청 상태,
     * Credential 발급 상태, 최근 심사 이력 요약을 QueryRepository에서 조회합니다.</p>
     *
     * @param kycId KYC 신청 ID
     * @return KYC 신청 상세 정보
     */
    @Transactional(readOnly = true)
    public AdminKycApplicationDetailResponse getApplicationDetail(Long kycId) {
        // KYC 존재 여부 확인: 없는 ID는 도메인 전용 KYC_NOT_FOUND로 변환한다.
        kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        return kycApplicationQueryRepository.findDetailByKycId(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    private void validateSearchRequest(AdminKycApplicationSearchRequest request) {
        if (StringUtils.hasText(request.status())) {
            parseKycStatus(request.status());
        }
        if (StringUtils.hasText(request.aiReviewStatus())) {
            parseAiReviewStatus(request.aiReviewStatus());
        }
        if (StringUtils.hasText(request.supplementYn()) && !isValidSupplementYn(request.supplementYn())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "supplementYn은 Y 또는 N이어야 합니다.");
        }
        if (request.submittedFrom() != null
                && request.submittedTo() != null
                && request.submittedFrom().isAfter(request.submittedTo())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "submittedFrom은 submittedTo보다 늦을 수 없습니다.");
        }
    }

    private KyvcEnums.KycStatus parseKycStatus(String status) {
        try {
            return KyvcEnums.KycStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 KYC 신청 상태입니다.");
        }
    }

    private KyvcEnums.AiReviewStatus parseAiReviewStatus(String aiReviewStatus) {
        try {
            return KyvcEnums.AiReviewStatus.valueOf(aiReviewStatus);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 AI 심사 상태입니다.");
        }
    }

    private boolean isValidSupplementYn(String supplementYn) {
        return "Y".equals(supplementYn) || "N".equals(supplementYn);
    }
}
