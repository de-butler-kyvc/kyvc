package com.kyvc.backend.domain.vp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialOffer;
import com.kyvc.backend.domain.credential.repository.CredentialOfferRepository;
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
import com.kyvc.backend.domain.vp.dto.VpVerificationResultResponse;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// VP 검증 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class VpVerificationService {

    private static final String QR_CREDENTIAL_OFFER_MESSAGE = "Credential Offer QR입니다.";
    private static final String QR_VP_REQUEST_MESSAGE = "VP 요청 QR입니다.";
    private static final String ELIGIBLE_MATCH_REASON = "동일 법인의 Wallet 저장 VALID Credential입니다.";
    private static final String VP_PRESENTATION_MESSAGE = "VP 검증이 완료되었습니다.";
    private static final String PRESENTATION_FORMAT_VP_JWT = "vp+jwt";
    private static final String PRESENTATION_FORMAT_SD_JWT = "kyvc-sd-jwt-presentation-v1";
    private static final String PRESENTATION_FORMAT_JSONLD_VP = "kyvc-jsonld-vp-v1";
    private static final int MAX_DID_DOCUMENT_BYTES = 64 * 1024;

    private final VpVerificationRepository vpVerificationRepository;
    private final CredentialRepository credentialRepository;
    private final CredentialOfferRepository credentialOfferRepository;
    private final CorporateRepository corporateRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // QR 해석
    @Transactional(readOnly = true)
    public QrResolveResponse resolveQr(
            CustomUserDetails userDetails, // 인증 사용자 정보
            QrResolveRequest request // QR 해석 요청
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        validateQrResolveRequest(request);
        JsonNode rootNode = parseQrPayload(request.qrPayload());
        KyvcEnums.QrType qrType = resolveQrType(rootNode);
        QrResolveResponse response = switch (qrType) {
            case CREDENTIAL_OFFER -> resolveCredentialOfferQr(rootNode);
            case VP_REQUEST -> resolveVpRequestQr(rootNode, authContext);
        };
        logEventLogger.info(
                "qr.resolve.completed",
                "QR resolve completed",
                createBaseLogFields(authContext.userId(), authContext.corporateId(), response.offerId(), null, response.requestId(), response.type())
        );
        return response;
    }

    // VP 요청 상세 조회
    @Transactional(readOnly = true)
    public VpRequestResponse getVpRequest(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String requestId // VP 요청 ID
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        VpVerification vpVerification = getAccessibleVpRequest(authContext.corporateId(), normalizeRequiredText(requestId));
        return toVpRequestResponse(vpVerification);
    }

    // VP 제출 가능 Credential 목록 조회
    @Transactional(readOnly = true)
    public EligibleCredentialListResponse getEligibleCredentials(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String requestId // VP 요청 ID
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        VpVerification vpVerification = getAccessibleVpRequest(authContext.corporateId(), normalizeRequiredText(requestId));
        validateVpRequestNotExpired(vpVerification, LocalDateTime.now());
        List<EligibleCredentialResponse> credentials = credentialRepository
                .findVpEligibleCredentialsByCorporateId(authContext.corporateId())
                .stream()
                .filter(credential -> credential.isValid(LocalDateTime.now()))
                .map(this::toEligibleCredentialResponse)
                .toList();
        return new EligibleCredentialListResponse(vpVerification.getVpRequestId(), credentials, credentials.size());
    }

    // VP 제출
    public VpPresentationResponse submitPresentation(
            CustomUserDetails userDetails, // 인증 사용자 정보
            VpPresentationRequest request // VP 제출 요청
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        validatePresentationRequest(request);
        ResolvedPresentation resolvedPresentation = resolvePresentation(request);
        VpVerification vpVerification = getAccessibleVpRequest(authContext.corporateId(), request.requestId().trim());
        validateVpRequestNotExpired(vpVerification, LocalDateTime.now());
        validateVpRequestSubmittable(vpVerification);

        Credential credential = credentialRepository.getById(request.credentialId());
        validateCredentialOwnership(authContext.corporateId(), credential);
        validateCredentialEligible(credential);
        validateNonceAndChallenge(vpVerification, request);
        Map<String, Object> didDocuments = buildDidDocuments(credential, request.didDocument());

        String vpJwtHash = TokenHashUtil.sha256(resolvedPresentation.hashSource());
        if (vpVerificationRepository.existsReplayCandidate(vpVerification.getRequestNonce(), vpJwtHash)) {
            vpVerification.markReplaySuspected("VP 재제출이 의심됩니다.", LocalDateTime.now());
            vpVerificationRepository.save(vpVerification);
            throw new ApiException(ErrorCode.VP_PRESENTATION_REPLAY_SUSPECTED);
        }

        CoreRequest coreRequest = coreRequestService.createVpVerificationRequest(vpVerification.getVpVerificationId(), null);
        LocalDateTime presentedAt = LocalDateTime.now();
        vpVerification.markPresentedForCorporate(authContext.corporateId(), credential.getCredentialId(), vpJwtHash, coreRequest.getCoreRequestId(), presentedAt);
        CoreVpVerificationRequest coreRequestDto = buildCoreVpVerificationRequest(vpVerification, credential, request.challenge(), coreRequest.getCoreRequestId(), presentedAt);
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreRequestDto));
        coreRequestService.markRunning(coreRequest.getCoreRequestId());

        try {
            logEventLogger.info(
                    "core.call.started",
                    "Core VP verification call started",
                    createBaseLogFields(authContext.userId(), authContext.corporateId(), credential.getCredentialId(), vpVerification.getVpVerificationId(), vpVerification.getVpRequestId(), coreRequest.getCoreRequestId())
            );
            CoreVpVerificationResponse coreResponse = coreAdapter.requestVpVerification(
                    coreRequestDto,
                    resolvedPresentation.format(),
                    resolvedPresentation.presentation(),
                    didDocuments
            );
            logEventLogger.info(
                    "core.call.completed",
                    "Core VP verification call completed",
                    createBaseLogFields(authContext.userId(), authContext.corporateId(), credential.getCredentialId(), vpVerification.getVpVerificationId(), vpVerification.getVpRequestId(), coreRequest.getCoreRequestId())
            );
            applyCoreVerificationResult(vpVerification, coreResponse);
            updateCoreRequestStatus(coreRequest.getCoreRequestId(), coreResponse);
            VpVerification saved = vpVerificationRepository.save(vpVerification);
            return new VpPresentationResponse(
                    saved.getVpVerificationId(),
                    saved.getVpRequestId(),
                    saved.getCredentialId(),
                    enumName(saved.getVpVerificationStatus()),
                    toVerificationResultResponse(saved, coreResponse),
                    saved.getPresentedAt(),
                    saved.getVerifiedAt(),
                    resolveVpPresentationMessage(saved, coreResponse)
            );
        } catch (ApiException exception) {
            markCoreRequestFailure(coreRequest.getCoreRequestId(), exception);
            vpVerification.markInvalid(exception.getMessage(), LocalDateTime.now());
            vpVerificationRepository.save(vpVerification);
            throw exception;
        }
    }

    // VP 제출 결과 조회
    @Transactional(readOnly = true)
    public VpPresentationResultResponse getPresentationResult(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long presentationId // VP 제출 ID
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        VpVerification vpVerification = vpVerificationRepository.getById(presentationId);
        validatePresentationOwnership(authContext.corporateId(), vpVerification);
        return new VpPresentationResultResponse(
                vpVerification.getVpVerificationId(),
                vpVerification.getVpRequestId(),
                vpVerification.getRequesterName(),
                vpVerification.getPurpose(),
                enumName(vpVerification.getVpVerificationStatus()),
                vpVerification.getCredentialId(),
                enumName(vpVerification.getVpVerificationStatus()),
                toNullableVerificationResultResponse(vpVerification, null),
                KyvcEnums.Yn.Y.name().equals(vpVerification.getReplaySuspectedYn()),
                vpVerification.getResultSummary(),
                vpVerification.getPresentedAt(),
                vpVerification.getVerifiedAt()
        );
    }

    private VpRequestResponse toVpRequestResponse(
            VpVerification vpVerification // VP 검증 요청
    ) {
        return new VpRequestResponse(
                vpVerification.getVpRequestId(),
                vpVerification.getRequesterName(),
                vpVerification.getPurpose(),
                vpVerification.getRequiredClaimsJson(),
                vpVerification.getChallenge(),
                vpVerification.getRequestNonce(),
                vpVerification.getExpiresAt(),
                vpVerification.isExpired(LocalDateTime.now()),
                !vpVerification.isRequested(),
                enumName(vpVerification.getVpVerificationStatus()),
                toNullableVerificationResultResponse(vpVerification, null),
                vpVerification.getVerifiedAt()
        );
    }

    private EligibleCredentialResponse toEligibleCredentialResponse(
            Credential credential // Credential
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

    private QrResolveResponse resolveCredentialOfferQr(
            JsonNode rootNode // QR Payload JSON
    ) {
        Long offerId = extractLongField(rootNode, "offerId");
        CredentialOffer credentialOffer = credentialOfferRepository.getById(offerId);
        validateCredentialOfferResolvable(credentialOffer, LocalDateTime.now());
        return new QrResolveResponse(
                KyvcEnums.QrType.CREDENTIAL_OFFER.name(),
                String.valueOf(offerId),
                offerId,
                null,
                KyvcEnums.QrNextAction.OPEN_CREDENTIAL_OFFER.name(),
                QR_CREDENTIAL_OFFER_MESSAGE
        );
    }

    private void validateCredentialOfferResolvable(
            CredentialOffer credentialOffer, // Credential Offer
            LocalDateTime now // 기준 일시
    ) {
        if (credentialOffer.isUsed()) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_ALREADY_USED);
        }
        if (credentialOffer.isExpired(now)) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_EXPIRED);
        }
        if (KyvcEnums.CredentialOfferStatus.ACTIVE != credentialOffer.getOfferStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_ACTIVE);
        }
    }

    private QrResolveResponse resolveVpRequestQr(
            JsonNode rootNode, // QR Payload JSON
            AuthContext authContext // 인증 컨텍스트
    ) {
        String requestId = extractTextField(rootNode, "requestId");
        VpVerification vpVerification = getAccessibleVpRequest(authContext.corporateId(), requestId);
        validateVpRequestNotExpired(vpVerification, LocalDateTime.now());
        validateVpRequestSubmittable(vpVerification);
        return new QrResolveResponse(
                KyvcEnums.QrType.VP_REQUEST.name(),
                requestId,
                null,
                requestId,
                KyvcEnums.QrNextAction.OPEN_VP_REQUEST.name(),
                QR_VP_REQUEST_MESSAGE
        );
    }

    private CoreVpVerificationRequest buildCoreVpVerificationRequest(
            VpVerification vpVerification, // VP 검증 요청
            Credential credential, // Credential
            String challenge, // challenge
            String coreRequestId, // Core 요청 ID
            LocalDateTime requestedAt // 요청 시각
    ) {
        return new CoreVpVerificationRequest(
                coreRequestId,
                vpVerification.getVpVerificationId(),
                credential.getCredentialId(),
                vpVerification.getCorporateId(),
                vpVerification.getRequestNonce(),
                challenge,
                vpVerification.getPurpose(),
                resolveVpAud(vpVerification),
                vpVerification.getRequiredClaimsJson(),
                requestedAt
        );
    }

    private String resolveVpAud(
            VpVerification vpVerification // VP 검증 요청
    ) {
        if (!StringUtils.hasText(vpVerification.getPermissionResultJson())) {
            return CoreMockSeedData.DEV_VP_AUD;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(vpVerification.getPermissionResultJson());
            JsonNode coreChallengeNode = rootNode.get("coreChallenge");
            JsonNode audNode = coreChallengeNode == null ? rootNode.get("aud") : coreChallengeNode.get("aud");
            if (audNode != null && StringUtils.hasText(audNode.asText())) {
                return audNode.asText().trim();
            }
        } catch (JsonProcessingException exception) {
            return CoreMockSeedData.DEV_VP_AUD;
        }
        return CoreMockSeedData.DEV_VP_AUD;
    }

    private void applyCoreVerificationResult(
            VpVerification vpVerification, // VP 검증 요청
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (coreResponse == null || !Boolean.TRUE.equals(coreResponse.completed())) {
            vpVerification.markInvalid("VP 검증 결과를 확인할 수 없습니다.", LocalDateTime.now());
            return;
        }
        String summary = resolveVpPresentationMessage(vpVerification, coreResponse);
        if (Boolean.TRUE.equals(coreResponse.replaySuspected())) {
            vpVerification.markReplaySuspected(summary, LocalDateTime.now());
            return;
        }
        if (Boolean.TRUE.equals(coreResponse.valid())) {
            vpVerification.markValid(summary, LocalDateTime.now());
            return;
        }
        vpVerification.markInvalid(summary, LocalDateTime.now());
    }

    private void updateCoreRequestStatus(
            String coreRequestId, // Core 요청 ID
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (coreResponse == null || !Boolean.TRUE.equals(coreResponse.completed())) {
            coreRequestService.markFailed(coreRequestId, "VP 검증 결과를 확인할 수 없습니다.");
            return;
        }
        if (Boolean.TRUE.equals(coreResponse.valid())) {
            coreRequestService.markSuccess(coreRequestId, toJson(coreResponse));
            return;
        }
        coreRequestService.markFailed(coreRequestId, resolveVpPresentationMessage(null, coreResponse));
    }

    private String resolveVpPresentationMessage(
            VpVerification vpVerification, // VP 검증 요청
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (coreResponse != null && StringUtils.hasText(coreResponse.resultSummary())) {
            return coreResponse.resultSummary().trim();
        }
        if (coreResponse != null && StringUtils.hasText(coreResponse.message())) {
            return coreResponse.message().trim();
        }
        if (vpVerification != null && vpVerification.isCompleted()) {
            return enumName(vpVerification.getVpVerificationStatus());
        }
        return VP_PRESENTATION_MESSAGE;
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

    private VpVerificationResultResponse toVerificationResultResponse(
            VpVerification vpVerification, // VP 검증 요청
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        boolean replayDetected = KyvcEnums.Yn.Y.name().equals(vpVerification.getReplaySuspectedYn())
                || Boolean.TRUE.equals(coreResponse == null ? null : coreResponse.replaySuspected());
        boolean signatureValid = KyvcEnums.VpVerificationStatus.VALID == vpVerification.getVpVerificationStatus();
        boolean issuerTrusted = signatureValid && !replayDetected;
        String credentialStatus = signatureValid
                ? KyvcEnums.CredentialStatus.VALID.name()
                : KyvcEnums.VpVerificationStatus.INVALID.name();
        return new VpVerificationResultResponse(
                signatureValid,
                issuerTrusted,
                credentialStatus,
                replayDetected
        );
    }

    private VpVerificationResultResponse toNullableVerificationResultResponse(
            VpVerification vpVerification, // VP 검증 요청
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (vpVerification == null || !vpVerification.isCompleted()) {
            return null;
        }
        return toVerificationResultResponse(vpVerification, coreResponse);
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

    private ResolvedPresentation resolvePresentation(
            VpPresentationRequest request // VP 제출 요청
    ) {
        String format = resolvePresentationFormat(request);
        if (PRESENTATION_FORMAT_JSONLD_VP.equals(format)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (!PRESENTATION_FORMAT_VP_JWT.equals(format) && !PRESENTATION_FORMAT_SD_JWT.equals(format)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        Object presentation = request.presentation();
        if (presentation == null && StringUtils.hasText(request.vpJwt())) {
            presentation = request.vpJwt().trim();
        }
        if (presentation == null || isBlankPresentationText(presentation) || isEmptyPresentationMap(presentation)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        if (PRESENTATION_FORMAT_VP_JWT.equals(format) && !(presentation instanceof String)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String hashSource = presentation instanceof String presentationString
                ? presentationString.trim()
                : toRequiredJson(presentation);
        Object normalizedPresentation = presentation instanceof String presentationString
                ? presentationString.trim()
                : presentation;
        return new ResolvedPresentation(format, normalizedPresentation, hashSource);
    }

    private String resolvePresentationFormat(
            VpPresentationRequest request // VP 제출 요청
    ) {
        if (StringUtils.hasText(request.format())) {
            return request.format().trim().toLowerCase(java.util.Locale.ROOT);
        }
        if (StringUtils.hasText(request.vpJwt())) {
            return PRESENTATION_FORMAT_VP_JWT;
        }
        if (request.presentation() instanceof Map<?, ?> presentationMap) {
            Object embeddedFormat = presentationMap.get("format");
            if (embeddedFormat instanceof String embeddedFormatString && StringUtils.hasText(embeddedFormatString)) {
                return embeddedFormatString.trim().toLowerCase(java.util.Locale.ROOT);
            }
        }
        throw new ApiException(ErrorCode.INVALID_REQUEST);
    }

    private boolean isEmptyPresentationMap(
            Object presentation // Presentation 원문 또는 객체
    ) {
        return presentation instanceof Map<?, ?> presentationMap && presentationMap.isEmpty();
    }

    private boolean isBlankPresentationText(
            Object presentation // Presentation 원문 또는 객체
    ) {
        return presentation instanceof String presentationString && !StringUtils.hasText(presentationString);
    }

    private String toRequiredJson(
            Object value // JSON 변환 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, exception);
        }
    }

    private JsonNode parseQrPayload(
            String qrPayload // QR Payload JSON
    ) {
        try {
            return objectMapper.readTree(qrPayload);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.QR_PAYLOAD_INVALID, exception);
        }
    }

    private KyvcEnums.QrType resolveQrType(
            JsonNode rootNode // QR Payload JSON
    ) {
        String typeValue = extractTextField(rootNode, "type");
        try {
            return KyvcEnums.QrType.valueOf(typeValue);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.QR_TYPE_NOT_SUPPORTED);
        }
    }

    private String extractTextField(
            JsonNode rootNode, // QR Payload JSON
            String fieldName // 필드명
    ) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode == null || !StringUtils.hasText(fieldNode.asText())) {
            throw new ApiException(ErrorCode.QR_PAYLOAD_INVALID);
        }
        return fieldNode.asText().trim();
    }

    private Long extractLongField(
            JsonNode rootNode, // QR Payload JSON
            String fieldName // 필드명
    ) {
        JsonNode fieldNode = rootNode.get(fieldName);
        if (fieldNode == null || !fieldNode.canConvertToLong()) {
            throw new ApiException(ErrorCode.QR_PAYLOAD_INVALID);
        }
        return fieldNode.longValue();
    }

    private VpVerification getOwnedVpRequest(
            Long corporateId, // 법인 ID
            String requestId // VP 요청 ID
    ) {
        VpVerification vpVerification = vpVerificationRepository.getByRequestId(requestId);
        validateVpVerificationOwnership(corporateId, vpVerification);
        return vpVerification;
    }

    private VpVerification getAccessibleVpRequest(
            Long corporateId, // 법인 ID
            String requestId // VP 요청 ID
    ) {
        VpVerification vpVerification = vpVerificationRepository.getByRequestId(requestId);
        if (isFinanceVpRequest(vpVerification)) {
            return vpVerification;
        }
        validateVpVerificationOwnership(corporateId, vpVerification);
        return vpVerification;
    }

    private void validateVpRequestNotExpired(
            VpVerification vpVerification, // VP 검증 요청
            LocalDateTime now // 기준 일시
    ) {
        if (vpVerification.isExpired(now)) {
            throw new ApiException(ErrorCode.VP_REQUEST_EXPIRED);
        }
    }

    private void validatePresentationOwnership(
            Long corporateId, // 법인 ID
            VpVerification vpVerification // VP 검증 요청
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

    private void validateVpVerificationOwnership(
            Long corporateId, // 법인 ID
            VpVerification vpVerification // VP 검증 요청
    ) {
        if (vpVerification == null || !corporateId.equals(vpVerification.getCorporateId())) {
            throw new ApiException(ErrorCode.VP_REQUEST_NOT_FOUND);
        }
    }

    private boolean isFinanceVpRequest(
            VpVerification vpVerification
    ) {
        return vpVerification != null && KyvcEnums.VpRequestType.FINANCE_VERIFY == vpVerification.getRequestTypeCode();
    }

    private void validateCredentialOwnership(
            Long corporateId, // 법인 ID
            Credential credential // Credential
    ) {
        if (credential == null || !credential.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
    }

    private void validateCredentialEligible(
            Credential credential // Credential
    ) {
        if (!credential.isWalletSaved() || !credential.isValid(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.VP_CREDENTIAL_NOT_ELIGIBLE);
        }
    }

    private void validateNonceAndChallenge(
            VpVerification vpVerification, // VP 검증 요청
            VpPresentationRequest request // VP 제출 요청
    ) {
        if (!vpVerification.matchesNonce(request.nonce())) {
            throw new ApiException(ErrorCode.VP_NONCE_INVALID);
        }
        if (!vpVerification.matchesChallenge(request.challenge())) {
            throw new ApiException(ErrorCode.VP_CHALLENGE_INVALID);
        }
    }

    // Holder DID Document 목록 생성
    private Map<String, Object> buildDidDocuments(
            Credential credential, // 제출 Credential
            Object didDocument // Holder DID Document
    ) {
        if (didDocument == null) {
            throw new ApiException(ErrorCode.VP_DID_DOCUMENT_REQUIRED);
        }
        if (!(didDocument instanceof Map<?, ?>)) {
            throw new ApiException(ErrorCode.VP_DID_DOCUMENT_INVALID);
        }
        validateDidDocumentPayloadSize(didDocument);
        String holderDid = normalizeRequiredHolderDid(credential);
        Map<String, Object> document = resolveDidDocumentMap(asObjectMap(didDocument), holderDid);
        if (document.isEmpty()) {
            throw new ApiException(ErrorCode.VP_DID_DOCUMENT_INVALID);
        }

        Object idValue = document.get("id");
        if (!(idValue instanceof String didDocumentId) || !StringUtils.hasText(didDocumentId)) {
            throw new ApiException(ErrorCode.VP_DID_DOCUMENT_INVALID);
        }
        if (!holderDid.equals(didDocumentId.trim())) {
            throw new ApiException(ErrorCode.VP_DID_DOCUMENT_INVALID);
        }

        Map<String, Object> didDocuments = new LinkedHashMap<>();
        didDocuments.put(holderDid, document);
        return didDocuments;
    }

    // Holder DID Document Map 조회
    private Map<String, Object> resolveDidDocumentMap(
            Map<String, Object> payload, // DID Document payload
            String holderDid // Holder DID
    ) {
        if (payload.containsKey("id")) {
            return payload;
        }
        Object nestedDocument = payload.get(holderDid);
        if (nestedDocument instanceof Map<?, ?>) {
            return asObjectMap(nestedDocument);
        }
        return Map.of();
    }

    // Holder DID 조회
    private String normalizeRequiredHolderDid(
            Credential credential // 제출 Credential
    ) {
        if (credential == null || !StringUtils.hasText(credential.getHolderDid())) {
            throw new ApiException(ErrorCode.VP_DID_DOCUMENT_INVALID);
        }
        return credential.getHolderDid().trim();
    }

    // Holder DID Document 크기 검증
    private void validateDidDocumentPayloadSize(
            Object didDocument // Holder DID Document
    ) {
        try {
            int byteLength = objectMapper.writeValueAsString(didDocument).getBytes(StandardCharsets.UTF_8).length;
            if (byteLength > MAX_DID_DOCUMENT_BYTES) {
                throw new ApiException(ErrorCode.VP_DID_DOCUMENT_TOO_LARGE);
            }
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.VP_DID_DOCUMENT_INVALID, exception);
        }
    }

    private Map<String, Object> asObjectMap(
            Object value // Map 변환 대상
    ) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        mapValue.forEach((key, mapEntryValue) -> {
            if (key != null) {
                result.put(String.valueOf(key), mapEntryValue);
            }
        });
        return result;
    }

    private void validateVpRequestSubmittable(
            VpVerification vpVerification // VP 검증 요청
    ) {
        if (!vpVerification.isRequested()) {
            throw new ApiException(ErrorCode.VP_REQUEST_INVALID_STATUS);
        }
    }

    private void validatePresentationRequest(
            VpPresentationRequest request // VP 제출 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.requestId())
                || request.credentialId() == null
                || !StringUtils.hasText(request.nonce())
                || !StringUtils.hasText(request.challenge())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateQrResolveRequest(
            QrResolveRequest request // QR 해석 요청
    ) {
        if (request == null || !StringUtils.hasText(request.qrPayload())) {
            throw new ApiException(ErrorCode.QR_PAYLOAD_INVALID);
        }
    }

    private String normalizeRequiredText(
            String value // 필수 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return value.trim();
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

    private record ResolvedPresentation(
            String format, // Presentation format
            Object presentation, // Presentation 원문 또는 객체
            String hashSource // 해시 대상 문자열
    ) {
    }
}
