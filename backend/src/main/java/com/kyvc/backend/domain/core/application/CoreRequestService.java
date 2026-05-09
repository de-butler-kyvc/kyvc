package com.kyvc.backend.domain.core.application;

import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.repository.CoreRequestRepository;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Core 요청 추적 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CoreRequestService {

    private final CoreRequestRepository coreRequestRepository;
    private final CorePayloadSanitizer corePayloadSanitizer;

    // AI 심사 요청 생성
    public CoreRequest createAiReviewRequest(
            Long kycId, // KYC 요청 ID
            String requestPayloadJson // 요청 Payload JSON
    ) {
        return createRequest(
                KyvcEnums.CoreRequestType.AI_REVIEW,
                KyvcEnums.CoreTargetType.KYC_APPLICATION,
                kycId,
                requestPayloadJson
        );
    }

    // VC 발급 요청 생성
    public CoreRequest createVcIssuanceRequest(
            Long credentialId, // Credential ID
            String requestPayloadJson // 요청 Payload JSON
    ) {
        return createRequest(
                KyvcEnums.CoreRequestType.VC_ISSUE,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                credentialId,
                requestPayloadJson
        );
    }

    // VP 검증 요청 생성
    public CoreRequest createVpVerificationRequest(
            Long vpVerificationId, // VP 검증 ID
            String requestPayloadJson // 요청 Payload JSON
    ) {
        return createRequest(
                KyvcEnums.CoreRequestType.VP_VERIFY,
                KyvcEnums.CoreTargetType.VP_VERIFICATION,
                vpVerificationId,
                requestPayloadJson
        );
    }

    // XRPL 트랜잭션 요청 생성
    public CoreRequest createXrplTransactionRequest(
            Long credentialId, // Credential ID
            String requestPayloadJson // 요청 Payload JSON
    ) {
        return createRequest(
                KyvcEnums.CoreRequestType.XRPL_TX,
                KyvcEnums.CoreTargetType.CREDENTIAL,
                credentialId,
                requestPayloadJson
        );
    }

    // 요청 Payload JSON 갱신
    public CoreRequest updateRequestPayloadJson(
            String coreRequestId, // Core 요청 ID
            String requestPayloadJson // 요청 Payload JSON
    ) {
        CoreRequest coreRequest = coreRequestRepository.getById(coreRequestId);
        coreRequest.updateRequestPayloadJson(corePayloadSanitizer.sanitizePayload(requestPayloadJson));
        return coreRequestRepository.save(coreRequest);
    }

    // 요청 수신 상태 반영
    public CoreRequest markRequested(
            String coreRequestId, // Core 요청 ID
            String responsePayloadJson // 응답 Payload JSON
    ) {
        CoreRequest coreRequest = coreRequestRepository.getById(coreRequestId);
        coreRequest.markRequested(corePayloadSanitizer.sanitizePayload(responsePayloadJson));
        return coreRequestRepository.save(coreRequest);
    }

    // 동기 Core 호출 진행 상태 반영
    public CoreRequest markRunning(
            String coreRequestId // Core 요청 ID
    ) {
        CoreRequest coreRequest = coreRequestRepository.getById(coreRequestId);
        coreRequest.markRunning();
        return coreRequestRepository.save(coreRequest);
    }

    // 응답 수신 성공 상태 반영
    public CoreRequest markSuccess(
            String coreRequestId, // Core 요청 ID
            String responsePayloadJson // 성공 Payload JSON
    ) {
        CoreRequest coreRequest = coreRequestRepository.getById(coreRequestId);
        coreRequest.markSuccess(corePayloadSanitizer.sanitizePayload(responsePayloadJson));
        return coreRequestRepository.save(coreRequest);
    }

    // 응답 수신 실패 상태 반영
    public CoreRequest markFailed(
            String coreRequestId, // Core 요청 ID
            String errorMessage // 실패 메시지
    ) {
        CoreRequest coreRequest = coreRequestRepository.getById(coreRequestId);
        coreRequest.markFailed(corePayloadSanitizer.sanitizeText(errorMessage));
        return coreRequestRepository.save(coreRequest);
    }

    // Timeout 상태 반영
    public CoreRequest markTimeout(
            String coreRequestId, // Core 요청 ID
            String errorMessage // Timeout 메시지
    ) {
        CoreRequest coreRequest = coreRequestRepository.getById(coreRequestId);
        coreRequest.markTimeout(corePayloadSanitizer.sanitizeText(errorMessage));
        return coreRequestRepository.save(coreRequest);
    }

    // CoreRequest 조회
    @Transactional(readOnly = true)
    public CoreRequest getCoreRequest(
            String coreRequestId // Core 요청 ID
    ) {
        return coreRequestRepository.getById(coreRequestId);
    }

    // 완료 처리 여부 조회
    @Transactional(readOnly = true)
    public Optional<CoreRequest> findLatestVcIssuanceRequest(
            Long credentialId // Credential ID
    ) {
        return coreRequestRepository.findLatestByTarget(
                KyvcEnums.CoreTargetType.CREDENTIAL,
                credentialId,
                KyvcEnums.CoreRequestType.VC_ISSUE
        );
    }

    @Transactional(readOnly = true)
    public Optional<CoreRequest> findLatestXrplTransactionRequest(
            Long credentialId // Credential ID
    ) {
        return coreRequestRepository.findLatestByTarget(
                KyvcEnums.CoreTargetType.CREDENTIAL,
                credentialId,
                KyvcEnums.CoreRequestType.XRPL_TX
        );
    }

    // 공통 요청 생성
    private CoreRequest createRequest(
            KyvcEnums.CoreRequestType requestType, // Core 요청 유형
            KyvcEnums.CoreTargetType targetType, // Core 대상 유형
            Long targetId, // Core 대상 ID
            String requestPayloadJson // 요청 Payload JSON
    ) {
        CoreRequest coreRequest = CoreRequest.create(
                requestType,
                targetType,
                targetId,
                corePayloadSanitizer.sanitizePayload(requestPayloadJson)
        );
        return coreRequestRepository.save(coreRequest);
    }
}
