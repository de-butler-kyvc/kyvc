package com.kyvc.backendadmin.domain.review.application;

import com.kyvc.backendadmin.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backendadmin.domain.review.dto.AdminAiReviewDetailResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewAgentAuthorityResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewBeneficialOwnerResponse;
import com.kyvc.backendadmin.domain.review.dto.AiReviewMismatchResponse;
import com.kyvc.backendadmin.domain.review.dto.KycReviewHistoryResponse;
import com.kyvc.backendadmin.domain.review.repository.AiReviewQueryRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backend Admin AI 심사 결과 조회 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class AdminAiReviewService {

    private final KycApplicationRepository kycApplicationRepository;
    private final AiReviewQueryRepository aiReviewQueryRepository;

    /**
     * KYC 신청 ID를 기준으로 AI 심사 결과 상세 정보를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 심사 결과 상세 정보
     */
    @Transactional(readOnly = true)
    public AdminAiReviewDetailResponse getAiReviewDetail(Long kycId) {
        validateKycExists(kycId);
        return aiReviewQueryRepository.findAiReviewDetail(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }

    /**
     * KYC 신청 ID를 기준으로 AI 문서 불일치 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 문서 불일치 결과
     */
    @Transactional(readOnly = true)
    public AiReviewMismatchResponse getMismatches(Long kycId) {
        validateKycExists(kycId);
        // 2차 구현 단계에서는 AI 상세 결과 전용 테이블이 없으므로 빈 배열을 반환한다.
        // 추후 Core AI 상세 결과 저장 테이블이 추가되면 이 영역에서 실제 불일치 항목을 조회하도록 확장한다.
        return new AiReviewMismatchResponse(kycId, List.of());
    }

    /**
     * KYC 신청 ID를 기준으로 AI 실제소유자 판단 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 실제소유자 판단 결과
     */
    @Transactional(readOnly = true)
    public AiReviewBeneficialOwnerResponse getBeneficialOwners(Long kycId) {
        validateKycExists(kycId);
        // 2차 구현 단계에서는 AI 상세 결과 전용 테이블이 없으므로 빈 배열을 반환한다.
        // 추후 주주별 실제소유자 판단 결과 테이블이 추가되면 실제 항목 조회로 확장한다.
        return new AiReviewBeneficialOwnerResponse(kycId, List.of());
    }

    /**
     * KYC 신청 ID를 기준으로 AI 대리인 권한 판단 결과를 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return AI 대리인 권한 판단 결과
     */
    @Transactional(readOnly = true)
    public AiReviewAgentAuthorityResponse getAgentAuthority(Long kycId) {
        validateKycExists(kycId);
        // 2차 구현 단계에서는 AI 상세 결과 전용 테이블이 없으므로 법인 대리인 기본 정보와 null 판단값을 반환한다.
        // 추후 위임장/서명/직인 판단 결과 테이블이 추가되면 실제 AI 판단값을 채운다.
        AiReviewAgentAuthorityResponse.AgentAuthority agentAuthority = aiReviewQueryRepository
                .findAgentAuthorityBase(kycId)
                .orElse(new AiReviewAgentAuthorityResponse.AgentAuthority(null, null, null, null, null, null, null));
        return new AiReviewAgentAuthorityResponse(kycId, agentAuthority);
    }

    /**
     * KYC 신청 ID를 기준으로 심사 이력을 조회합니다.
     *
     * @param kycId KYC 신청 ID
     * @return KYC 심사 이력 목록
     */
    @Transactional(readOnly = true)
    public List<KycReviewHistoryResponse> getReviewHistories(Long kycId) {
        validateKycExists(kycId);
        return aiReviewQueryRepository.findReviewHistories(kycId);
    }

    private void validateKycExists(Long kycId) {
        // KYC 존재 여부 확인: 모든 AI Review 조회 API는 먼저 KYC 신청 존재 여부를 검증한다.
        kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
    }
}
