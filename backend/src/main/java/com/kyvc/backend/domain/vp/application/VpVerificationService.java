package com.kyvc.backend.domain.vp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.config.CoreInternalProperties;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.dto.EligibleCredentialListResponse;
import com.kyvc.backend.domain.vp.dto.EligibleCredentialResponse;
import com.kyvc.backend.domain.vp.dto.QrResolveRequest;
import com.kyvc.backend.domain.vp.dto.QrResolveResponse;
import com.kyvc.backend.domain.vp.dto.VpPresentationRequest;
import com.kyvc.backend.domain.vp.dto.VpPresentationResponse;
import com.kyvc.backend.domain.vp.dto.VpPresentationResultResponse;
import com.kyvc.backend.domain.vp.dto.VpRequestResponse;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
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
import java.util.Map;

// VP 검증 서비스
@Service
@RequiredArgsConstructor
public class VpVerificationService {

    private static final String QR_CREDENTIAL_OFFER_MESSAGE = "Credential Offer QR입니다.";
    private static final String QR_VP_REQUEST_MESSAGE = "VP 요청 QR입니다.";
    private static final String ELIGIBLE_MATCH_REASON = "동일 법인의 Wallet 저장 VALID Credential입니다.";
    private static final String VP_PRESENTATION_MESSAGE = "VP 제출이 접수되었습니다. 검증 결과는 추후 확인할 수 있습니다.";

    private final VpVerificationRepository vpVerificationRepository;
    private final CredentialRepository credentialRepository;
    private final CorporateRepository corporateRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final CoreInternalProperties coreInternalProperties;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // QR 해석
    @Transactional(readOnly = true)
    public QrResolveResponse resolveQr(
            CustomUserDetails userDetails, // 인증 사용자 정보
            QrResolveRequest request // QR 해석 요청
    ) {
        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        validateQrResolveRequest(request);

        logEventLogger.info(
                "qr.resolve.requested",
                "QR resolve requested",
                createBaseLogFields(authContext.userId(), authContext.corporateId(), null, null, null, null)
        );

        try {
            JsonNode rootNode = parseQrPayload(request.qrPayload()); // QR Payload JSON
            KyvcEnums.QrType qrType = resolveQrType(rootNode); // QR 유형

            QrResolveResponse response = switch (qrType) {
                case CREDENTIAL_OFFER -> resolveCredentialOfferQr(rootNode);
                case VP_REQUEST -> resolveVpRequestQr(rootNode);
            };

            logEventLogger.info(
                "qr.resolve.completed",
                "QR resolve completed",
                createBaseLogFields(
                    authContext.userId(),
                    authContext.corporateId(),
                    null,
                    null,
                    response.requestId(),
                    response.type()
                )
            );
            return response;
        } catch (ApiException exception) {
            logEventLogger.warn(
                    "qr.resolve.failed",
                    exception.getMessage(),
                    createBaseLogFields(authContext.userId(), authContext.corporateId(), null, null, null, null)
            );
            throw exception;
        }
    }

    // VP 요청 조회
    @Transactional(readOnly = true)
    public VpRequestResponse getVpRequest(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String requestId // VP 요청 ID
    ) {
        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        String normalizedRequestId = normalizeRequiredText(requestId); // 정규화 요청 ID
        VpVerification vpVerification = getOwnedVpRequest(authContext.corporateId(), normalizedRequestId); // 소유 VP 요청
        validateVpRequestNotExpired(vpVerification, LocalDateTime.now());

        logEventLogger.info(
                "vp.request.detail.requested",
                "VP request detail requested",
                createBaseLogFields(
                        authContext.userId(),
                        authContext.corporateId(),
                        null,
                        vpVerification.getVpVerificationId(),
                        vpVerification.getVpRequestId(),
                        enumName(vpVerification.getVpVerificationStatus())
                )
        );

        return new VpRequestResponse(
                vpVerification.getVpRequestId(),
                vpVerification.getRequesterName(),
                vpVerification.getPurpose(),
                vpVerification.getRequiredClaimsJson(),
                vpVerification.getChallenge(),
                vpVerification.getRequestNonce(),
                vpVerification.getExpiresAt(),
                enumName(vpVerification.getVpVerificationStatus())
        );
    }

