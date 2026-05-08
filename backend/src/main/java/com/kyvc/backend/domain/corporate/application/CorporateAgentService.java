package com.kyvc.backend.domain.corporate.application;

import com.kyvc.backend.domain.corporate.domain.CorporateAgent;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.domain.CorporateDocument;
import com.kyvc.backend.domain.corporate.dto.AgentRequest;
import com.kyvc.backend.domain.corporate.dto.AgentResponse;
import com.kyvc.backend.domain.corporate.repository.CorporateAgentRepository;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 대리인 정보 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CorporateAgentService {

    private static final String POWER_OF_ATTORNEY_DOCUMENT_TYPE = "POWER_OF_ATTORNEY"; // 위임장 문서 유형 코드

    private final CorporateRepository corporateRepository;
    private final CorporateAgentRepository corporateAgentRepository;
    private final CorporateDocumentService corporateDocumentService;

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
        Long delegationDocumentId = storeRequiredDelegationDocument(
                userId,
                corporateId,
                request.powerOfAttorneyFile()
        ); // 위임장 문서 ID
        CorporateAgent agent = CorporateAgent.create(
                corporateId,
                normalizeRequired(request.name()),
                request.birthDate(),
                normalizeOptional(request.phoneNumber()),
                normalizeOptional(request.email()),
                normalizeOptional(request.authorityScope()),
                delegationDocumentId
        );
        CorporateAgent savedAgent = saveAgentEntity(agent); // 저장된 대리인
        syncLegacyCorporateAgent(corporate, savedAgent);
        return toResponse(savedAgent);
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
        List<CorporateAgent> agents = corporateAgentRepository.findByCorporateId(corporateId); // 대리인 목록
        if (!agents.isEmpty()) {
            return agents.stream()
                    .map(this::toResponse)
                    .toList();
        }
        return legacyAgentResponse(corporate);
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
        validateAgentRequest(request);

        Corporate corporate = getOwnedCorporate(userId, corporateId);
        CorporateAgent agent = corporateAgentRepository.findById(agentId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
        validateAgentRelation(corporateId, agent);

        Long delegationDocumentId = storeDelegationDocument(
                userId,
                corporateId,
                request.powerOfAttorneyFile()
        ); // 위임장 문서 ID
        agent.update(
                normalizeRequired(request.name()),
                request.birthDate(),
                normalizeOptional(request.phoneNumber()),
                normalizeOptional(request.email()),
                normalizeOptional(request.authorityScope()),
                delegationDocumentId
        );
        CorporateAgent savedAgent = saveAgentEntity(agent); // 저장된 대리인
        syncLegacyCorporateAgent(corporate, savedAgent);
        return toResponse(savedAgent);
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
            CorporateAgent agent // 대리인 엔티티
    ) {
        if (!agent.belongsToCorporate(corporateId)) {
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
            CorporateAgent agent // 대리인 엔티티
    ) {
        return new AgentResponse(
                agent.getAgentId(),
                agent.getCorporateId(),
                agent.getAgentName(),
                agent.getAgentBirthDate(),
                agent.getAgentPhone(),
                agent.getAgentEmail(),
                agent.getAuthorityScope(),
                agent.getDelegationDocumentId()
        );
    }

    // legacy 대리인 응답 변환
    private List<AgentResponse> legacyAgentResponse(
            Corporate corporate // 법인 엔티티
    ) {
        if (!StringUtils.hasText(corporate.getAgentName())) {
            return List.of();
        }
        return List.of(new AgentResponse(
                corporate.getCorporateId(),
                corporate.getCorporateId(),
                corporate.getAgentName(),
                null,
                corporate.getAgentPhone(),
                corporate.getAgentEmail(),
                corporate.getAgentAuthorityScope(),
                null
        ));
    }

    // 대리인 Entity 저장
    private CorporateAgent saveAgentEntity(
            CorporateAgent agent // 저장 대상 대리인
    ) {
        try {
            return corporateAgentRepository.save(agent);
        } catch (DataAccessException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    // 위임장 필수 저장
    private Long storeRequiredDelegationDocument(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            MultipartFile powerOfAttorneyFile // 위임장 파일
    ) {
        if (!hasFile(powerOfAttorneyFile)) {
            throw new ApiException(ErrorCode.DOCUMENT_FILE_REQUIRED);
        }
        return storeDelegationDocument(userId, corporateId, powerOfAttorneyFile);
    }

    // 위임장 저장
    private Long storeDelegationDocument(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            MultipartFile powerOfAttorneyFile // 위임장 파일
    ) {
        if (!hasFile(powerOfAttorneyFile)) {
            return null;
        }
        CorporateDocument document = corporateDocumentService.storeCorporateDocument(
                corporateId,
                POWER_OF_ATTORNEY_DOCUMENT_TYPE,
                powerOfAttorneyFile,
                userId
        );
        return document.getCorporateDocumentId();
    }

    // legacy 법인 대리인 컬럼 동기화
    private void syncLegacyCorporateAgent(
            Corporate corporate, // 법인 엔티티
            CorporateAgent agent // 대리인 엔티티
    ) {
        corporate.updateAgent(
                agent.getAgentName(),
                agent.getAgentPhone(),
                agent.getAgentEmail(),
                agent.getAuthorityScope()
        );
        corporateRepository.save(corporate);
    }

    // 파일 포함 여부
    private boolean hasFile(
            MultipartFile file // 업로드 파일
    ) {
        return file != null && !file.isEmpty();
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
