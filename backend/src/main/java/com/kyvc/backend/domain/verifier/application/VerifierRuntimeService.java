package com.kyvc.backend.domain.verifier.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CoreDocumentEvidencePolicy;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.verifier.domain.VerifierLog;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationDetailResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierLogRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.dto.VpVerificationResultResponse;
import com.kyvc.backend.domain.vp.repository.VpVerificationQueryRepository;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.security.VerifierPrincipal;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 외부 Verifier Runtime 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class VerifierRuntimeService {

    private static final long DEFAULT_EXPIRES_IN_SECONDS = 600L; // 기본 만료 초
    private static final String TEST_VERIFY_PURPOSE = "TEST_VERIFY"; // 테스트 검증 목적
    private static final String RE_AUTH_PURPOSE = "RE_AUTH"; // 재인증 목적
    private static final String PRESENTATION_FORMAT_VP_JWT = "vp+jwt"; // VP JWT format
    private static final String PRESENTATION_FORMAT_SD_JWT = "kyvc-sd-jwt-presentation-v1"; // SD-JWT presentation format
    private static final String PRESENTATION_CHALLENGE_FORMAT_SD_JWT = "dc+sd-jwt"; // Core challenge format
    private static final String PRESENTATION_FORMAT_JSONLD_VP = "kyvc-jsonld-vp-v1"; // JSON-LD VP format
    private static final String RESULT_SUCCESS = "SUCCESS"; // 성공 결과
    private static final String RESULT_FAILED = "FAILED"; // 실패 결과

    private final VpVerificationRepository vpVerificationRepository;
    private final VpVerificationQueryRepository vpVerificationQueryRepository;
    private final CredentialRepository credentialRepository;
    private final CorporateRepository corporateRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final ObjectMapper objectMapper;
    private final VerifierLogRepository verifierLogRepository;
    private final LogEventLogger logEventLogger;

    // 외부 Verifier 재인증 요청 생성
    public VerifierReAuthRequestCreateResponse createReAuthRequest(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            VerifierReAuthRequestCreateRequest request // 재인증 요청
    ) {
        validateReAuthCreateRequest(request);
        Corporate corporate = getCorporateById(request.corporateId());
        Credential credential = getLatestValidCredential(corporate.getCorporateId());
        List<String> requestedClaims = normalizeRequiredClaims(request.requestedClaims());
        String resultNotifyUrl = normalizeResultNotifyUrl(request.resultNotifyUrl());
        LocalDateTime fallbackExpiresAt = LocalDateTime.now().plusSeconds(DEFAULT_EXPIRES_IN_SECONDS);
        ChallengeContext challengeContext = issuePresentationChallenge(requestedClaims, fallbackExpiresAt);

        VpVerification vpVerification = VpVerification.createRequest(
                credential.getCredentialId(),
                corporate.getCorporateId(),
                createRequestId(),
                challengeContext.nonce(),
                challengeContext.challenge(),
                RE_AUTH_PURPOSE,
                principal.verifierName(),
                toJson(requestedClaims),
                challengeContext.expiresAt(),
                principal.verifierId(),
                null,
                KyvcEnums.VpRequestType.RE_AUTH,
                KyvcEnums.Yn.N,
                KyvcEnums.Yn.Y,
                toJson(createReAuthMetadata(request.reason().trim(), resultNotifyUrl, challengeContext))
        );
        VpVerification saved = vpVerificationRepository.save(vpVerification);
        saveVerifierLog(principal, KyvcEnums.VerifierActionType.RE_AUTH, "/api/verifier/re-auth-requests", "POST", 201, RESULT_SUCCESS, null);
        logEventLogger.info(
                "verifier.reauth.created",
                "Verifier re-auth request created",
                Map.of("verifierId", principal.verifierId(), "corporateId", corporate.getCorporateId(), "requestId", saved.getVpRequestId())
        );
        return new VerifierReAuthRequestCreateResponse(
                saved.getVpRequestId(),
                enumName(saved.getVpVerificationStatus()),
                buildQrPayload(saved),
                saved.getExpiresAt()
        );
    }

    // 외부 Verifier 테스트 VP 검증 실행
    public VerifierTestVpVerificationResponse testVpVerification(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
    ) {
        validateTestVpVerificationRequest(request);
        ResolvedPresentation resolvedPresentation = resolvePresentation(request);
        Credential credential = credentialRepository.findLatestValid()
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND));
        Corporate corporate = getCorporateById(credential.getCorporateId());
        if (!credential.isValid(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_VALID);
        }
        List<String> requestedClaims = normalizeOptionalClaims(request.requestedClaims());
        LocalDateTime now = LocalDateTime.now();
        VpVerification vpVerification = VpVerification.createRequest(
                credential.getCredentialId(),
                corporate.getCorporateId(),
                createRequestId(),
                resolveNonce(request),
                createRandomValue(),
                TEST_VERIFY_PURPOSE,
                principal.verifierName(),
                toJson(requestedClaims),
                now.plusSeconds(DEFAULT_EXPIRES_IN_SECONDS),
                principal.verifierId(),
                null,
                KyvcEnums.VpRequestType.TEST_VERIFY,
                KyvcEnums.Yn.Y,
                KyvcEnums.Yn.N,
                null
        );
        VpVerification saved = vpVerificationRepository.save(vpVerification);
        CoreRequest coreRequest = coreRequestService.createVpVerificationRequest(saved.getVpVerificationId(), null);
        saved.markPresented(
                credential.getCredentialId(),
                TokenHashUtil.sha256(resolvedPresentation.hashSource()),
                coreRequest.getCoreRequestId(),
                now
        );
        CoreVpVerificationRequest coreRequestDto = buildCoreVpVerificationRequest(
                saved,
                credential,
                coreRequest.getCoreRequestId(),
                now
        );
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreRequestDto));
        coreRequestService.markRunning(coreRequest.getCoreRequestId());

        try {
            CoreVpVerificationResponse coreResponse = coreAdapter.requestVpVerification(
                    coreRequestDto,
                    resolvedPresentation.format(),
                    resolvedPresentation.presentation()
            );
            applyCoreVerificationResult(saved, coreResponse);
            updateCoreRequestStatus(coreRequest.getCoreRequestId(), coreResponse);
            VpVerification completed = vpVerificationRepository.save(saved);
            saveVerifierLog(principal, KyvcEnums.VerifierActionType.TEST_VERIFY, "/api/verifier/test-vp-verifications", "POST", 201, RESULT_SUCCESS, null);
            return toTestResponse(completed);
        } catch (ApiException exception) {
            markCoreRequestFailure(coreRequest.getCoreRequestId(), exception);
            saved.markFailed(exception.getErrorCode().getCode(), LocalDateTime.now());
            VpVerification failed = vpVerificationRepository.save(saved);
            saveVerifierLog(principal, KyvcEnums.VerifierActionType.TEST_VERIFY, "/api/verifier/test-vp-verifications", "POST", exception.getErrorCode().getStatus().value(), RESULT_FAILED, exception.getErrorCode().getCode());
            return toTestResponse(failed);
        }
    }

    // 외부 Verifier 테스트 VP 검증 결과 조회
    @Transactional(readOnly = true)
    public VerifierTestVpVerificationDetailResponse getTestVpVerification(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            Long testId // 테스트 검증 ID
    ) {
        validateId(testId);
        VpVerification vpVerification = vpVerificationQueryRepository
                .findVerifierTest(principal.verifierId(), testId)
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_TEST_RESULT_NOT_FOUND));
        return new VerifierTestVpVerificationDetailResponse(
                vpVerification.getVpVerificationId(),
                enumName(vpVerification.getVpVerificationStatus()),
                parseClaims(vpVerification.getRequiredClaimsJson()),
                toNullableVerificationResultResponse(vpVerification),
                resolveFailureReason(vpVerification),
                vpVerification.getRequestedAt(),
                vpVerification.getVerifiedAt()
        );
    }

    private void validateReAuthCreateRequest(
            VerifierReAuthRequestCreateRequest request // 재인증 요청
    ) {
        if (request == null
                || request.corporateId() == null
                || !StringUtils.hasText(request.reason())
                || request.requestedClaims() == null
                || request.requestedClaims().isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateTestVpVerificationRequest(
            VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
    ) {
        if (request == null) {
            throw new ApiException(ErrorCode.VP_JWT_REQUIRED);
        }
    }

    private ResolvedPresentation resolvePresentation(
            VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
    ) {
        String format = resolvePresentationFormat(request);
        if (PRESENTATION_FORMAT_JSONLD_VP.equals(format)
                || (!PRESENTATION_FORMAT_VP_JWT.equals(format) && !PRESENTATION_FORMAT_SD_JWT.equals(format))) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        Object presentation = request.presentation();
        if (presentation == null && StringUtils.hasText(request.vpJwt())) {
            presentation = request.vpJwt().trim();
        }
        if (presentation == null || isBlankPresentationText(presentation) || isEmptyPresentationMap(presentation)) {
            throw new ApiException(ErrorCode.VP_JWT_REQUIRED);
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
            VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
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

    private String resolveNonce(
            VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
    ) {
        return StringUtils.hasText(request.nonce()) ? request.nonce().trim() : createRandomValue();
    }

    private Corporate getCorporateById(
            Long corporateId // 법인 ID
    ) {
        return corporateRepository.findById(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
    }

    private Credential getLatestValidCredential(
            Long corporateId // 법인 ID
    ) {
        Credential credential = credentialRepository.findLatestByCorporateId(corporateId)
                .orElseThrow(() -> new ApiException(ErrorCode.CREDENTIAL_NOT_FOUND));
        if (!credential.isValid(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_VALID);
        }
        return credential;
    }

    private List<String> normalizeRequiredClaims(
            List<String> requestedClaims // 요청 Claim 목록
    ) {
        if (requestedClaims == null || requestedClaims.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        List<String> normalized = requestedClaims.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private List<String> normalizeOptionalClaims(
            List<String> requestedClaims // 요청 Claim 목록
    ) {
        if (requestedClaims == null || requestedClaims.isEmpty()) {
            return CoreMockSeedData.requiredClaims();
        }
        return requestedClaims.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalizeResultNotifyUrl(
            String resultNotifyUrl // 결과 통지 URL
    ) {
        if (!StringUtils.hasText(resultNotifyUrl)) {
            return null;
        }
        String normalized = resultNotifyUrl.trim();
        try {
            URI uri = URI.create(normalized);
            if (!StringUtils.hasText(uri.getHost()) || !isAllowedNotifyScheme(uri.getScheme())) {
                throw new ApiException(ErrorCode.INVALID_NOTIFY_URL);
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_NOTIFY_URL);
        }
    }

    private boolean isAllowedNotifyScheme(
            String scheme // URL scheme
    ) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private ChallengeContext issuePresentationChallenge(
            List<String> requestedClaims, // 요청 Claim 목록
            LocalDateTime fallbackExpiresAt // fallback 만료 일시
    ) {
        try {
            CorePresentationChallengeResponse response = coreAdapter.issuePresentationChallenge(
                    new CorePresentationChallengeRequest(
                            "kyvc-backend",
                            CoreMockSeedData.DEV_VP_AUD,
                            "kyvc-kyc-presentation-v1",
                            PRESENTATION_CHALLENGE_FORMAT_SD_JWT,
                            createPresentationDefinition(requestedClaims)
                    )
            );
            return new ChallengeContext(
                    resolveChallengeText(response.nonce(), createRandomValue()),
                    resolveChallengeText(response.challenge(), createRandomValue()),
                    resolveChallengeText(response.domain(), "kyvc-backend"),
                    resolveChallengeText(response.aud(), CoreMockSeedData.DEV_VP_AUD),
                    response.expiresAt() == null ? fallbackExpiresAt : response.expiresAt(),
                    response.presentationDefinition()
            );
        } catch (ApiException exception) {
            if (!isCoreChallengeFallbackError(exception.getErrorCode())) {
                throw exception;
            }
            return new ChallengeContext(
                    createRandomValue(),
                    createRandomValue(),
                    "kyvc-backend",
                    CoreMockSeedData.DEV_VP_AUD,
                    fallbackExpiresAt,
                    createPresentationDefinition(requestedClaims)
            );
        }
    }

    private CoreVpVerificationRequest buildCoreVpVerificationRequest(
            VpVerification vpVerification, // VP 검증 요청
            Credential credential, // Credential
            String coreRequestId, // Core 요청 ID
            LocalDateTime requestedAt // 요청 일시
    ) {
        return new CoreVpVerificationRequest(
                coreRequestId,
                vpVerification.getVpVerificationId(),
                credential.getCredentialId(),
                vpVerification.getCorporateId(),
                vpVerification.getRequestNonce(),
                vpVerification.getChallenge(),
                vpVerification.getPurpose(),
                CoreMockSeedData.DEV_VP_AUD,
                vpVerification.getRequiredClaimsJson(),
                requestedAt
        );
    }

    private void applyCoreVerificationResult(
            VpVerification vpVerification, // VP 검증 요청
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (coreResponse == null || !Boolean.TRUE.equals(coreResponse.completed())) {
            vpVerification.markFailed(ErrorCode.CORE_API_RESPONSE_INVALID.getCode(), LocalDateTime.now());
            return;
        }
        String summary = resolveCoreResultSummary(coreResponse);
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
            coreRequestService.markFailed(coreRequestId, ErrorCode.CORE_API_RESPONSE_INVALID.getCode());
            return;
        }
        if (Boolean.TRUE.equals(coreResponse.valid())) {
            coreRequestService.markSuccess(coreRequestId, toJson(coreResponse));
            return;
        }
        coreRequestService.markFailed(coreRequestId, resolveCoreResultSummary(coreResponse));
    }

    private void markCoreRequestFailure(
            String coreRequestId, // Core 요청 ID
            ApiException exception // Core 호출 예외
    ) {
        if (ErrorCode.CORE_API_TIMEOUT == exception.getErrorCode()) {
            coreRequestService.markTimeout(coreRequestId, exception.getErrorCode().getCode());
            return;
        }
        coreRequestService.markFailed(coreRequestId, exception.getErrorCode().getCode());
    }

    private VerifierTestVpVerificationResponse toTestResponse(
            VpVerification vpVerification // VP 테스트 검증
    ) {
        return new VerifierTestVpVerificationResponse(
                vpVerification.getVpVerificationId(),
                enumName(vpVerification.getVpVerificationStatus()),
                toNullableVerificationResultResponse(vpVerification),
                resolveFailureReason(vpVerification),
                vpVerification.getVerifiedAt()
        );
    }

    private VpVerificationResultResponse toNullableVerificationResultResponse(
            VpVerification vpVerification // VP 검증 요청
    ) {
        if (vpVerification == null || !vpVerification.isCompleted()) {
            return null;
        }
        boolean replayDetected = KyvcEnums.Yn.Y.name().equals(vpVerification.getReplaySuspectedYn());
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

    private String resolveFailureReason(
            VpVerification vpVerification // VP 검증 요청
    ) {
        if (KyvcEnums.VpVerificationStatus.VALID == vpVerification.getVpVerificationStatus()
                || KyvcEnums.VpVerificationStatus.REQUESTED == vpVerification.getVpVerificationStatus()
                || KyvcEnums.VpVerificationStatus.PRESENTED == vpVerification.getVpVerificationStatus()) {
            return null;
        }
        return vpVerification.getResultSummary();
    }

    private Map<String, Object> createPresentationDefinition(
            List<String> requestedClaims // 요청 Claim 목록
    ) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("id", "kyvc-kyc-presentation-v1");
        definition.put("acceptedFormat", PRESENTATION_CHALLENGE_FORMAT_SD_JWT);
        definition.put("format", PRESENTATION_CHALLENGE_FORMAT_SD_JWT);
        definition.put("requiredClaims", requestedClaims == null ? List.of() : requestedClaims);
        definition.put("requiredDisclosures", CoreDocumentEvidencePolicy.requiredDisclosures(requestedClaims));
        definition.put("documentRules", CoreDocumentEvidencePolicy.attachedOriginalDocumentRules(requestedClaims));
        return definition;
    }

    private Map<String, Object> createChallengeMetadata(
            ChallengeContext challengeContext // Core challenge context
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        Map<String, Object> coreChallenge = new LinkedHashMap<>();
        coreChallenge.put("domain", challengeContext.domain());
        coreChallenge.put("aud", challengeContext.aud());
        coreChallenge.put("presentationDefinition", challengeContext.presentationDefinition());
        metadata.put("coreChallenge", coreChallenge);
        return metadata;
    }

    private Map<String, Object> createReAuthMetadata(
            String reason, // 재인증 사유
            String resultNotifyUrl, // 결과 통지 URL
            ChallengeContext challengeContext // Core challenge context
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reason", reason);
        metadata.put("resultNotifyUrl", resultNotifyUrl);
        metadata.putAll(createChallengeMetadata(challengeContext));
        return metadata;
    }

    private String buildQrPayload(
            VpVerification vpVerification // VP 검증 요청
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", KyvcEnums.QrType.VP_REQUEST.name());
        payload.put("requestId", vpVerification.getVpRequestId());
        payload.put("nonce", vpVerification.getRequestNonce());
        payload.put("challenge", vpVerification.getChallenge());
        payload.put("expiresAt", vpVerification.getExpiresAt());
        return toJson(payload);
    }

    private String resolveCoreResultSummary(
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (coreResponse != null && StringUtils.hasText(coreResponse.resultSummary())) {
            return coreResponse.resultSummary().trim();
        }
        if (coreResponse != null && StringUtils.hasText(coreResponse.message())) {
            return coreResponse.message().trim();
        }
        return coreResponse == null || coreResponse.status() == null ? null : coreResponse.status();
    }

    private String resolveChallengeText(
            String value, // 원본 문자열
            String fallback // 대체 문자열
    ) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private boolean isCoreChallengeFallbackError(
            ErrorCode errorCode // 오류 코드
    ) {
        return ErrorCode.CORE_API_CALL_FAILED == errorCode
                || ErrorCode.CORE_API_TIMEOUT == errorCode
                || ErrorCode.CORE_API_RESPONSE_INVALID == errorCode
                || ErrorCode.CORE_REQUIRED_DATA_MISSING == errorCode
                || ErrorCode.CORE_DEV_SEED_DISABLED == errorCode
                || ErrorCode.CORE_UNSUPPORTED_OPERATION == errorCode;
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

    private void validateId(
            Long id // 식별자
    ) {
        if (id == null || id < 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void saveVerifierLog(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            KyvcEnums.VerifierActionType actionType, // 작업 유형
            String path, // 요청 경로
            String method, // HTTP method
            int statusCode, // HTTP 상태
            String resultCode, // 처리 결과
            String errorMessage // 오류 요약
    ) {
        verifierLogRepository.save(VerifierLog.create(
                principal.verifierId(),
                principal.apiKeyId(),
                actionType,
                path,
                method,
                statusCode,
                resultCode,
                errorMessage
        ));
    }

    private String createRequestId() {
        return "vp-req-" + UUID.randomUUID();
    }

    private String createRandomValue() {
        return UUID.randomUUID().toString();
    }

    private String toJson(
            Object value // JSON 변환 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
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

    private List<String> parseClaims(
            String requiredClaimsJson // 요청 Claim JSON
    ) {
        if (!StringUtils.hasText(requiredClaimsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(requiredClaimsJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    private record ResolvedPresentation(
            String format, // Presentation format
            Object presentation, // Presentation 원문 또는 객체
            String hashSource // 해시 저장 문자열
    ) {
    }

    private record ChallengeContext(
            String nonce, // Core nonce
            String challenge, // Core challenge
            String domain, // Core domain
            String aud, // Core aud
            LocalDateTime expiresAt, // 만료 일시
            Map<String, Object> presentationDefinition // Presentation Definition 객체
    ) {
    }
}