    // 제출 가능 Credential 목록 조회
    @Transactional(readOnly = true)
    public EligibleCredentialListResponse getEligibleCredentials(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String requestId // VP 요청 ID
    ) {
        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        String normalizedRequestId = normalizeRequiredText(requestId); // 정규화 요청 ID
        VpVerification vpVerification = getOwnedVpRequest(authContext.corporateId(), normalizedRequestId); // 소유 VP 요청
        validateVpRequestNotExpired(vpVerification, LocalDateTime.now());

        // TODO(vp-policy): requiredClaims 정책 확정 후 Credential claim 매칭을 정교화한다.
        List<EligibleCredentialResponse> credentials = credentialRepository
                .findVpEligibleCredentialsByCorporateId(authContext.corporateId()).stream()
                .map(this::toEligibleCredentialResponse)
                .toList();

        logEventLogger.info(
                "vp.eligible-credentials.requested",
                "VP eligible credentials requested",
                createBaseLogFields(
                        authContext.userId(),
                        authContext.corporateId(),
                        null,
                        vpVerification.getVpVerificationId(),
                        vpVerification.getVpRequestId(),
                        enumName(vpVerification.getVpVerificationStatus())
                )
        );

        return new EligibleCredentialListResponse(
                vpVerification.getVpRequestId(),
                credentials,
                credentials.size()
        );
    }

