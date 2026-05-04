package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.dto.AgentRequest;
import com.kyvc.backend.domain.corporate.dto.AgentResponse;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

// 대리인 정보 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CorporateAgentService {

    private final CorporateRepository corporateRepository;

    // 대리인 정보 저장
    public AgentResponse saveAgent(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            AgentRequest request // 대리인 정보 저장 요청
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);
        validateAgentRequest(request);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        corporate.updateAgent(
                normalizeRequired(request.name()),
                normalizeOptional(request.phoneNumber()),
                normalizeOptional(request.email()),
                normalizeOptional(request.authorityScope())
        );
        return toResponse(corporateRepository.save(corporate));
    }

    // 대리인 정보 목록 조회
    @Transactional(readOnly = true)
    public List<AgentResponse> getAgents(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        if (!StringUtils.hasText(corporate.getAgentName())) {
            return List.of();
        }
        return List.of(toResponse(corporate));
    }

    // 대리인 정보 수정
    public AgentResponse updateAgent(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long agentId, // 대리인 ID
            AgentRequest request // 대리인 정보 수정 요청
    ) {
        validateUserId(userId);
        validateCorporateId(corporateId);
        validateAgentId(agentId);
        validateAgentRelation(corporateId, agentId);
        validateAgentRequest(request);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        corporate.updateAgent(
                normalizeRequired(request.name()),
                normalizeOptional(request.phoneNumber()),
                normalizeOptional(request.email()),
                normalizeOptional(request.authorityScope())
        );
        return toResponse(corporateRepository.save(corporate));
    }

    // 대리인 정보 저장 요청 검증
    private void validateAgentRequest(
            AgentRequest request // 대리인 정보 저장 요청
    ) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 대리인과 법인 관계 검증
    private void validateAgentRelation(
            Long corporateId, // 법인 ID
            Long agentId // 대리인 ID
    ) {
        if (!corporateId.equals(agentId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 소유 법인 조회
    private Corporate getOwnedCorporate(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
        Corporate corporate = corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        if (!corporate.isOwnedBy(userId)) {
            throw new ApiException(ErrorCode.CORPORATE_ACCESS_DENIED);
        }
        return corporate;
    }

    // 대리인 응답 변환
    private AgentResponse toResponse(
            Corporate corporate // 법인 엔티티
    ) {
        return new AgentResponse(
                corporate.getCorporateId(),
                corporate.getCorporateId(),
                corporate.getAgentName(),
                corporate.getAgentPhone(),
                corporate.getAgentEmail(),
                corporate.getAgentAuthorityScope()
        );
    }

    // 필수 문자열 정규화
    private String normalizeRequired(
            String value // 원본 문자열
    ) {
        return value.trim();
    }

    // 선택 문자열 정규화
    private String normalizeOptional(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    // 대리인 ID 검증
    private void validateAgentId(
            Long agentId // 대리인 ID
    ) {
        if (agentId == null || agentId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }
}
