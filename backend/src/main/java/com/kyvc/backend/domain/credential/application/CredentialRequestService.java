package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialRequest;
import com.kyvc.backend.domain.credential.domain.CredentialStatusHistory;
import com.kyvc.backend.domain.credential.dto.CredentialReissueRequest;
import com.kyvc.backend.domain.credential.dto.CredentialRequestDetailResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRequestListResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRequestResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRequestSummaryResponse;
import com.kyvc.backend.domain.credential.dto.CredentialRevokeRequest;
import com.kyvc.backend.domain.credential.repository.CredentialQueryRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestQueryRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRequestRepository;
import com.kyvc.backend.domain.credential.repository.CredentialStatusHistoryRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Credential 요청 이력 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CredentialRequestService {

    private static final String REISSUE_UNSUPPORTED_REASON = "Core 재발급 endpoint가 확인되지 않았습니다.";

    private final CredentialRepository credentialRepository;
    private final CredentialQueryRepository credentialQueryRepository;
    private final CredentialRequestRepository credentialRequestRepository;
    private final CredentialRequestQueryRepository credentialRequestQueryRepository;
    private final CredentialStatusHistoryRepository credentialStatusHistoryRepository;
    private final CorporateRepository corporateRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // Credential 재발급 요청
    public CredentialRequestResponse requestReissue(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId, // Credential ID
            CredentialReissueRequest request // 재발급 요청
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        Credential credential = getOwnedCredential(authContext.corporateId(), credentialId);
        validateReissueAllowed(credential);
        validateNoInProgressRequest(credentialId, KyvcEnums.CredentialRequestType.REISSUE);

        CredentialRequest credentialRequest = credentialRequestRepository.save(CredentialRequest.create(
                credentialId,
                KyvcEnums.CredentialRequestType.REISSUE,
                KyvcEnums.ActorType.USER,
                authContext.userId(),
                null,
                resolveReason(request == null ? null : request.reason(), request == null ? null : request.requestMessage())
        ));
        CoreRequest coreRequest = coreRequestService.createVcReissueRequest(credentialId, toJson(createUnsupportedPayload(credential, credentialRequest)));
        coreRequestService.markRunning(coreRequest.getCoreRequestId());
        credentialRequest.markProcessing(coreRequest.getCoreRequestId());
        credentialRequest.markFailed(ErrorCode.CORE_UNSUPPORTED_OPERATION.getCode());
        credentialRequestRepository.save(credentialRequest);
        coreRequestService.markFailed(coreRequest.getCoreRequestId(), REISSUE_UNSUPPORTED_REASON);

        logEventLogger.warn(
                "credential.reissue.unsupported",
                REISSUE_UNSUPPORTED_REASON,
                createBaseLogFields(authContext.userId(), authContext.corporateId(), credentialId, credentialRequest.getCredentialRequestId())
        );

        return toRequestResponse(
                credentialRequest,
                credential,
                KyvcEnums.XrplTransactionStatus.FAILED,
                ErrorCode.CORE_UNSUPPORTED_OPERATION.getCode()
        );
    }

    // Credential 폐기 요청
    public CredentialRequestResponse requestRevoke(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialId, // Credential ID
            CredentialRevokeRequest request // 폐기 요청
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        Credential credential = getOwnedCredential(authContext.corporateId(), credentialId);
        validateRevokeAllowed(credential);
        validateNoInProgressRequest(credentialId, KyvcEnums.CredentialRequestType.REVOKE);

        CredentialRequest credentialRequest = credentialRequestRepository.save(CredentialRequest.create(
                credentialId,
                KyvcEnums.CredentialRequestType.REVOKE,
                KyvcEnums.ActorType.USER,
                authContext.userId(),
                null,
                normalizeReason(request == null ? null : request.reason())
        ));
        CoreRequest coreRequest = coreRequestService.createVcRevocationRequest(credentialId, null);
        credentialRequest.markProcessing(coreRequest.getCoreRequestId());
        credentialRequestRepository.save(credentialRequest);
        CoreRevokeCredentialRequest coreRequestDto = buildCoreRevokeCredentialRequest(credential, request);
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreRequestDto));
        coreRequestService.markRunning(coreRequest.getCoreRequestId());

        try {
            logEventLogger.info(
                    "core.call.started",
                    "Core VC revocation call started",
                    createBaseLogFields(authContext.userId(), authContext.corporateId(), credentialId, credentialRequest.getCredentialRequestId())
            );
            CoreRevokeCredentialResponse coreResponse = coreAdapter.revokeCredential(coreRequestDto);
            logEventLogger.info(
                    "core.call.completed",
                    "Core VC revocation call completed",
                    createBaseLogFields(authContext.userId(), authContext.corporateId(), credentialId, credentialRequest.getCredentialRequestId())
            );
            if (coreResponse.revoked()) {
                KyvcEnums.CredentialStatus beforeStatus = credential.getCredentialStatus();
                credential.revoke(LocalDateTime.now());
                credentialRepository.save(credential);
                saveStatusHistoryIfChanged(
                        credential.getCredentialId(),
                        beforeStatus,
                        credential.getCredentialStatus(),
                        authContext.userId(),
                        KyvcEnums.CredentialRequestType.REVOKE.name(),
                        "VC 폐기 Core 응답 반영"
                );
                credentialRequest.markCompleted(null);
                credentialRequestRepository.save(credentialRequest);
                coreRequestService.markSuccess(coreRequest.getCoreRequestId(), toJson(coreResponse));
                return toRequestResponse(credentialRequest, credential, KyvcEnums.XrplTransactionStatus.CONFIRMED, null);
            }
            credentialRequest.markFailed(ErrorCode.CORE_API_CALL_FAILED.getCode());
            credentialRequestRepository.save(credentialRequest);
            coreRequestService.markFailed(coreRequest.getCoreRequestId(), coreResponse.message());
            return toRequestResponse(credentialRequest, credential, KyvcEnums.XrplTransactionStatus.FAILED, ErrorCode.CORE_API_CALL_FAILED.getCode());
        } catch (ApiException exception) {
            markCoreRequestFailure(coreRequest.getCoreRequestId(), exception);
            credentialRequest.markFailed(exception.getErrorCode().getCode());
            credentialRequestRepository.save(credentialRequest);
            logEventLogger.warn(
                    "credential.revoke.failed",
                    exception.getMessage(),
                    createBaseLogFields(authContext.userId(), authContext.corporateId(), credentialId, credentialRequest.getCredentialRequestId())
            );
            return toRequestResponse(credentialRequest, credential, KyvcEnums.XrplTransactionStatus.FAILED, exception.getErrorCode().getCode());
        }
    }

    // Credential 요청 이력 목록 조회
    @Transactional(readOnly = true)
    public CredentialRequestListResponse getCredentialRequests(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String type, // 요청 유형 필터
            String status // 요청 상태 필터
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        KyvcEnums.CredentialRequestType requestType = parseRequestType(type);
        KyvcEnums.CredentialRequestStatus requestStatus = parseRequestStatus(status);
        List<Credential> credentials = credentialQueryRepository.findByCorporateId(authContext.corporateId(), null);
        Map<Long, Credential> credentialMap = credentials.stream()
                .collect(Collectors.toMap(Credential::getCredentialId, Function.identity()));
        List<CredentialRequestSummaryResponse> requests = credentialRequestQueryRepository
                .findByCredentialIds(credentialMap.keySet(), requestType, requestStatus)
                .stream()
                .map(request -> toSummaryResponse(request, credentialMap.get(request.getCredentialId())))
                .toList();
        return new CredentialRequestListResponse(requests, requests.size());
    }

    // Credential 요청 이력 상세 조회
    @Transactional(readOnly = true)
    public CredentialRequestDetailResponse getCredentialRequestDetail(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long credentialRequestId // Credential 요청 ID
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        CredentialRequest credentialRequest = credentialRequestRepository.getById(credentialRequestId);
        Credential credential = getOwnedCredential(authContext.corporateId(), credentialRequest.getCredentialId());
        return toDetailResponse(credentialRequest, credential);
    }

    private Credential getOwnedCredential(
            Long corporateId, // 법인 ID
            Long credentialId // Credential ID
    ) {
        validateCredentialId(credentialId);
        Credential credential = credentialRepository.getById(credentialId);
        if (!credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
        return credential;
    }

    private void validateReissueAllowed(
            Credential credential // Credential
    ) {
        if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_REISSUE_NOT_ALLOWED);
        }
    }

    private void validateRevokeAllowed(
            Credential credential // Credential
    ) {
        if (KyvcEnums.CredentialStatus.REVOKED == credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_REVOKE_NOT_ALLOWED);
        }
        if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_REVOKE_NOT_ALLOWED);
        }
    }

    private void validateNoInProgressRequest(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialRequestType requestType // 요청 유형
    ) {
        if (credentialRequestRepository.existsInProgress(credentialId, requestType)) {
            throw new ApiException(ErrorCode.CREDENTIAL_REQUEST_DUPLICATED);
        }
    }

    private CoreRevokeCredentialRequest buildCoreRevokeCredentialRequest(
            Credential credential, // Credential
            CredentialRevokeRequest request // 폐기 요청
    ) {
        return new CoreRevokeCredentialRequest(
                accountFromDid(credential.getIssuerDid()),
                credential.getHolderXrplAddress(),
                credential.getCredentialTypeCode(),
                credential.getCredentialStatusId(),
                credential.getCredentialExternalId(),
                normalizeReason(request == null ? null : request.reason())
        );
    }

    private Map<String, Object> createUnsupportedPayload(
            Credential credential, // Credential
            CredentialRequest credentialRequest // Credential 요청 이력
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("credentialId", credential.getCredentialId());
        payload.put("credentialRequestId", credentialRequest.getCredentialRequestId());
        payload.put("requestType", KyvcEnums.CredentialRequestType.REISSUE.name());
        payload.put("unsupportedReason", ErrorCode.CORE_UNSUPPORTED_OPERATION.getCode());
        return payload;
    }

    private CredentialRequestResponse toRequestResponse(
            CredentialRequest credentialRequest, // Credential 요청 이력
            Credential credential, // Credential
            KyvcEnums.XrplTransactionStatus txStatus, // 트랜잭션 상태
            String failureReason // 실패 사유 코드
    ) {
        return new CredentialRequestResponse(
                credentialRequest.getCredentialRequestId(),
                credential.getCredentialId(),
                enumName(credentialRequest.getRequestStatus()),
                enumName(credential.getCredentialStatus()),
                enumName(txStatus),
                failureReason
        );
    }

    private CredentialRequestSummaryResponse toSummaryResponse(
            CredentialRequest request, // Credential 요청 이력
            Credential credential // Credential
    ) {
        return new CredentialRequestSummaryResponse(
                request.getCredentialRequestId(),
                request.getCredentialId(),
                enumName(request.getRequestType()),
                enumName(request.getRequestStatus()),
                credential == null ? null : enumName(credential.getCredentialStatus()),
                request.getRequestedAt(),
                request.getCompletedAt()
        );
    }

    private CredentialRequestDetailResponse toDetailResponse(
            CredentialRequest request, // Credential 요청 이력
            Credential credential // Credential
    ) {
        return new CredentialRequestDetailResponse(
                request.getCredentialRequestId(),
                request.getCredentialId(),
                enumName(request.getRequestType()),
                enumName(request.getRequestStatus()),
                enumName(credential.getCredentialStatus()),
                request.getReason(),
                resolveFailureReason(request),
                request.getRequestedAt(),
                request.getCompletedAt()
        );
    }

    private String resolveFailureReason(
            CredentialRequest request // Credential 요청 이력
    ) {
        return KyvcEnums.CredentialRequestStatus.FAILED == request.getRequestStatus()
                ? request.getReasonCode()
                : null;
    }

    private void markCoreRequestFailure(
            String coreRequestId, // Core 요청 ID
            ApiException exception // Core 호출 예외
    ) {
        if (ErrorCode.CORE_API_TIMEOUT == exception.getErrorCode()) {
            coreRequestService.markTimeout(coreRequestId, exception.getMessage());
            return;
        }
        coreRequestService.markFailed(coreRequestId, exception.getMessage());
    }

    private void saveStatusHistoryIfChanged(
            Long credentialId, // Credential ID
            KyvcEnums.CredentialStatus beforeStatus, // 변경 전 상태
            KyvcEnums.CredentialStatus afterStatus, // 변경 후 상태
            Long userId, // 변경 사용자 ID
            String reasonCode, // 변경 사유 코드
            String reason // 변경 사유
    ) {
        if (afterStatus == null || beforeStatus == afterStatus) {
            return;
        }
        credentialStatusHistoryRepository.save(CredentialStatusHistory.create(
                credentialId,
                beforeStatus,
                afterStatus,
                KyvcEnums.ActorType.USER,
                userId,
                reasonCode,
                reason
        ));
    }

    private KyvcEnums.CredentialRequestType parseRequestType(
            String type // 요청 유형 문자열
    ) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        try {
            return KyvcEnums.CredentialRequestType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private KyvcEnums.CredentialRequestStatus parseRequestStatus(
            String status // 요청 상태 문자열
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return KyvcEnums.CredentialRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String resolveReason(
            String reason, // 요청 사유
            String requestMessage // 요청 메시지
    ) {
        String normalizedReason = normalizeReason(reason);
        String normalizedMessage = normalizeReason(requestMessage);
        if (StringUtils.hasText(normalizedReason) && StringUtils.hasText(normalizedMessage)) {
            return normalizedReason + " / " + normalizedMessage;
        }
        return StringUtils.hasText(normalizedReason) ? normalizedReason : normalizedMessage;
    }

    private String normalizeReason(
            String reason // 요청 사유
    ) {
        return StringUtils.hasText(reason) ? reason.trim() : null;
    }

    private String accountFromDid(
            String did // DID 문자열
    ) {
        if (!StringUtils.hasText(did)) {
            return null;
        }
        String prefix = "did:xrpl:1:";
        String normalized = did.trim();
        return normalized.startsWith(prefix) ? normalized.substring(prefix.length()) : null;
    }

    private String toJson(
            Object value // JSON 변환 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private AuthContext resolveAuthContext(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        Corporate corporate = corporateRepository.findByUserId(userDetails.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
        return new AuthContext(userDetails.getUserId(), corporate.getCorporateId());
    }

    private void validateCredentialId(
            Long credentialId // Credential ID
    ) {
        if (credentialId == null || credentialId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Map<String, Object> createBaseLogFields(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long credentialId, // Credential ID
            Long credentialRequestId // Credential 요청 ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("userId", userId);
        fields.put("corporateId", corporateId);
        fields.put("credentialId", credentialId);
        fields.put("credentialRequestId", credentialRequestId);
        return fields;
    }

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    private record AuthContext(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
    }
}
