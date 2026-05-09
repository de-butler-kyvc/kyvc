package com.kyvc.backendadmin.domain.core.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backendadmin.domain.core.repository.CoreRequestRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core 요청 생성 유스케이스를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class CoreRequestService {

    private static final String CORE_REQUEST_ID_PREFIX = "AI_REVIEW";
    private static final String VC_ISSUE_REQUEST_ID_PREFIX = "VC_ISSUE";

    private final CoreRequestRepository coreRequestRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI 재심사 Core 요청 row를 생성합니다.
     *
     * @param kycId KYC 신청 ID
     * @param reason AI 재심사 요청 사유
     * @param documentIds 재심사 대상 문서 ID 목록
     * @return 생성된 Core 요청 ID
     */
    public String createAiReviewRequest(Long kycId, String reason, List<Long> documentIds) {
        String coreRequestId = "%s-%d-%s".formatted(CORE_REQUEST_ID_PREFIX, kycId, UUID.randomUUID());
        String payload = toPayloadJson(Map.of(
                "kycId", kycId,
                "reason", reason,
                "documentIds", documentIds == null ? List.of() : documentIds
        ));

        saveCoreRequest(
                coreRequestId,
                KyvcEnums.CoreRequestType.AI_REVIEW,
                KyvcEnums.CoreTargetType.KYC_APPLICATION,
                kycId,
                payload
        );
        return coreRequestId;
    }

    /**
     * VC 발급 Core 요청 row를 생성합니다.
     *
     * @param credentialId Credential ID
     * @param kycId KYC 신청 ID
     * @param credentialType Credential 유형
     * @return 생성된 Core 요청 ID
     */
    public String createVcIssueRequest(Long credentialId, Long kycId, KyvcEnums.CredentialType credentialType) {
        String coreRequestId = "%s-%d-%s".formatted(VC_ISSUE_REQUEST_ID_PREFIX, credentialId, UUID.randomUUID());
        String payload = toPayloadJson(Map.of(
                "credentialId", credentialId,
                "kycId", kycId,
                "credentialType", credentialType.name()
        ));

        saveCoreRequest(
                coreRequestId,
                KyvcEnums.CoreRequestType.VC_ISSUE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                credentialId,
                payload
        );
        return coreRequestId;
    }

    /**
     * Core 요청 row를 저장합니다.
     *
     * @param coreRequestId Core 요청 ID
     * @param requestType 요청 유형
     * @param targetType 대상 유형
     * @param targetId 대상 ID
     * @param payload 요청 payload JSON
     */
    private void saveCoreRequest(
            String coreRequestId,
            KyvcEnums.CoreRequestType requestType,
            KyvcEnums.CoreTargetType targetType,
            Long targetId,
            String payload
    ) {
        try {
            // 실제 Core 실행은 후속 처리기가 담당, backend_admin은 요청 대기 row만 생성
            int insertedRows = coreRequestRepository.save(
                    coreRequestId,
                    requestType,
                    targetType,
                    targetId,
                    KyvcEnums.CoreRequestStatus.QUEUED,
                    payload,
                    LocalDateTime.now()
            );
            if (insertedRows != 1) {
                throw new ApiException(ErrorCode.CORE_REQUEST_CREATE_FAILED);
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(ErrorCode.CORE_REQUEST_CREATE_FAILED, exception);
        }
    }

    private String toPayloadJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CORE_REQUEST_CREATE_FAILED, exception);
        }
    }
}
