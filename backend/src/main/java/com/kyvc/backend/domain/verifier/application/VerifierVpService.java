package com.kyvc.backend.domain.verifier.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.kyvc.backend.domain.finance.application.FinanceContextService;
import com.kyvc.backend.domain.finance.domain.FinanceCorporateCustomer;
import com.kyvc.backend.domain.finance.repository.FinanceCorporateCustomerRepository;
import com.kyvc.backend.domain.verifier.domain.Verifier;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestDetailResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestListResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestResultResponse;
import com.kyvc.backend.domain.verifier.dto.FinanceVpRequestSummaryResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierReAuthRequestCreateResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationDetailResponse;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationRequest;
import com.kyvc.backend.domain.verifier.dto.VerifierTestVpVerificationResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.dto.VpVerificationResultResponse;
import com.kyvc.backend.domain.vp.repository.VpVerificationQueryRepository;
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

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 금융사와 Verifier VP 요청 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class VerifierVpService {

    private static final long DEFAULT_EXPIRES_IN_SECONDS = 600L;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String FINANCE_CODE_PREFIX = "FINANCE_USER_";
    private static final String TEST_VERIFY_PURPOSE = "TEST_VERIFY";
    private static final String RE_AUTH_PURPOSE = "RE_AUTH";
    private static final String VERIFIER_NAME_PREFIX = "Verifier ";
    private static final String PRESENTATION_FORMAT_VP_JWT = "vp+jwt";
    private static final String PRESENTATION_FORMAT_SD_JWT = "kyvc-sd-jwt-presentation-v1";
    private static final String PRESENTATION_CHALLENGE_FORMAT_SD_JWT = "dc+sd-jwt";
    private static final String PRESENTATION_FORMAT_JSONLD_VP = "kyvc-jsonld-vp-v1";

    private final VpVerificationRepository vpVerificationRepository;
    private final VpVerificationQueryRepository vpVerificationQueryRepository;
    private final CredentialRepository credentialRepository;
    private final CorporateRepository corporateRepository;
    private final FinanceContextService financeContextService;
    private final FinanceCorporateCustomerRepository financeCorporateCustomerRepository;
    private final VerifierRepository verifierRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // 금융사 VP 요청 생성
    public FinanceVpRequestCreateResponse createFinanceVpRequest(
            CustomUserDetails userDetails, // 인증 사용자 정보
            FinanceVpRequestCreateRequest request // 금융사 VP 요청 생성 요청
    ) {
        AuthContext authContext = resolveFinanceAuthContext(userDetails);
        validateFinanceCreateRequest(request);
        Long placeholderCorporateId = resolveFinancePlaceholderCorporateId(authContext);
        List<String> requestedClaims = normalizeRequiredClaims(request.requestedClaims());
        LocalDateTime fallbackExpiresAt = LocalDateTime.now().plusSeconds(resolveExpiresInSeconds(request.expiresInSeconds()));
        ChallengeContext challengeContext = issuePresentationChallenge(requestedClaims, fallbackExpiresAt);
        VpVerification vpVerification = VpVerification.createRequest(
                null,
                placeholderCorporateId,
                createRequestId(),
                challengeContext.nonce(),
                challengeContext.challenge(),
                request.purpose().trim(),
                resolveFinanceRequesterName(userDetails),
                toJson(requestedClaims),
                challengeContext.expiresAt(),
                null,
                authContext.financeInstitutionCode(),
                KyvcEnums.VpRequestType.FINANCE_VERIFY,
                KyvcEnums.Yn.N,
                KyvcEnums.Yn.N,
                toJson(createChallengeMetadata(challengeContext))
        );
        VpVerification saved = vpVerificationRepository.save(vpVerification);
        logEventLogger.info(
                "vp.request.created",
                "Finance VP request created",
                createLogFields(authContext.userId(), placeholderCorporateId, saved.getVpVerificationId(), saved.getVpRequestId(), null)
        );
        return new FinanceVpRequestCreateResponse(
                saved.getVpRequestId(),
                enumName(saved.getVpVerificationStatus()),
                buildQrPayload(saved),
                saved.getExpiresAt()
        );
    }

    // 금융사 VP 요청 목록 조회
    @Transactional(readOnly = true)
    public FinanceVpRequestListResponse getFinanceVpRequests(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String status, // VP 검증 상태 필터
            LocalDateTime from, // 조회 시작 일시
            LocalDateTime to, // 조회 종료 일시
            Integer page, // 페이지 번호
            Integer size // 페이지 크기
    ) {
        AuthContext authContext = resolveFinanceAuthContext(userDetails);
        KyvcEnums.VpVerificationStatus statusFilter = parseVpStatus(status);
        List<VpVerification> allItems = vpVerificationQueryRepository.findFinanceRequests(
                authContext.financeInstitutionCode(),
                statusFilter,
                from,
                to
        )
                .stream()
                .filter(vpVerification -> isFinanceRequesterOwner(userDetails, vpVerification))
                .toList();
        PageRequest pageRequest = normalizePageRequest(page, size);
        List<FinanceVpRequestSummaryResponse> pageItems = allItems.stream()
                .skip((long) pageRequest.page() * pageRequest.size())
                .limit(pageRequest.size())
                .map(this::toFinanceSummaryResponse)
                .toList();
        return new FinanceVpRequestListResponse(
                pageItems,
                pageRequest.page(),
                pageRequest.size(),
                allItems.size(),
                calculateTotalPages(allItems.size(), pageRequest.size())
        );
    }

    // 금융사 VP 요청 상세 조회
    @Transactional(readOnly = true)
    public FinanceVpRequestDetailResponse getFinanceVpRequest(
            CustomUserDetails userDetails, // 인증 사용자 정보
            String requestId // VP 요청 ID
    ) {
        AuthContext authContext = resolveFinanceAuthContext(userDetails);
        VpVerification vpVerification = vpVerificationQueryRepository
                .findFinanceRequest(authContext.financeInstitutionCode(), normalizeRequiredText(requestId))
                .orElseThrow(() -> new ApiException(ErrorCode.VP_REQUEST_NOT_FOUND));
        validateFinanceVpRequestAccess(userDetails, vpVerification);
        Corporate corporate = getCorporateById(vpVerification.getCorporateId());
        return new FinanceVpRequestDetailResponse(
                vpVerification.getVpRequestId(),
                enumName(vpVerification.getVpVerificationStatus()),
                enumName(vpVerification.getVpVerificationStatus()),
                vpVerification.getPurpose(),
                parseClaims(vpVerification.getRequiredClaimsJson()),
                buildQrPayload(vpVerification),
                vpVerification.getCorporateId(),
                corporate.getCorporateName(),
                toFinanceResultResponse(vpVerification, corporate),
                vpVerification.getExpiresAt(),
                vpVerification.getRequestedAt(),
                vpVerification.getVerifiedAt()
        );
    }

    // Verifier 테스트 VP 검증 실행
    public VerifierTestVpVerificationResponse testVpVerification(
            CustomUserDetails userDetails, // 인증 사용자 정보
            VerifierTestVpVerificationRequest request // 테스트 VP 검증 요청
    ) {
        AuthContext authContext = resolveAuthContext(userDetails);
        validateTestVpVerificationRequest(request);
        ResolvedPresentation resolvedPresentation = resolvePresentation(request);
        Verifier verifier = resolveVerifier(userDetails);
        Corporate corporate = getCorporateById(authContext.corporateId());
        Credential credential = getLatestValidCredential(corporate.getCorporateId());
        List<String> requestedClaims = normalizeOptionalClaims(request.requestedClaims());
        String nonce = StringUtils.hasText(request.nonce()) ? request.nonce().trim() : createRandomValue();
        LocalDateTime now = LocalDateTime.now();
        VpVerification vpVerification = VpVerification.createRequest(
                credential.getCredentialId(),
                corporate.getCorporateId(),
                createRequestId(),
                nonce,
                createRandomValue(),
                TEST_VERIFY_PURPOSE,
                verifier.getVerifierName(),
                toJson(requestedClaims),
                now.plusSeconds(DEFAULT_EXPIRES_IN_SECONDS),
                verifier.getVerifierId(),
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
            logCoreStarted(authContext, credential, saved, coreRequest.getCoreRequestId());
            CoreVpVerificationResponse coreResponse = coreAdapter.requestVpVerification(
                    coreRequestDto,
                    resolvedPresentation.format(),
                    resolvedPresentation.presentation()
            );
            logCoreCompleted(authContext, credential, saved, coreRequest.getCoreRequestId());
            applyCoreVerificationResult(saved, coreResponse);
            updateCoreRequestStatus(coreRequest.getCoreRequestId(), coreResponse);
            VpVerification completed = vpVerificationRepository.save(saved);
            return toTestResponse(completed);
        } catch (ApiException exception) {
            markCoreRequestFailure(coreRequest.getCoreRequestId(), exception);
            saved.markFailed(exception.getErrorCode().getCode(), LocalDateTime.now());
            VpVerification failed = vpVerificationRepository.save(saved);
            logEventLogger.warn(
                    "core.call.failed",
                    "Core VP verification call failed",
                    createLogFields(authContext.userId(), corporate.getCorporateId(), failed.getVpVerificationId(), failed.getVpRequestId(), exception.getErrorCode().getCode())
            );
            return toTestResponse(failed);
        }
    }

    // Verifier 테스트 VP 검증 이력 상세 조회
    @Transactional(readOnly = true)
    public VerifierTestVpVerificationDetailResponse getTestVpVerification(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long testId // 테스트 검증 ID
    ) {
        validateId(testId);
        Verifier verifier = resolveVerifier(userDetails);
        VpVerification vpVerification = vpVerificationQueryRepository
                .findVerifierTest(verifier.getVerifierId(), testId)
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFIER_TEST_NOT_FOUND));
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

    // Verifier 기업 재인증 요청 생성
    public VerifierReAuthRequestCreateResponse createReAuthRequest(
            CustomUserDetails userDetails, // 인증 사용자 정보
            VerifierReAuthRequestCreateRequest request // 재인증 요청 생성 요청
    ) {
        validateReAuthCreateRequest(request);
        Verifier verifier = resolveVerifier(userDetails);
        Corporate corporate = getCorporateById(request.corporateId());
        Credential credential = getLatestValidCredential(corporate.getCorporateId());
        String resultNotifyUrl = normalizeResultNotifyUrl(request.resultNotifyUrl());
        List<String> requestedClaims = normalizeOptionalClaims(request.requestedClaims());
        LocalDateTime fallbackExpiresAt = LocalDateTime.now().plusSeconds(DEFAULT_EXPIRES_IN_SECONDS);
        ChallengeContext challengeContext = issuePresentationChallenge(requestedClaims, fallbackExpiresAt);
        VpVerification vpVerification = VpVerification.createRequest(
                credential.getCredentialId(),
                corporate.getCorporateId(),
                createRequestId(),
                challengeContext.nonce(),
                challengeContext.challenge(),
                RE_AUTH_PURPOSE,
                verifier.getVerifierName(),
                toJson(requestedClaims),
                challengeContext.expiresAt(),
                verifier.getVerifierId(),
                null,
                KyvcEnums.VpRequestType.RE_AUTH,
                KyvcEnums.Yn.N,
                KyvcEnums.Yn.Y,
                toJson(createReAuthMetadata(request.reason().trim(), resultNotifyUrl, challengeContext))
        );
        VpVerification saved = vpVerificationRepository.save(vpVerification);
        logEventLogger.info(
                "verifier.reauth.created",
                "Verifier re-auth request created",
                createLogFields(null, corporate.getCorporateId(), saved.getVpVerificationId(), saved.getVpRequestId(), verifier.getVerifierId())
        );
        return new VerifierReAuthRequestCreateResponse(
                saved.getVpRequestId(),
                enumName(saved.getVpVerificationStatus()),
                buildQrPayload(saved),
                saved.getExpiresAt()
        );
    }

    private FinanceVpRequestSummaryResponse toFinanceSummaryResponse(
            VpVerification vpVerification // VP 검증 요청
    ) {
        Corporate corporate = getCorporateById(vpVerification.getCorporateId());
        return new FinanceVpRequestSummaryResponse(
                vpVerification.getVpRequestId(),
                enumName(vpVerification.getVpVerificationStatus()),
                vpVerification.getPurpose(),
                parseClaims(vpVerification.getRequiredClaimsJson()),
                vpVerification.getCorporateId(),
                corporate.getCorporateName(),
                vpVerification.getRequestedAt(),
                vpVerification.getExpiresAt(),
                vpVerification.getVerifiedAt()
        );
    }

    private FinanceVpRequestResultResponse toFinanceResultResponse(
            VpVerification vpVerification, // VP 검증 요청
            Corporate corporate // 법인
    ) {
        if (vpVerification == null || !vpVerification.isCompleted()) {
            return null;
        }
        return new FinanceVpRequestResultResponse(
                corporate.getCorporateName(),
                corporate.getBusinessRegistrationNo(),
                vpVerification.getVerifiedAt()
        );
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
            Map<String, Object> metadata = objectMapper.readValue(vpVerification.getPermissionResultJson(), new TypeReference<>() {
            });
            Object coreChallenge = metadata.get("coreChallenge");
            if (coreChallenge instanceof Map<?, ?> challengeMap) {
                Object aud = challengeMap.get("aud");
                if (aud instanceof String audString && StringUtils.hasText(audString)) {
                    return audString.trim();
                }
            }
            Object aud = metadata.get("aud");
            if (aud instanceof String audString && StringUtils.hasText(audString)) {
                return audString.trim();
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

    private void logCoreStarted(
            AuthContext authContext, // 인증 컨텍스트
            Credential credential, // Credential
            VpVerification vpVerification, // VP 검증 요청
            String coreRequestId // Core 요청 ID
    ) {
        logEventLogger.info(
                "core.call.started",
                "Core VP verification call started",
                createLogFields(authContext.userId(), credential.getCorporateId(), vpVerification.getVpVerificationId(), vpVerification.getVpRequestId(), coreRequestId)
        );
    }

    private void logCoreCompleted(
            AuthContext authContext, // 인증 컨텍스트
            Credential credential, // Credential
            VpVerification vpVerification, // VP 검증 요청
            String coreRequestId // Core 요청 ID
    ) {
        logEventLogger.info(
                "core.call.completed",
                "Core VP verification call completed",
                createLogFields(authContext.userId(), credential.getCorporateId(), vpVerification.getVpVerificationId(), vpVerification.getVpRequestId(), coreRequestId)
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

    private AuthContext resolveAuthContext(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        Corporate corporate = corporateRepository.findByUserId(userDetails.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
        return new AuthContext(
                userDetails.getUserId(),
                userDetails.getEmail(),
                corporate.getCorporateId(),
                FINANCE_CODE_PREFIX + userDetails.getUserId()
        );
    }

    private AuthContext resolveFinanceAuthContext(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        return new AuthContext(
                context.userId(),
                userDetails == null ? null : userDetails.getEmail(),
                null,
                context.financeInstitutionCode()
        );
    }

    private Long resolveFinancePlaceholderCorporateId(
            AuthContext authContext // 금융사 직원 인증 컨텍스트
    ) {
        return financeCorporateCustomerRepository.findLatestByLinkedByUserId(authContext.userId())
                .map(FinanceCorporateCustomer::getCorporateId)
                .orElseGet(() -> corporateRepository.findByUserId(authContext.userId())
                        .map(Corporate::getCorporateId)
                        .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_CONTEXT_NOT_FOUND)));
    }

    private void validateFinanceVpRequestAccess(
            CustomUserDetails userDetails, // 인증 사용자 정보
            VpVerification vpVerification // VP 요청
    ) {
        if (!isFinanceRequesterOwner(userDetails, vpVerification)) {
            throw new ApiException(ErrorCode.VP_REQUEST_NOT_FOUND);
        }
    }

    private boolean isFinanceRequesterOwner(
            CustomUserDetails userDetails, // 인증 사용자 정보
            VpVerification vpVerification // VP 요청
    ) {
        return vpVerification != null
                && KyvcEnums.VpRequestType.FINANCE_VERIFY == vpVerification.getRequestTypeCode()
                && resolveFinanceRequesterName(userDetails).equals(vpVerification.getRequesterName());
    }

    private Verifier resolveVerifier(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        String contactEmail = resolveVerifierContactEmail(userDetails);
        Verifier verifier = verifierRepository.findLatestByContactEmail(contactEmail)
                .orElseGet(() -> verifierRepository.save(Verifier.createForAuthenticatedUser(
                        resolveVerifierName(userDetails),
                        contactEmail
                )));
        if (!verifier.isActive()) {
            throw new ApiException(ErrorCode.VERIFIER_ACCESS_DENIED);
        }
        return verifier;
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

    private void validateFinanceCreateRequest(
            FinanceVpRequestCreateRequest request // 금융사 VP 요청 생성 요청
    ) {
        if (request == null || !StringUtils.hasText(request.purpose())
                || request.expiresInSeconds() == null || request.expiresInSeconds() < 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        normalizeRequiredClaims(request.requestedClaims());
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

    private void validateReAuthCreateRequest(
            VerifierReAuthRequestCreateRequest request // 재인증 요청 생성 요청
    ) {
        if (request == null || request.corporateId() == null || !StringUtils.hasText(request.reason())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateId(
            Long id // 식별자
    ) {
        if (id == null || id < 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
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

    private String normalizeRequiredText(
            String value // 필수 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return value.trim();
    }

    private long resolveExpiresInSeconds(
            Long expiresInSeconds // QR 유효 초
    ) {
        if (expiresInSeconds == null) {
            return DEFAULT_EXPIRES_IN_SECONDS;
        }
        if (expiresInSeconds < 1) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return expiresInSeconds;
    }

    private KyvcEnums.VpVerificationStatus parseVpStatus(
            String status // VP 검증 상태 문자열
    ) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return KyvcEnums.VpVerificationStatus.valueOf(status.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private PageRequest normalizePageRequest(
            Integer page, // 페이지 번호
            Integer size // 페이지 크기
    ) {
        int normalizedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageRequest(normalizedPage, normalizedSize);
    }

    private int calculateTotalPages(
            int totalElements, // 전체 건수
            int size // 페이지 크기
    ) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    private String normalizeResultNotifyUrl(
            String resultNotifyUrl // 외부 Verifier 결과 통지 URL
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

    private String createRequestId() {
        return "vp-req-" + UUID.randomUUID();
    }

    private String createRandomValue() {
        return UUID.randomUUID().toString();
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
            logEventLogger.warn(
                    "core.challenge.fallback",
                    "Core VP challenge fallback used",
                    Map.of("errorCode", exception.getErrorCode().getCode())
            );
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

    private Map<String, Object> createPresentationDefinition(
            List<String> requestedClaims // 요청 Claim 목록
    ) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("id", "kyvc-kyc-presentation-v1");
        definition.put("acceptedFormat", PRESENTATION_CHALLENGE_FORMAT_SD_JWT);
        definition.put("format", PRESENTATION_CHALLENGE_FORMAT_SD_JWT);
        definition.put("requiredClaims", requestedClaims == null ? List.of() : requestedClaims);
        return definition;
    }

    private Map<String, Object> createChallengeMetadata(
            ChallengeContext challengeContext // Core challenge 컨텍스트
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
            String resultNotifyUrl, // 외부 Verifier 결과 통지 URL
            ChallengeContext challengeContext // Core challenge 컨텍스트
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("reason", reason);
        metadata.put("resultNotifyUrl", resultNotifyUrl);
        metadata.putAll(createChallengeMetadata(challengeContext));
        return metadata;
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

    private String resolveCoreResultSummary(
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (coreResponse != null && StringUtils.hasText(coreResponse.resultSummary())) {
            return coreResponse.resultSummary().trim();
        }
        if (coreResponse != null && StringUtils.hasText(coreResponse.message())) {
            return coreResponse.message().trim();
        }
        return enumName(coreResponse == null ? null : coreResponse.status());
    }

    private String resolveFinanceRequesterName(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (StringUtils.hasText(userDetails.getEmail())) {
            return userDetails.getEmail().trim();
        }
        return FINANCE_CODE_PREFIX + userDetails.getUserId();
    }

    private String resolveVerifierContactEmail(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (StringUtils.hasText(userDetails.getEmail())) {
            return userDetails.getEmail().trim();
        }
        return "user-" + userDetails.getUserId() + "@kyvc.local";
    }

    private String resolveVerifierName(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (StringUtils.hasText(userDetails.getEmail())) {
            return userDetails.getEmail().trim();
        }
        return VERIFIER_NAME_PREFIX + userDetails.getUserId();
    }

    private Map<String, Object> createLogFields(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long vpVerificationId, // VP 검증 ID
            String requestId, // VP 요청 ID
            Object statusOrReference // 상태 또는 참조값
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("userId", userId);
        fields.put("corporateId", corporateId);
        fields.put("vpVerificationId", vpVerificationId);
        fields.put("requestId", requestId);
        fields.put("status", statusOrReference);
        return fields;
    }

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }

    private String enumName(
            String value // 문자열 값
    ) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record AuthContext(
            Long userId, // 사용자 ID
            String email, // 사용자 이메일
            Long corporateId, // 법인 ID
            String financeInstitutionCode // 금융기관 코드
    ) {
    }

    private record PageRequest(
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
    }

    private record ResolvedPresentation(
            String format, // Presentation format
            Object presentation, // Presentation 원문 또는 객체
            String hashSource // 해시 대상 문자열
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
