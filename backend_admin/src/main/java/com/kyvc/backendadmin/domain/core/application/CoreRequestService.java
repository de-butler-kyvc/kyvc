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

    private final CoreRequestRepository coreRequestRepository;
    private final ObjectMapper objectMapper;

    /**
     * AI 심사용 Core 요청 row를 생성합니다.
     *
     * @param kycId KYC 신청 ID
     * @param reason AI 재심사 요청 사유
     * @param documentIds 재심사 대상 문서 ID 목록
     * @return 생성된 Core 요청 ID
     */
    public String createAiReviewRequest(Long kycId, String reason, List<Long> documentIds) {
        String coreRequestId = "%s-%d-%s".formatted(CORE_REQUEST_ID_PREFIX, kycId, UUID.randomUUID());
        LocalDateTime requestedAt = LocalDateTime.now();
        String payload = toPayloadJson(kycId, reason, documentIds);

        int insertedRows;
        try {
            // 실제 AI 심사는 Core에서 수행하므로 Backend Admin에서는 Core 요청 대기 row만 생성한다.
            insertedRows = coreRequestRepository.save(
                    coreRequestId,
                    KyvcEnums.CoreRequestType.AI_REVIEW,
                    KyvcEnums.CoreTargetType.KYC_APPLICATION,
                    kycId,
                    KyvcEnums.CoreRequestStatus.QUEUED,
                    payload,
                    requestedAt
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.CORE_REQUEST_CREATE_FAILED, exception);
        }
        if (insertedRows != 1) {
            throw new ApiException(ErrorCode.CORE_REQUEST_CREATE_FAILED);
        }
        return coreRequestId;
    }

    private String toPayloadJson(Long kycId, String reason, List<Long> documentIds) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "kycId", kycId,
                    "reason", reason,
                    "documentIds", documentIds == null ? List.of() : documentIds
            ));
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CORE_REQUEST_CREATE_FAILED, exception);
        }
    }
}