    // VP 제출
    @Transactional
    public VpPresentationResponse submitPresentation(
            CustomUserDetails userDetails, // 인증 사용자 정보
            VpPresentationRequest request // VP 제출 요청
    ) {
        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        validatePresentationRequest(request);

        String normalizedRequestId = normalizeRequiredText(request.requestId()); // 정규화 요청 ID
        String normalizedNonce = normalizeRequiredText(request.nonce()); // 정규화 nonce
        String normalizedChallenge = normalizeRequiredText(request.challenge()); // 정규화 challenge
        Long credentialId = request.credentialId(); // 제출 Credential ID
        VpVerification vpVerification = getOwnedVpRequest(authContext.corporateId(), normalizedRequestId); // 소유 VP 요청

        logEventLogger.info(
                "vp.presentation.started",
                "VP presentation started",
                createBaseLogFields(
                        authContext.userId(),
                        authContext.corporateId(),
                        credentialId,
                        vpVerification.getVpVerificationId(),
                        vpVerification.getVpRequestId(),
                        enumName(vpVerification.getVpVerificationStatus())
                )
        );

        LocalDateTime now = LocalDateTime.now(); // 기준 일시
        validateVpRequestNotExpired(vpVerification, now);
        if (!vpVerification.isRequested()) {
            throw new ApiException(ErrorCode.VP_REQUEST_INVALID_STATUS);
        }
        if (!vpVerification.matchesNonce(normalizedNonce)) {
            throw new ApiException(ErrorCode.VP_NONCE_INVALID);
        }
        if (!vpVerification.matchesChallenge(normalizedChallenge)) {
            throw new ApiException(ErrorCode.VP_CHALLENGE_INVALID);
        }

        Credential credential = credentialRepository.getById(credentialId); // 제출 Credential
        validateCredentialOwnership(authContext.corporateId(), credential);
        if (!credential.isWalletSaved()) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_NOT_FOUND);
        }
        if (!credential.isValid(now)) {
            throw new ApiException(ErrorCode.VP_CREDENTIAL_NOT_ELIGIBLE);
        }

        String vpJwtHash = TokenHashUtil.sha256(request.vpJwt()); // VP JWT 해시
        logEventLogger.info(
                "vp.presentation.hash-created",
                "VP presentation hash created",
                createBaseLogFields(
                        authContext.userId(),
                        authContext.corporateId(),
                        credentialId,
                        vpVerification.getVpVerificationId(),
                        vpVerification.getVpRequestId(),
                        enumName(vpVerification.getVpVerificationStatus())
                )
        );

        if (vpVerificationRepository.existsReplayCandidate(vpVerification.getRequestNonce(), vpJwtHash)) {
            logEventLogger.warn(
                    "vp.presentation.replay-suspected",
                    "VP presentation replay suspected",
                    createBaseLogFields(
                            authContext.userId(),
                            authContext.corporateId(),
                            credentialId,
                            vpVerification.getVpVerificationId(),
                            vpVerification.getVpRequestId(),
                            enumName(vpVerification.getVpVerificationStatus())
                    )
            );
            throw new ApiException(ErrorCode.VP_PRESENTATION_REPLAY_SUSPECTED);
        }

        CoreRequest coreRequest = coreRequestService.createVpVerificationRequest(vpVerification.getVpVerificationId(), null);
        String coreRequestId = coreRequest.getCoreRequestId(); // Core 요청 ID

        logEventLogger.info(
                "vp.presentation.core-request-created",
                "VP presentation core request created",
                createBaseLogFields(
                        authContext.userId(),
                        authContext.corporateId(),
                        credentialId,
                        vpVerification.getVpVerificationId(),
                        vpVerification.getVpRequestId(),
                        coreRequestId
                )
        );

        CoreVpVerificationRequest coreVpVerificationRequest = buildCoreVpVerificationRequest(
                vpVerification,
                credential,
                coreRequestId,
                now
        ); // Core VP 검증 요청
        coreRequestService.updateRequestPayloadJson(coreRequestId, toJson(coreVpVerificationRequest));

        CoreVpVerificationResponse coreVpVerificationResponse = coreAdapter.requestVpVerification(coreVpVerificationRequest);
        coreRequestService.markRequested(coreRequestId, toJson(coreVpVerificationResponse));

        vpVerification.markPresented(
                credential.getCredentialId(),
                vpJwtHash,
                coreRequestId,
                now
        );
        VpVerification savedVpVerification = vpVerificationRepository.save(vpVerification); // 제출 반영 VP 검증

        logEventLogger.info(
                "vp.presentation.completed",
                "VP presentation completed",
                createBaseLogFields(
                        authContext.userId(),
                        authContext.corporateId(),
                        credentialId,
                        savedVpVerification.getVpVerificationId(),
                        savedVpVerification.getVpRequestId(),
                        enumName(savedVpVerification.getVpVerificationStatus())
                )
        );

        return new VpPresentationResponse(
                savedVpVerification.getVpVerificationId(),
                savedVpVerification.getVpRequestId(),
                savedVpVerification.getCredentialId(),
                enumName(savedVpVerification.getVpVerificationStatus()),
                savedVpVerification.getPresentedAt(),
                VP_PRESENTATION_MESSAGE
        );
    }

    // VP 제출 결과 조회
    @Transactional(readOnly = true)
    public VpPresentationResultResponse getPresentationResult(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long presentationId // VP 제출 ID
    ) {
        AuthContext authContext = resolveAuthContext(userDetails); // 인증 컨텍스트
        validatePresentationId(presentationId);

        VpVerification vpVerification = vpVerificationRepository.getById(presentationId); // VP 검증 정보
        validatePresentationOwnership(authContext.corporateId(), vpVerification);

        logEventLogger.info(
                "vp.presentation.result.requested",
                "VP presentation result requested",
                createBaseLogFields(
                        authContext.userId(),
                        authContext.corporateId(),
                        vpVerification.getCredentialId(),
                        vpVerification.getVpVerificationId(),
                        vpVerification.getVpRequestId(),
                        enumName(vpVerification.getVpVerificationStatus())
                )
        );

        return new VpPresentationResultResponse(
                vpVerification.getVpVerificationId(),
                vpVerification.getVpRequestId(),
                vpVerification.getCredentialId(),
                enumName(vpVerification.getVpVerificationStatus()),
                KyvcEnums.Yn.Y.name().equals(vpVerification.getReplaySuspectedYn()),
                vpVerification.getResultSummary(),
                vpVerification.getPresentedAt(),
                vpVerification.getVerifiedAt()
        );
    }

    // QR Payload JSON 파싱
    private JsonNode parseQrPayload(
            String qrPayload // QR Payload JSON 문자열
    ) {
        // TODO(qr-contract): 모바일 QR payload 인코딩 방식 확정 후 JSON/Base64/JWT 파싱 정책을 보강한다.
        try {
            return objectMapper.readTree(qrPayload.trim());
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.QR_PAYLOAD_INVALID, exception);
        }
    }

    // QR 유형 해석
    private KyvcEnums.QrType resolveQrType(
            JsonNode rootNode // QR Payload JSON
    ) {
        String typeValue = extractTextField(rootNode, "type"); // QR 유형 문자열
        try {
            return KyvcEnums.QrType.valueOf(typeValue);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.QR_TYPE_NOT_SUPPORTED, exception);
        }
    }

    // Credential Offer QR 해석
    private QrResolveResponse resolveCredentialOfferQr(
            JsonNode rootNode // QR Payload JSON
    ) {
        Long offerId = extractLongField(rootNode, "offerId"); // Offer ID
        extractTextField(rootNode, "qrToken");
        return new QrResolveResponse(
                KyvcEnums.QrType.CREDENTIAL_OFFER.name(),
                String.valueOf(offerId),
                offerId,
                null,
                KyvcEnums.QrNextAction.OPEN_CREDENTIAL_OFFER.name(),
                QR_CREDENTIAL_OFFER_MESSAGE
        );
    }

    // VP 요청 QR 해석
    private QrResolveResponse resolveVpRequestQr(
            JsonNode rootNode // QR Payload JSON
    ) {
        String requestId = extractTextField(rootNode, "requestId"); // VP 요청 ID
        extractTextField(rootNode, "nonce");
        extractTextField(rootNode, "challenge");
        return new QrResolveResponse(
                KyvcEnums.QrType.VP_REQUEST.name(),
                requestId,
                null,
                requestId,
                KyvcEnums.QrNextAction.OPEN_VP_REQUEST.name(),
                QR_VP_REQUEST_MESSAGE
        );
    }

    // QR 텍스트 필드 추출
    private String extractTextField(
            JsonNode rootNode, // QR Payload JSON
            String fieldName // 필드명
    ) {
        JsonNode fieldNode = rootNode.get(fieldName); // 필드 노드
        if (fieldNode == null || !StringUtils.hasText(fieldNode.asText())) {
            throw new ApiException(ErrorCode.QR_PAYLOAD_INVALID);
        }
        return fieldNode.asText().trim();
    }

    // QR 숫자 필드 추출
    private Long extractLongField(
            JsonNode rootNode, // QR Payload JSON
            String fieldName // 필드명
    ) {
        JsonNode fieldNode = rootNode.get(fieldName); // 필드 노드
        if (fieldNode == null || !fieldNode.canConvertToLong()) {
            throw new ApiException(ErrorCode.QR_PAYLOAD_INVALID);
        }
        return fieldNode.longValue();
    }

    // 소유 VP 요청 조회
    private VpVerification getOwnedVpRequest(
            Long corporateId, // 법인 ID
            String requestId // VP 요청 ID
    ) {
        VpVerification vpVerification = vpVerificationRepository.getByRequestId(requestId);
        validateVpVerificationOwnership(corporateId, vpVerification);
        return vpVerification;
    }

    // VP 요청 만료 검증
    private void validateVpRequestNotExpired(
            VpVerification vpVerification, // VP 검증 정보
            LocalDateTime now // 기준 일시
    ) {
        if (vpVerification.isExpired(now)) {
            throw new ApiException(ErrorCode.VP_REQUEST_EXPIRED);
        }
    }

    // VP 제출 결과 소유권 검증
    private void validatePresentationOwnership(
            Long corporateId, // 법인 ID
            VpVerification vpVerification // VP 검증 정보
    ) {
        if (vpVerification.getCredentialId() != null) {
            credentialRepository.findById(vpVerification.getCredentialId())
                    .ifPresentOrElse(
                            credential -> validateCredentialOwnership(corporateId, credential),
                            () -> validateVpVerificationOwnership(corporateId, vpVerification)
                    );
            return;
        }
        validateVpVerificationOwnership(corporateId, vpVerification);
    }

    // VP 검증 소유권 검증
    private void validateVpVerificationOwnership(
            Long corporateId, // 법인 ID
            VpVerification vpVerification // VP 검증 정보
    ) {
        if (vpVerification.getCorporateId() == null || !vpVerification.getCorporateId().equals(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
    }

    // Credential 소유권 검증
    private void validateCredentialOwnership(
            Long corporateId, // 법인 ID
            Credential credential // Credential 정보
    ) {
        if (!credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
    }

    // 제출 가능 Credential 응답 변환
    private EligibleCredentialResponse toEligibleCredentialResponse(
            Credential credential // Credential 정보
    ) {
        return new EligibleCredentialResponse(
                credential.getCredentialId(),
                credential.getCredentialTypeCode(),
                credential.getIssuerDid(),
                credential.getIssuedAt(),
                credential.getExpiresAt(),
                ELIGIBLE_MATCH_REASON
        );
    }

    // Core VP 검증 요청 생성
    private CoreVpVerificationRequest buildCoreVpVerificationRequest(
            VpVerification vpVerification, // VP 검증 정보
            Credential credential, // Credential 정보
            String coreRequestId, // Core 요청 ID
            LocalDateTime requestedAt // 요청 일시
    ) {
        return new CoreVpVerificationRequest(
                coreRequestId,
                vpVerification.getVpVerificationId(),
                credential.getCredentialId(),
                vpVerification.getCorporateId(),
                vpVerification.getRequestNonce(),
                vpVerification.getPurpose(),
                buildVpVerificationCallbackUrl(coreRequestId),
                requestedAt
        );
    }

    // VP 검증 Callback URL 생성
    private String buildVpVerificationCallbackUrl(
            String coreRequestId // Core 요청 ID
    ) {
        String callbackBaseUrl = coreInternalProperties.getCallbackBaseUrl(); // Callback 기준 URL
        if (!StringUtils.hasText(callbackBaseUrl) || !StringUtils.hasText(coreRequestId)) {
            return null;
        }

        String normalizedCallbackBaseUrl = callbackBaseUrl.endsWith("/")
                ? callbackBaseUrl.substring(0, callbackBaseUrl.length() - 1)
                : callbackBaseUrl;
        return normalizedCallbackBaseUrl + "/api/internal/core/vp-verifications/" + coreRequestId + "/callback";
    }

    // JSON 직렬화
    private String toJson(
            Object value // 직렬화 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            logEventLogger.error("vp.presentation.failed", "VP payload serialization failed", exception);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    // QR 해석 요청 검증
    private void validateQrResolveRequest(
            QrResolveRequest request // QR 해석 요청
    ) {
        if (request == null || !StringUtils.hasText(request.qrPayload())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // VP 제출 요청 검증
    private void validatePresentationRequest(
            VpPresentationRequest request // VP 제출 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.requestId())
                || request.credentialId() == null
                || request.credentialId() <= 0
                || !StringUtils.hasText(request.nonce())
                || !StringUtils.hasText(request.challenge())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!StringUtils.hasText(request.vpJwt())) {
            throw new ApiException(ErrorCode.VP_JWT_REQUIRED);
        }
    }

    // VP 제출 ID 검증
    private void validatePresentationId(
            Long presentationId // VP 제출 ID
    ) {
        if (presentationId == null || presentationId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 필수 문자열 정규화
    private String normalizeRequiredText(
            String value // 원본 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return value.trim();
    }

    // 인증 컨텍스트 조회
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

    // 공통 로그 필드 생성
    private Map<String, Object> createBaseLogFields(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long credentialId, // Credential ID
            Long vpVerificationId, // VP 검증 ID
            String requestId, // VP 요청 ID
            String statusOrCoreRequestId // 상태 또는 Core 요청 ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("userId", userId);
        fields.put("corporateId", corporateId);
        fields.put("credentialId", credentialId);
        fields.put("vpVerificationId", vpVerificationId);
        fields.put("requestId", requestId);
        fields.put("status", statusOrCoreRequestId);
        return fields;
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    // 인증 컨텍스트
    private record AuthContext(
            Long userId, // 사용자 ID
            Long corporateId // 법인 ID
    ) {
    }
}
