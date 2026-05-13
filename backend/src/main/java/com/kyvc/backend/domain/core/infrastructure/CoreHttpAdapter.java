package com.kyvc.backend.domain.core.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.application.CorePayloadSanitizer;
import com.kyvc.backend.domain.core.config.CoreProperties;
import com.kyvc.backend.domain.core.dto.CoreAiReviewRequest;
import com.kyvc.backend.domain.core.dto.CoreAiReviewResponse;
import com.kyvc.backend.domain.core.dto.CoreAiReviewStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialSchemaResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreCredentialVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreCredentialVerificationResponse;
import com.kyvc.backend.domain.core.dto.CoreDidDocumentResponse;
import com.kyvc.backend.domain.core.dto.CoreHealthResponse;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeResponse;
import com.kyvc.backend.domain.core.dto.CorePresentationVerifyRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationVerifyResponse;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialRequest;
import com.kyvc.backend.domain.core.dto.CoreRevokeCredentialResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceRequest;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceResponse;
import com.kyvc.backend.domain.core.dto.CoreVcIssuanceStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationStatusResponse;
import com.kyvc.backend.domain.core.dto.CoreXrplTransactionResponse;
import com.kyvc.backend.domain.core.exception.CoreAiReviewException;
import com.kyvc.backend.domain.core.infrastructure.dto.CoreHealthApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.CredentialStatusApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.IssueKycCredentialApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.IssueKycCredentialApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.IssuePresentationChallengeApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.IssuePresentationChallengeApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.LlmPrimaryAssessmentApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.LlmPrimaryAssessmentApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.RevokeCredentialApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.RevokeCredentialApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.VerificationApiResponse;
import com.kyvc.backend.domain.core.infrastructure.dto.VerifyCredentialApiRequest;
import com.kyvc.backend.domain.core.infrastructure.dto.VerifyPresentationApiRequest;
import com.kyvc.backend.domain.core.mock.CoreMockSeedData;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Core 실제 HTTP Adapter
@Component
@ConditionalOnExpression("'${kyvc.core.mode:http}'.toLowerCase() == 'http' || '${kyvc.core.mode:http}'.toLowerCase() == 'hybrid'")
public class CoreHttpAdapter implements CoreAdapter {

    private static final String HEALTH_ENDPOINT = "/health";
    private static final String AI_ASSESSMENT_ENDPOINT = "/ai-assessment/assessments/llm-primary";
    private static final String ISSUE_KYC_CREDENTIAL_ENDPOINT = "/issuer/credentials/kyc";
    private static final String REVOKE_CREDENTIAL_ENDPOINT = "/issuer/credentials/revoke";
    private static final String CREDENTIAL_STATUS_ENDPOINT = "/credential-status/credentials/{issuerAccount}/{holderAccount}/{credentialType}";
    private static final String VERIFY_PRESENTATION_ENDPOINT = "/verifier/presentations/verify";
    private static final String ISSUE_PRESENTATION_CHALLENGE_ENDPOINT = "/verifier/presentations/challenges";
    private static final String VERIFY_CREDENTIAL_ENDPOINT = "/verifier/credentials/verify";
    private static final String DID_DOCUMENT_ENDPOINT = "/dids/{account}/diddoc.json";
    private static final String PRESENTATION_FORMAT_SD_JWT = "kyvc-sd-jwt-presentation-v1";
    private static final String PRESENTATION_DEFINITION_ID_KYC = "kyvc-kyc-presentation-v1";
    private static final String PRESENTATION_CHALLENGE_FORMAT_SD_JWT = "dc+sd-jwt";
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final String CORE_API_KEY_HEADER = "X-API-Key";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String CORE_LEGAL_ENTITY_STOCK_COMPANY = "STOCK_COMPANY";
    private static final String CORE_LEGAL_ENTITY_LIMITED_COMPANY = "LIMITED_COMPANY";
    private static final String CORE_LEGAL_ENTITY_INCORPORATED_ASSOCIATION = "INCORPORATED_ASSOCIATION";
    private static final String CORE_LEGAL_ENTITY_COOPERATIVE = "COOPERATIVE";
    private static final String CORE_LEGAL_ENTITY_FOREIGN_COMPANY = "FOREIGN_COMPANY";
    private static final String CREDENTIAL_STATUS_ID_TYPE = "credential";
    private static final String CORE_APPLICANT_ROLE_REPRESENTATIVE = "REPRESENTATIVE";
    private static final String CORE_APPLICANT_ROLE_DELEGATE = "DELEGATE";
    private static final String CORE_ASSESSMENT_NORMAL = "NORMAL";
    private static final String CORE_ASSESSMENT_SUPPLEMENT_REQUIRED = "SUPPLEMENT_REQUIRED";
    private static final String CORE_ASSESSMENT_MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";
    private static final String CORE_ASSESSMENT_REJECTED = "REJECTED";
    private static final String CORE_DOCUMENT_TYPE_UNKNOWN = "UNKNOWN";
    private static final String BACKEND_CORPORATE_TYPE_CORPORATION = "CORPORATION";
    private static final String BACKEND_CORPORATE_TYPE_LIMITED_COMPANY = "LIMITED_COMPANY";
    private static final String BACKEND_CORPORATE_TYPE_NON_PROFIT = "NON_PROFIT";
    private static final String BACKEND_CORPORATE_TYPE_ASSOCIATION = "ASSOCIATION";
    private static final String BACKEND_CORPORATE_TYPE_FOREIGN_COMPANY = "FOREIGN_COMPANY";
    private static final String BACKEND_DOCUMENT_BUSINESS_REGISTRATION = "BUSINESS_REGISTRATION";
    private static final String BACKEND_DOCUMENT_CORPORATE_REGISTRATION = "CORPORATE_REGISTRATION";
    private static final String BACKEND_DOCUMENT_CORPORATE_SEAL_CERTIFICATE = "CORPORATE_SEAL_CERTIFICATE";
    private static final String BACKEND_DOCUMENT_SHAREHOLDER_LIST = "SHAREHOLDER_LIST";
    private static final String BACKEND_DOCUMENT_ARTICLES_OF_INCORPORATION = "ARTICLES_OF_INCORPORATION";
    private static final String BACKEND_DOCUMENT_POWER_OF_ATTORNEY = "POWER_OF_ATTORNEY";
    private static final String CORE_DOCUMENT_CORPORATE_REGISTRY = "CORPORATE_REGISTRY";
    private static final String CORE_DOCUMENT_SEAL_CERTIFICATE = "SEAL_CERTIFICATE";
    private static final String CORE_DOCUMENT_SHAREHOLDER_REGISTRY = "SHAREHOLDER_REGISTRY";
    private static final String CORE_DOCUMENT_ARTICLES_OF_ASSOCIATION = "ARTICLES_OF_ASSOCIATION";
    private static final String DEFAULT_STATUS_MODE = "xrpl";
    private static final String DEFAULT_CREDENTIAL_FORMAT = "jwt";
    private static final String DEFAULT_VC_FORMAT = "vc+jwt";
    private static final Set<String> ALLOWED_STATUS_MODES = Set.of("xrpl", "local");
    private static final Set<String> ALLOWED_CREDENTIAL_FORMATS = Set.of("jwt", "embedded_jws");
    private static final Set<String> ALLOWED_VC_FORMATS = Set.of("vc+jwt", "dc+sd-jwt");
    private static final Set<String> ALLOWED_PRESENTATION_CHALLENGE_FORMATS = Set.of("dc+sd-jwt", "vp+jwt");
    private static final Set<String> ALLOWED_CORE_DOCUMENT_TYPES = Set.of(
            "BUSINESS_REGISTRATION",
            "CORPORATE_REGISTRY",
            "SHAREHOLDER_REGISTRY",
            "STOCK_CHANGE_STATEMENT",
            "INVESTOR_REGISTRY",
            "MEMBER_REGISTRY",
            "BOARD_REGISTRY",
            "ARTICLES_OF_ASSOCIATION",
            "OPERATING_RULES",
            "REGULATIONS",
            "MEETING_MINUTES",
            "OFFICIAL_LETTER",
            "PURPOSE_PROOF_DOCUMENT",
            "ORGANIZATION_IDENTITY_CERTIFICATE",
            "INVESTMENT_REGISTRATION_CERTIFICATE",
            "BENEFICIAL_OWNER_PROOF_DOCUMENT",
            "POWER_OF_ATTORNEY",
            "SEAL_CERTIFICATE",
            "UNKNOWN"
    );

    private final RestClient coreRestClient;
    private final RestClient coreAiReviewRestClient;
    private final CoreProperties coreProperties;
    private final LogEventLogger logEventLogger;
    private final ObjectMapper objectMapper;
    private final CorePayloadSanitizer corePayloadSanitizer;

    public CoreHttpAdapter(
            @Qualifier("coreRestClient") RestClient coreRestClient, // 일반 Core HTTP Client
            @Qualifier("coreAiReviewRestClient") RestClient coreAiReviewRestClient, // AI 심사 Core HTTP Client
            CoreProperties coreProperties, // Core 설정
            LogEventLogger logEventLogger, // 업무 로그 컴포넌트
            ObjectMapper objectMapper, // JSON 변환기
            CorePayloadSanitizer corePayloadSanitizer // Core payload 마스킹 컴포넌트
    ) {
        this.coreRestClient = coreRestClient;
        this.coreAiReviewRestClient = coreAiReviewRestClient;
        this.coreProperties = coreProperties;
        this.logEventLogger = logEventLogger;
        this.objectMapper = objectMapper;
        this.corePayloadSanitizer = corePayloadSanitizer;
    }

    @Override
    public CoreHealthResponse checkHealth() {
        String endpoint = HEALTH_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, null, null, null);
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core health API call started", fields);
        try {
            ResponseEntity<CoreHealthApiResponse> responseEntity = coreRestClient.get()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .retrieve()
                    .toEntity(CoreHealthApiResponse.class);
            CoreHealthApiResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            logEventLogger.info("core.response.mapped", "Core health response mapped", fields);
            return new CoreHealthResponse(
                    coreProperties.normalizedMode().toUpperCase(Locale.ROOT),
                    true,
                    body.service() + ":" + body.status() + " (" + body.environment() + ")"
            );
        } catch (RestClientException exception) {
            ApiException mapped = mapCoreException(endpoint, exception, fields);
            throw mapped;
        }
    }

    @Override
    public CoreAiReviewResponse requestAiReview(
            CoreAiReviewRequest request // AI 심사 요청
    ) {
        validateAiReviewRequest(request);

        LlmPrimaryAssessmentApiRequest apiRequest = buildAiAssessmentApiRequest(request);
        String endpoint = AI_ASSESSMENT_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, request.coreRequestId(), null, null);
        fields.put("kycId", request.kycId());
        fields.put("corporateId", request.corporateId());
        fields.put("configuredAiReviewTimeoutSeconds", coreProperties.resolvedAiReviewReadTimeoutSeconds());
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core AI review API call started", fields);
        try {
            ResponseEntity<String> responseEntity = coreAiReviewRestClient.post()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .body(apiRequest)
                    .retrieve()
                    .toEntity(String.class);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            LlmPrimaryAssessmentApiResponse body = parseAiReviewResponseBody(
                    request,
                    endpoint,
                    responseEntity,
                    startedAt,
                    fields
            );
            CoreAiReviewResponse mapped = mapAiReviewResponseSafely(request, endpoint, body, startedAt, fields);
            logEventLogger.info("core.response.mapped", "Core AI review response mapped", fields);
            return mapped;
        } catch (CoreAiReviewException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw buildAiReviewCoreErrorException(request, endpoint, startedAt, fields, exception);
        } catch (ResourceAccessException exception) {
            if (isTimeoutException(exception)) {
                throw buildAiReviewTimeoutException(request, endpoint, startedAt, fields, exception);
            }
            throw buildAiReviewCoreErrorException(request, endpoint, startedAt, fields, exception);
        } catch (RestClientException exception) {
            throw buildAiReviewCoreErrorException(request, endpoint, startedAt, fields, exception);
        }
    }

    @Override
    public CoreAiReviewStatusResponse getAiReviewStatus(
            String coreRequestId // Core 요청 ID
    ) {
        throw new ApiException(ErrorCode.CORE_UNSUPPORTED_OPERATION, "Core AI 심사 상태 조회 API가 Swagger에서 확인되지 않았습니다.");
    }

    @Override
    public CoreVcIssuanceResponse requestVcIssuance(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        validateVcIssuanceRequest(request);

        String holderAccount = resolveHolderAccount(request);
        String holderDid = resolveHolderDid(request, holderAccount);
        OffsetDateTime validFrom = resolveValidFrom(request);
        OffsetDateTime validUntil = resolveValidUntil(request, validFrom);
        String statusMode = resolveAllowedValue(request.statusMode(), DEFAULT_STATUS_MODE, ALLOWED_STATUS_MODES, "statusMode");
        String credentialFormat = resolveAllowedValue(
                request.credentialFormat(),
                DEFAULT_CREDENTIAL_FORMAT,
                ALLOWED_CREDENTIAL_FORMATS,
                "credentialFormat"
        );
        String format = resolveAllowedValue(request.format(), DEFAULT_VC_FORMAT, ALLOWED_VC_FORMATS, "format");

        IssueKycCredentialApiRequest apiRequest = new IssueKycCredentialApiRequest(
                holderAccount,
                holderDid,
                resolveClaims(request.claims()),
                validFrom,
                validUntil,
                resolveBoolean(request.persist(), true),
                resolveBoolean(request.persistStatus(), true),
                resolveBoolean(request.markStatusAccepted(), false),
                normalizeOptional(request.statusUri()),
                normalizeOptional(request.xrplJsonRpcUrl()),
                resolveBoolean(request.allowMainnet(), false),
                statusMode,
                credentialFormat,
                format,
                resolveHolderKeyId(request),
                resolveVct(request)
        );

        String endpoint = ISSUE_KYC_CREDENTIAL_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, request.coreRequestId(), request.credentialId(), null);
        fields.put("kycId", request.kycId());
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core VC issuance API call started", fields);
        try {
            ResponseEntity<IssueKycCredentialApiResponse> responseEntity = coreRestClient.post()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .body(apiRequest)
                    .retrieve()
                    .toEntity(IssueKycCredentialApiResponse.class);
            IssueKycCredentialApiResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            CoreVcIssuanceResponse mapped = mapVcIssuanceResponse(request, body);
            logEventLogger.info("core.response.mapped", "Core VC issuance response mapped", fields);
            return mapped;
        } catch (RestClientException exception) {
            ApiException mapped = mapCoreException(endpoint, exception, fields);
            throw mapped;
        }
    }

    @Override
    public CoreVcIssuanceStatusResponse getVcIssuanceStatus(
            String coreRequestId // Core 요청 ID
    ) {
        throw new ApiException(ErrorCode.CORE_UNSUPPORTED_OPERATION, "Core VC 발급 상태 조회 API가 Swagger에서 확인되지 않았습니다.");
    }

    @Override
    public CoreRevokeCredentialResponse revokeCredential(
            CoreRevokeCredentialRequest request // Credential 폐기 요청
    ) {
        if (request == null || !StringUtils.hasText(request.holderAccount())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "Credential 폐기에 필요한 holderAccount가 없습니다.");
        }
        if (!StringUtils.hasText(request.credentialType())
                && !StringUtils.hasText(request.credentialStatusId())
                && !StringUtils.hasText(request.credentialExternalId())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "Credential 폐기 식별 데이터가 없습니다.");
        }
        String statusMode = resolveAllowedValue(request.statusMode(), DEFAULT_STATUS_MODE, ALLOWED_STATUS_MODES, "statusMode");
        RevokeCredentialApiRequest apiRequest = new RevokeCredentialApiRequest(
                normalizeOptional(request.issuerAccount()),
                normalizeOptional(request.issuerSeed()),
                request.holderAccount().trim(),
                normalizeOptional(request.credentialType()),
                normalizeOptional(request.credentialExternalId()),
                normalizeOptional(request.credentialStatusId()),
                normalizeOptional(request.holderDid()),
                normalizeOptional(request.issuerDid()),
                normalizeOptional(request.vct()),
                normalizeOptional(request.xrplJsonRpcUrl()),
                resolveBoolean(request.allowMainnet(), false),
                statusMode
        );

        String endpoint = REVOKE_CREDENTIAL_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, null, null, null);
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core revoke credential API call started", fields);
        try {
            ResponseEntity<RevokeCredentialApiResponse> responseEntity = coreRestClient.post()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .body(apiRequest)
                    .retrieve()
                    .toEntity(RevokeCredentialApiResponse.class);
            RevokeCredentialApiResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            return new CoreRevokeCredentialResponse(
                    body.revoked(),
                    body.statusMode(),
                    body.revoked() ? "Credential 폐기 성공" : "Credential 폐기 실패"
            );
        } catch (RestClientException exception) {
            ApiException mapped = mapCoreException(endpoint, exception, fields);
            throw mapped;
        }
    }

    @Override
    public CoreCredentialStatusResponse getCredentialStatus(
            String issuerAccount, // Issuer XRPL Account
            String holderAccount, // Holder XRPL Account
            String credentialType // Credential 유형
    ) {
        if (!StringUtils.hasText(issuerAccount) || !StringUtils.hasText(holderAccount) || !StringUtils.hasText(credentialType)) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "Credential 상태조회 필수 데이터가 부족합니다.");
        }

        String endpoint = CREDENTIAL_STATUS_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, null, null, null);
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core credential-status API call started", fields);
        try {
            ResponseEntity<CredentialStatusApiResponse> responseEntity = coreRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(endpoint)
                            .queryParam("allow_mainnet", false)
                            .queryParam("status_mode", DEFAULT_STATUS_MODE)
                            .build(issuerAccount.trim(), holderAccount.trim(), credentialType.trim()))
                    .headers(this::applyApiKeyHeader)
                    .retrieve()
                    .toEntity(CredentialStatusApiResponse.class);
            CredentialStatusApiResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            CoreCredentialStatusResponse mapped = new CoreCredentialStatusResponse(
                    body.issuerAccount(),
                    body.holderAccount(),
                    body.credentialType(),
                    body.found(),
                    body.active(),
                    mapCredentialStatusCode(body),
                    parseDateTime(body.checkedAt()),
                    body.found() ? "Core credential status synced." : "Core credential status entry not found."
            );
            logEventLogger.info("core.response.mapped", "Core credential-status response mapped", fields);
            return mapped;
        } catch (RestClientException exception) {
            ApiException mapped = mapCoreException(endpoint, exception, fields);
            throw mapped;
        }
    }

    @Override
    public CoreVpVerificationResponse requestVpVerification(
            CoreVpVerificationRequest request, // VP 검증 요청
            String format, // Presentation format
            Object presentation // Presentation 원문 또는 객체
    ) {
        if (request == null || !StringUtils.hasText(request.coreRequestId())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "VP 검증 요청 필수 데이터가 부족합니다.");
        }
        if (!StringUtils.hasText(format) || presentation == null) {
            throw new ApiException(ErrorCode.VP_JWT_REQUIRED);
        }
        if (presentation instanceof String presentationString && !StringUtils.hasText(presentationString)) {
            throw new ApiException(ErrorCode.VP_JWT_REQUIRED);
        }

        VerifyPresentationApiRequest apiRequest = new VerifyPresentationApiRequest(
                format.trim(),
                buildCorePresentation(request, format.trim(), presentation),
                null,
                buildPolicy(parseRequiredClaims(request.requiredClaimsJson())),
                true,
                null,
                false,
                DEFAULT_STATUS_MODE
        );

        String endpoint = VERIFY_PRESENTATION_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, request.coreRequestId(), request.credentialId(), request.vpVerificationId());
        fields.put("presentationFormat", format.trim());
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core VP verify API call started", fields);
        try {
            ResponseEntity<VerificationApiResponse> responseEntity = coreRestClient.post()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .body(apiRequest)
                    .retrieve()
                    .toEntity(VerificationApiResponse.class);
            VerificationApiResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            CoreVpVerificationResponse mapped = mapVpVerificationResponse(request, body);
            logEventLogger.info("core.response.mapped", "Core VP verify response mapped", fields);
            return mapped;
        } catch (RestClientException exception) {
            ApiException mapped = mapCoreException(endpoint, exception, fields);
            throw mapped;
        }
    }

    private Object buildCorePresentation(
            CoreVpVerificationRequest request, // VP 검증 요청
            String format, // Presentation format
            Object presentation // Presentation 원문 또는 객체
    ) {
        if (!PRESENTATION_FORMAT_SD_JWT.equals(format) || !(presentation instanceof String presentationString)) {
            return presentation;
        }
        Map<String, Object> presentationObject = new LinkedHashMap<>();
        presentationObject.put("format", PRESENTATION_FORMAT_SD_JWT);
        presentationObject.put("aud", request.aud());
        presentationObject.put("nonce", request.requestNonce());
        presentationObject.put("sdJwtKb", presentationString);
        return presentationObject;
    }

    private Map<String, Object> buildPolicy(
            List<String> requiredClaims // 필수 Claim 목록
    ) {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("id", PRESENTATION_DEFINITION_ID_KYC);
        policy.put("acceptedFormat", PRESENTATION_CHALLENGE_FORMAT_SD_JWT);
        policy.put("requiredClaims", requiredClaims == null ? List.of() : requiredClaims);
        return policy;
    }

    @Override
    public CoreVpVerificationStatusResponse getVpVerificationStatus(
            String coreRequestId // Core 요청 ID
    ) {
        throw new ApiException(ErrorCode.CORE_UNSUPPORTED_OPERATION, "Core VP 검증 상태 조회 API가 Swagger에서 확인되지 않았습니다.");
    }

    @Override
    public CorePresentationChallengeResponse issuePresentationChallenge(
            CorePresentationChallengeRequest request // VP Challenge 발급 요청
    ) {
        if (request == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        String format = resolveAllowedValue(
                request.format(),
                PRESENTATION_CHALLENGE_FORMAT_SD_JWT,
                ALLOWED_PRESENTATION_CHALLENGE_FORMATS,
                "format"
        );
        IssuePresentationChallengeApiRequest apiRequest = new IssuePresentationChallengeApiRequest(
                request.domain(),
                request.aud(),
                request.definitionId(),
                format,
                request.presentationDefinition()
        );

        String endpoint = ISSUE_PRESENTATION_CHALLENGE_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, null, null, null);
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core VP challenge API call started", fields);
        try {
            ResponseEntity<IssuePresentationChallengeApiResponse> responseEntity = coreRestClient.post()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .body(apiRequest)
                    .retrieve()
                    .toEntity(IssuePresentationChallengeApiResponse.class);
            IssuePresentationChallengeApiResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            return new CorePresentationChallengeResponse(
                    body.challenge(),
                    body.nonce(),
                    body.domain(),
                    body.aud(),
                    parsePresentationChallengeExpiresAt(StringUtils.hasText(body.expiresAtCamelCase()) ? body.expiresAtCamelCase() : body.expiresAtSnakeCase()),
                    body.presentationDefinition()
            );
        } catch (RestClientException exception) {
            throw mapCoreException(endpoint, exception, fields);
        }
    }

    @Override
    public CorePresentationVerifyResponse verifyWebVpLoginPresentation(
            Object vp // Wallet 생성 VP 객체
    ) {
        if (vp == null || (vp instanceof String vpText && !StringUtils.hasText(vpText))) {
            throw new ApiException(ErrorCode.VP_LOGIN_PRESENTATION_REQUIRED);
        }

        CorePresentationVerifyRequest apiRequest = new CorePresentationVerifyRequest(
                PRESENTATION_FORMAT_SD_JWT,
                vp,
                Map.<String, Map<String, Object>>of(),
                null,
                true,
                null,
                false,
                DEFAULT_STATUS_MODE
        );

        String endpoint = VERIFY_PRESENTATION_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, null, null, null);
        fields.put("presentationFormat", PRESENTATION_FORMAT_SD_JWT);
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core web VP login verify API call started", fields);
        try {
            ResponseEntity<CorePresentationVerifyResponse> responseEntity = coreRestClient.post()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .body(apiRequest)
                    .retrieve()
                    .toEntity(CorePresentationVerifyResponse.class);
            CorePresentationVerifyResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            logEventLogger.info("core.response.mapped", "Core web VP login verify response mapped", fields);
            return body;
        } catch (RestClientException exception) {
            throw mapCoreException(endpoint, exception, fields);
        }
    }

    @Override
    public CoreCredentialVerificationResponse verifyCredential(
            CoreCredentialVerificationRequest request // Credential 검증 요청
    ) {
        if (request == null || request.credential() == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        VerifyCredentialApiRequest apiRequest = new VerifyCredentialApiRequest(
                request.format(),
                request.credential(),
                request.didDocuments(),
                request.policy(),
                true,
                null,
                false,
                DEFAULT_STATUS_MODE
        );

        String endpoint = VERIFY_CREDENTIAL_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, null, null, null);
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core credential verify API call started", fields);
        try {
            ResponseEntity<VerificationApiResponse> responseEntity = coreRestClient.post()
                    .uri(endpoint)
                    .headers(this::applyApiKeyHeader)
                    .body(apiRequest)
                    .retrieve()
                    .toEntity(VerificationApiResponse.class);
            VerificationApiResponse body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            return new CoreCredentialVerificationResponse(body.ok(), safeErrors(body.errors()), safeDetails(body.details()));
        } catch (RestClientException exception) {
            throw mapCoreException(endpoint, exception, fields);
        }
    }

    @Override
    public CoreDidDocumentResponse getDidDocument(
            String account // XRPL Account
    ) {
        if (!StringUtils.hasText(account)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String endpoint = DID_DOCUMENT_ENDPOINT;
        Map<String, Object> fields = createHttpLogFields(endpoint, null, null, null);
        long startedAt = System.currentTimeMillis();
        logEventLogger.info("core.call.started", "Core DID document API call started", fields);
        try {
            ResponseEntity<Map> responseEntity = coreRestClient.get()
                    .uri(endpoint, account.trim())
                    .headers(this::applyApiKeyHeader)
                    .retrieve()
                    .toEntity(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = requireResponseBody(responseEntity.getBody(), endpoint);
            logHttpCompleted(endpoint, responseEntity.getStatusCode().value(), startedAt, fields);
            return new CoreDidDocumentResponse(account.trim(), body);
        } catch (RestClientException exception) {
            throw mapCoreException(endpoint, exception, fields);
        }
    }

    @Override
    public CoreXrplTransactionResponse getXrplTransaction(
            String txHash // 트랜잭션 해시
    ) {
        throw new ApiException(ErrorCode.CORE_UNSUPPORTED_OPERATION, "Core XRPL 트랜잭션 조회 API가 Swagger에서 확인되지 않았습니다.");
    }

    @Override
    public CoreCredentialSchemaResponse getCredentialSchema(
            String schemaId // 스키마 ID
    ) {
        throw new ApiException(ErrorCode.CORE_UNSUPPORTED_OPERATION, "Core Credential Schema 조회 API가 Swagger에서 확인되지 않았습니다.");
    }

    private void validateAiReviewRequest(
            CoreAiReviewRequest request // AI 심사 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.coreRequestId())
                || request.kycId() == null
                || !StringUtils.hasText(request.corporateTypeCode())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "AI 심사 요청 필수 데이터가 부족합니다.");
        }
    }

    private LlmPrimaryAssessmentApiRequest buildAiAssessmentApiRequest(
            CoreAiReviewRequest request // AI 심사 요청
    ) {
        return new LlmPrimaryAssessmentApiRequest(
                String.valueOf(request.kycId()),
                resolveCoreLegalEntityType(request.corporateTypeCode()),
                resolveCoreApplicantRole(request.agentName()),
                request.representativeName(),
                false,
                request.businessNumber(),
                request.corporateRegistrationNumber(),
                buildDeclaredRepresentative(request),
                List.of(),
                buildAiAssessmentDocuments(request.documents())
        );
    }

    private LlmPrimaryAssessmentApiRequest.DeclaredPersonApiRequest buildDeclaredRepresentative(
            CoreAiReviewRequest request // AI 심사 요청
    ) {
        return new LlmPrimaryAssessmentApiRequest.DeclaredPersonApiRequest(
                request.representativeName(),
                null,
                null,
                null
        );
    }

    private List<LlmPrimaryAssessmentApiRequest.LlmPrimaryDocumentInputApiRequest> buildAiAssessmentDocuments(
            List<CoreAiReviewRequest.CoreAiReviewDocumentRequest> documents // AI 심사 문서 목록
    ) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                .map(document -> new LlmPrimaryAssessmentApiRequest.LlmPrimaryDocumentInputApiRequest(
                        document.documentId() == null ? null : String.valueOf(document.documentId()),
                        document.originalFileName(),
                        document.mimeType(),
                        resolveCoreDocumentType(document.documentTypeCode()),
                        document.storagePath(),
                        document.contentBase64(),
                        null,
                        document.fileSize(),
                        document.documentHash(),
                        null
                ))
                .toList();
    }

    private String resolveCoreLegalEntityType(
            String corporateTypeCode // backend 법인 유형 코드
    ) {
        if (!StringUtils.hasText(corporateTypeCode)) {
            return CORE_LEGAL_ENTITY_STOCK_COMPANY;
        }
        String normalized = corporateTypeCode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case BACKEND_CORPORATE_TYPE_CORPORATION -> CORE_LEGAL_ENTITY_STOCK_COMPANY;
            case BACKEND_CORPORATE_TYPE_LIMITED_COMPANY -> CORE_LEGAL_ENTITY_LIMITED_COMPANY;
            case BACKEND_CORPORATE_TYPE_NON_PROFIT -> CORE_LEGAL_ENTITY_INCORPORATED_ASSOCIATION;
            case BACKEND_CORPORATE_TYPE_ASSOCIATION -> CORE_LEGAL_ENTITY_COOPERATIVE;
            case BACKEND_CORPORATE_TYPE_FOREIGN_COMPANY -> CORE_LEGAL_ENTITY_FOREIGN_COMPANY;
            default -> throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "Core AI 심사 법인 유형 매핑이 없습니다.");
        };
    }

    private String resolveCoreApplicantRole(
            String agentName // 대리인명
    ) {
        return StringUtils.hasText(agentName)
                ? CORE_APPLICANT_ROLE_DELEGATE
                : CORE_APPLICANT_ROLE_REPRESENTATIVE;
    }

    private String resolveCoreDocumentType(
            String documentTypeCode // backend 문서 유형 코드
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            return CORE_DOCUMENT_TYPE_UNKNOWN;
        }
        String normalized = documentTypeCode.trim().toUpperCase(Locale.ROOT);
        if (ALLOWED_CORE_DOCUMENT_TYPES.contains(normalized)) {
            return normalized;
        }
        return switch (normalized) {
            case BACKEND_DOCUMENT_BUSINESS_REGISTRATION -> BACKEND_DOCUMENT_BUSINESS_REGISTRATION;
            case BACKEND_DOCUMENT_CORPORATE_REGISTRATION -> CORE_DOCUMENT_CORPORATE_REGISTRY;
            case BACKEND_DOCUMENT_CORPORATE_SEAL_CERTIFICATE -> CORE_DOCUMENT_SEAL_CERTIFICATE;
            case BACKEND_DOCUMENT_SHAREHOLDER_LIST -> CORE_DOCUMENT_SHAREHOLDER_REGISTRY;
            case BACKEND_DOCUMENT_ARTICLES_OF_INCORPORATION -> CORE_DOCUMENT_ARTICLES_OF_ASSOCIATION;
            case BACKEND_DOCUMENT_POWER_OF_ATTORNEY -> BACKEND_DOCUMENT_POWER_OF_ATTORNEY;
            default -> CORE_DOCUMENT_TYPE_UNKNOWN;
        };
    }

    private CoreAiReviewResponse mapAiReviewResponse(
            CoreAiReviewRequest request, // AI 심사 요청
            LlmPrimaryAssessmentApiResponse body // Core AI 심사 응답
    ) {
        LlmPrimaryAssessmentApiResponse.KycAssessmentApiResponse assessment = body.assessment();
        if (assessment == null || !StringUtils.hasText(assessment.status())) {
            throw new ApiException(ErrorCode.CORE_API_RESPONSE_INVALID, "Core AI 심사 응답 필수 필드가 부족합니다.");
        }
        String assessmentStatus = assessment.status().trim().toUpperCase(Locale.ROOT);
        return new CoreAiReviewResponse(
                request.coreRequestId(),
                resolveAiReviewStatus(assessmentStatus),
                assessmentStatus,
                assessment.assessmentId(),
                assessment.overallConfidence(),
                resolveAiReviewMessage(assessment),
                LocalDateTime.now()
        );
    }

    private CoreAiReviewResponse mapAiReviewResponseSafely(
            CoreAiReviewRequest request, // AI 심사 요청
            String endpoint, // 호출 endpoint
            LlmPrimaryAssessmentApiResponse body, // Core AI 심사 응답
            long startedAt, // 시작 시각
            Map<String, Object> fields // 로그 필드
    ) {
        try {
            return mapAiReviewResponse(request, body);
        } catch (ApiException exception) {
            throw buildAiReviewInvalidResponseException(request, endpoint, startedAt, fields, exception);
        }
    }

    private LlmPrimaryAssessmentApiResponse parseAiReviewResponseBody(
            CoreAiReviewRequest request, // AI 심사 요청
            String endpoint, // 호출 endpoint
            ResponseEntity<String> responseEntity, // Core 문자열 응답
            long startedAt, // 시작 시각
            Map<String, Object> fields // 로그 필드
    ) {
        String responseBody = responseEntity.getBody();
        MediaType contentType = responseEntity.getHeaders().getContentType();
        if (!isJsonContentType(contentType)) {
            throw buildAiReviewInvalidResponseException(
                    request,
                    endpoint,
                    startedAt,
                    fields,
                    null,
                    mediaTypeValue(contentType),
                    summarizeResponseBody(responseBody)
            );
        }
        if (!StringUtils.hasText(responseBody)) {
            throw buildAiReviewInvalidResponseException(
                    request,
                    endpoint,
                    startedAt,
                    fields,
                    null,
                    mediaTypeValue(contentType),
                    null
            );
        }
        try {
            return objectMapper.readValue(responseBody, LlmPrimaryAssessmentApiResponse.class);
        } catch (JsonProcessingException exception) {
            throw buildAiReviewInvalidResponseException(
                    request,
                    endpoint,
                    startedAt,
                    fields,
                    exception,
                    mediaTypeValue(contentType),
                    summarizeResponseBody(responseBody)
            );
        }
    }

    private CoreAiReviewException buildAiReviewTimeoutException(
            CoreAiReviewRequest request, // AI 심사 요청
            String endpoint, // 호출 endpoint
            long startedAt, // 시작 시각
            Map<String, Object> baseFields, // 로그 필드
            Exception exception // 원인 예외
    ) {
        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> fields = aiReviewFailureLogFields(
                baseFields,
                CoreAiReviewException.FailureType.TIMEOUT,
                null,
                null,
                null,
                durationMs,
                exception
        );
        logEventLogger.warn("core.ai_review.timeout", "Core AI review request timed out", fields);
        return new CoreAiReviewException(
                ErrorCode.CORE_API_TIMEOUT,
                "Core AI review request timed out",
                exception,
                endpoint,
                request.coreRequestId(),
                request.kycId(),
                request.corporateId(),
                CoreAiReviewException.FailureType.TIMEOUT,
                null,
                null,
                null,
                durationMs,
                coreProperties.resolvedAiReviewReadTimeoutSeconds()
        );
    }

    private CoreAiReviewException buildAiReviewInvalidResponseException(
            CoreAiReviewRequest request, // AI 심사 요청
            String endpoint, // 호출 endpoint
            long startedAt, // 시작 시각
            Map<String, Object> baseFields, // 로그 필드
            Exception exception // 원인 예외
    ) {
        return buildAiReviewInvalidResponseException(request, endpoint, startedAt, baseFields, exception, null, null);
    }

    private CoreAiReviewException buildAiReviewInvalidResponseException(
            CoreAiReviewRequest request, // AI 심사 요청
            String endpoint, // 호출 endpoint
            long startedAt, // 시작 시각
            Map<String, Object> baseFields, // 로그 필드
            Exception exception, // 원인 예외
            String contentType, // 응답 Content-Type
            String responseBodySummary // 응답 요약
    ) {
        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> fields = aiReviewFailureLogFields(
                baseFields,
                CoreAiReviewException.FailureType.INVALID_RESPONSE,
                null,
                contentType,
                responseBodySummary,
                durationMs,
                exception
        );
        logEventLogger.warn("core.ai_review.invalid_response", "Core AI review response is not valid JSON", fields);
        return new CoreAiReviewException(
                ErrorCode.CORE_API_RESPONSE_INVALID,
                "Core AI review response is not valid JSON",
                exception,
                endpoint,
                request.coreRequestId(),
                request.kycId(),
                request.corporateId(),
                CoreAiReviewException.FailureType.INVALID_RESPONSE,
                null,
                contentType,
                responseBodySummary,
                durationMs,
                coreProperties.resolvedAiReviewReadTimeoutSeconds()
        );
    }

    private CoreAiReviewException buildAiReviewCoreErrorException(
            CoreAiReviewRequest request, // AI 심사 요청
            String endpoint, // 호출 endpoint
            long startedAt, // 시작 시각
            Map<String, Object> baseFields, // 로그 필드
            RestClientResponseException exception // Core 응답 예외
    ) {
        int statusCode = exception.getStatusCode().value();
        String contentType = exception.getResponseHeaders() == null
                ? null
                : mediaTypeValue(exception.getResponseHeaders().getContentType());
        String responseBodySummary = summarizeResponseBody(exception.getResponseBodyAsString());
        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> fields = aiReviewFailureLogFields(
                baseFields,
                CoreAiReviewException.FailureType.CORE_ERROR,
                statusCode,
                contentType,
                responseBodySummary,
                durationMs,
                exception
        );
        if (statusCode >= 500) {
            logEventLogger.error("core.ai_review.failed", "Core AI review request failed", fields, exception);
        } else {
            logEventLogger.warn("core.ai_review.failed", "Core AI review request failed", fields);
        }
        return new CoreAiReviewException(
                ErrorCode.CORE_AI_REVIEW_FAILED,
                "Core AI review request failed",
                exception,
                endpoint,
                request.coreRequestId(),
                request.kycId(),
                request.corporateId(),
                CoreAiReviewException.FailureType.CORE_ERROR,
                statusCode,
                contentType,
                responseBodySummary,
                durationMs,
                coreProperties.resolvedAiReviewReadTimeoutSeconds()
        );
    }

    private CoreAiReviewException buildAiReviewCoreErrorException(
            CoreAiReviewRequest request, // AI 심사 요청
            String endpoint, // 호출 endpoint
            long startedAt, // 시작 시각
            Map<String, Object> baseFields, // 로그 필드
            Exception exception // 원인 예외
    ) {
        long durationMs = System.currentTimeMillis() - startedAt;
        Map<String, Object> fields = aiReviewFailureLogFields(
                baseFields,
                CoreAiReviewException.FailureType.CORE_ERROR,
                null,
                null,
                null,
                durationMs,
                exception
        );
        logEventLogger.error("core.ai_review.failed", "Core AI review request failed", fields, exception);
        return new CoreAiReviewException(
                ErrorCode.CORE_AI_REVIEW_FAILED,
                "Core AI review request failed",
                exception,
                endpoint,
                request.coreRequestId(),
                request.kycId(),
                request.corporateId(),
                CoreAiReviewException.FailureType.CORE_ERROR,
                null,
                null,
                null,
                durationMs,
                coreProperties.resolvedAiReviewReadTimeoutSeconds()
        );
    }

    private Map<String, Object> aiReviewFailureLogFields(
            Map<String, Object> baseFields, // 기본 로그 필드
            CoreAiReviewException.FailureType failureType, // 실패 유형
            Integer statusCode, // Core 응답 상태 코드
            String contentType, // Core 응답 Content-Type
            String responseBodySummary, // Core 응답 요약
            long durationMs, // 요청 소요 시간
            Exception exception // 원인 예외
    ) {
        Map<String, Object> fields = new LinkedHashMap<>(baseFields);
        fields.put("failureType", failureType.name());
        fields.put("statusCode", statusCode);
        fields.put("contentType", contentType);
        fields.put("durationMs", durationMs);
        fields.put("responseBodySummary", responseBodySummary);
        fields.put("exceptionClass", exception == null ? null : exception.getClass().getName());
        fields.put("configuredAiReviewTimeoutSeconds", coreProperties.resolvedAiReviewReadTimeoutSeconds());
        return fields;
    }

    private boolean isJsonContentType(
            MediaType contentType // 응답 Content-Type
    ) {
        if (contentType == null) {
            return false;
        }
        return MediaType.APPLICATION_JSON.includes(contentType)
                || contentType.getSubtype().toLowerCase(Locale.ROOT).endsWith("+json");
    }

    private String mediaTypeValue(
            MediaType contentType // 응답 Content-Type
    ) {
        return contentType == null ? null : contentType.toString();
    }

    private String summarizeResponseBody(
            String responseBody // Core 응답 body
    ) {
        String sanitized = corePayloadSanitizer.sanitizeText(responseBody);
        if (sanitized == null || sanitized.length() <= 500) {
            return sanitized;
        }
        return sanitized.substring(0, 500) + "...[TRUNCATED]";
    }

    private String resolveAiReviewStatus(
            String assessmentStatus // Core 심사 상태
    ) {
        return switch (assessmentStatus) {
            case CORE_ASSESSMENT_NORMAL -> KyvcEnums.AiReviewStatus.SUCCESS.name();
            case CORE_ASSESSMENT_SUPPLEMENT_REQUIRED, CORE_ASSESSMENT_MANUAL_REVIEW_REQUIRED, CORE_ASSESSMENT_REJECTED ->
                    KyvcEnums.AiReviewStatus.LOW_CONFIDENCE.name();
            default -> KyvcEnums.AiReviewStatus.FAILED.name();
        };
    }

    private String resolveAiReviewMessage(
            LlmPrimaryAssessmentApiResponse.KycAssessmentApiResponse assessment // Core 심사 결과
    ) {
        if (StringUtils.hasText(assessment.summary())) {
            return assessment.summary().trim();
        }
        if (StringUtils.hasText(assessment.status())) {
            return "Core AI 심사 상태: " + assessment.status().trim();
        }
        return "Core AI 심사가 완료되었습니다.";
    }

    private void validateVcIssuanceRequest(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        if (request == null || !StringUtils.hasText(request.coreRequestId())) {
            throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "VC 발급 요청 필수 데이터가 부족합니다.");
        }
    }

    private String resolveIssuerAccount(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        if (StringUtils.hasText(request.issuerAccount())) {
            return request.issuerAccount().trim();
        }
        String issuerFromDid = resolveAccountFromDid(request.issuerDid());
        if (StringUtils.hasText(issuerFromDid)) {
            return issuerFromDid;
        }
        return resolveDevSeedOrThrow("issuerAccount", CoreMockSeedData.DEV_ISSUER_ACCOUNT);
    }

    private String resolveIssuerDid(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        if (StringUtils.hasText(request.issuerDid())) {
            return request.issuerDid().trim();
        }
        return resolveDevSeedOrThrow("issuerDid", CoreMockSeedData.DEV_ISSUER_DID);
    }

    private String resolveIssuerVerificationMethodId(
            CoreVcIssuanceRequest request, // VC 발급 요청
            String issuerDid // Issuer DID
    ) {
        if (StringUtils.hasText(request.issuerVerificationMethodId())) {
            return request.issuerVerificationMethodId().trim();
        }
        if (StringUtils.hasText(request.keyId()) && StringUtils.hasText(issuerDid)) {
            return issuerDid + "#" + request.keyId().trim();
        }
        if (StringUtils.hasText(issuerDid)) {
            return issuerDid + "#issuer-key-1";
        }
        return resolveDevSeedOrThrow("issuerVerificationMethodId", CoreMockSeedData.DEV_ISSUER_VERIFICATION_METHOD_ID);
    }

    private String resolveKeyId(
            CoreVcIssuanceRequest request, // VC 발급 요청
            String issuerVerificationMethodId // Issuer Verification Method ID
    ) {
        if (StringUtils.hasText(request.keyId())) {
            return request.keyId().trim();
        }
        return extractKeyId(issuerVerificationMethodId);
    }

    private String resolveHolderAccount(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        if (StringUtils.hasText(request.holderAccount())) {
            return request.holderAccount().trim();
        }
        return resolveDevSeedOrThrow("holderAccount", CoreMockSeedData.DEV_HOLDER_ACCOUNT);
    }

    private String resolveHolderDid(
            CoreVcIssuanceRequest request, // VC 발급 요청
            String holderAccount // Holder XRPL Account
    ) {
        if (StringUtils.hasText(request.holderDid())) {
            return request.holderDid().trim();
        }
        if (StringUtils.hasText(holderAccount)) {
            return "did:xrpl:1:" + holderAccount;
        }
        return resolveDevSeedOrThrow("holderDid", CoreMockSeedData.DEV_HOLDER_DID);
    }

    private Map<String, Object> resolveClaims(
            Map<String, Object> claims // VC Claim 데이터
    ) {
        if (claims != null && !claims.isEmpty()) {
            return claims;
        }
        if (coreProperties.isDevSeedEnabled()) {
            return CoreMockSeedData.legalEntityClaims();
        }
        throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "VC 발급 claims 데이터가 부족합니다.");
    }

    private OffsetDateTime resolveValidFrom(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        return request.validFrom() == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : request.validFrom();
    }

    private OffsetDateTime resolveValidUntil(
            CoreVcIssuanceRequest request, // VC 발급 요청
            OffsetDateTime validFrom // VC 유효 시작 시각
    ) {
        return request.validUntil() == null
                ? validFrom.plusYears(1)
                : request.validUntil();
    }

    private Boolean resolveBoolean(
            Boolean value, // 원본 여부 값
            boolean defaultValue // 기본 여부 값
    ) {
        return value == null ? defaultValue : value;
    }

    private String resolveAllowedValue(
            String value, // 원본 값
            String defaultValue, // 기본 값
            Set<String> allowedValues, // 허용 값 목록
            String fieldName // 필드명
    ) {
        String resolved = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : defaultValue;
        if (allowedValues.contains(resolved)) {
            return resolved;
        }
        throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "Core 요청 " + fieldName + " 값이 올바르지 않습니다.");
    }

    private String resolveHolderKeyId(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        if (StringUtils.hasText(request.holderKeyId())) {
            return request.holderKeyId().trim();
        }
        if (StringUtils.hasText(request.holderDid())) {
            return request.holderDid().trim() + "#holder-key-1";
        }
        if (coreProperties.isDevSeedEnabled()) {
            return CoreMockSeedData.DEV_HOLDER_KEY_ID;
        }
        throw new ApiException(ErrorCode.CORE_REQUIRED_DATA_MISSING, "VC 발급 Holder 키 식별자가 없습니다.");
    }

    private String resolveVct(
            CoreVcIssuanceRequest request // VC 발급 요청
    ) {
        String vct = normalizeOptional(request.vct());
        return vct == null ? normalizeOptional(request.credentialType()) : vct;
    }

    private String normalizeOptional(
            String value // 원본 문자열
    ) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String resolveDevSeedOrThrow(
            String fieldName, // 누락 필드명
            String seedValue // 대체 seed 값
    ) {
        if (coreProperties.isDevSeedEnabled()) {
            logEventLogger.warn(
                    "core.dev-seed.used",
                    "Core 개발 seed 데이터 사용",
                    Map.of("fieldName", fieldName, "devSeedUsed", true)
            );
            return seedValue;
        }
        throw new ApiException(ErrorCode.CORE_DEV_SEED_DISABLED, "Core 요청 필수 데이터 누락: " + fieldName);
    }

    private CoreVcIssuanceResponse mapVcIssuanceResponse(
            CoreVcIssuanceRequest request, // VC 발급 요청
            IssueKycCredentialApiResponse body // Core 응답 DTO
    ) {
        Map<String, Object> status = safeDetails(body.status());
        Map<String, Object> tx = safeDetails(body.credentialCreateTransaction());
        Map<String, Object> ledgerEntry = safeDetails(body.ledgerEntry());
        String statusCode = mapCredentialStatusFromStatusObject(status);
        String credentialStatusId = resolveCredentialStatusId(body, status);
        String credentialExternalId = StringUtils.hasText(body.credentialId())
                ? body.credentialId()
                : extractString(status, "credential_id", "credentialId", "jti");
        ParsedCredentialStatusId parsedStatusId = parseCredentialStatusId(credentialStatusId);
        String issuerDid = extractString(status, "issuer_did", "issuerDid");
        String issuerAccount = resolveIssuerAccount(body, status, tx, ledgerEntry, issuerDid, parsedStatusId);
        String actualIssuerDid = resolveIssuerDid(issuerAccount, issuerDid, request.issuerDid());
        String actualCredentialType = parsedStatusId == null
                ? body.credentialType()
                : parsedStatusId.credentialType();
        String vcHash = StringUtils.hasText(body.vcCoreHash()) ? body.vcCoreHash() : extractString(status, "vc_hash", "vcHash");
        String xrplTxHash = extractString(tx, "hash", "tx_hash", "transaction_hash");
        LocalDateTime issuedAt = parseDateTime(extractString(status, "issued_at", "issuedAt", "created_at"));
        LocalDateTime expiresAt = parseDateTime(extractString(status, "expires_at", "expiresAt", "valid_until"));
        String format = StringUtils.hasText(body.format()) ? body.format().trim() : null;
        String credentialPayloadJson = null;
        String credentialJwt = null;
        Object credential = body.credential();
        if (credential instanceof String credentialString) {
            credentialJwt = credentialString;
        } else if (credential != null) {
            credentialPayloadJson = serializeCredentialPayload(credential);
        }

        return new CoreVcIssuanceResponse(
                request.coreRequestId(),
                statusCode,
                "Core VC 발급 API 호출 성공",
                LocalDateTime.now(),
                credentialExternalId,
                actualCredentialType,
                StringUtils.hasText(actualIssuerDid) ? actualIssuerDid : request.issuerDid(),
                issuerAccount,
                format,
                credentialPayloadJson,
                credentialJwt,
                vcHash,
                xrplTxHash,
                credentialStatusId,
                issuedAt,
                expiresAt,
                body.selectiveDisclosure()
        );
    }

    private String resolveIssuerAccount(
            IssueKycCredentialApiResponse body, // Core 발급 응답
            Map<String, Object> status, // 상태 객체
            Map<String, Object> tx, // CredentialCreate 트랜잭션 객체
            Map<String, Object> ledgerEntry, // Ledger entry 객체
            String issuerDid, // Issuer DID
            ParsedCredentialStatusId parsedStatusId // Credential Status ID
    ) {
        Map<String, Object> txJson = extractMap(tx, "tx_json");
        return firstXrplClassicAddress(
                parsedStatusId == null ? null : parsedStatusId.issuerAccount(),
                extractString(tx, "Account"),
                extractString(tx, "account"),
                extractString(tx, "Issuer"),
                extractString(tx, "issuer"),
                extractString(txJson, "Account"),
                extractString(txJson, "account"),
                extractString(txJson, "Issuer"),
                extractString(txJson, "issuer"),
                extractString(ledgerEntry, "Issuer"),
                extractString(ledgerEntry, "issuer"),
                extractString(ledgerEntry, "Account"),
                extractString(ledgerEntry, "issuerAccount"),
                extractString(ledgerEntry, "issuer_account"),
                extractString(status, "Issuer"),
                extractString(status, "issuer"),
                extractString(status, "issuerAccount"),
                extractString(status, "issuer_account"),
                body.issuerAccount(),
                body.issuerAccountSnake(),
                body.issuer(),
                accountFromDid(issuerDid)
        );
    }

    private String resolveIssuerDid(
            String issuerAccount, // Issuer XRPL Account
            String issuerDid, // Core Issuer DID
            String fallbackIssuerDid // 요청 Issuer DID
    ) {
        String normalizedIssuerAccount = firstXrplClassicAddress(issuerAccount);
        if (StringUtils.hasText(normalizedIssuerAccount)) {
            return "did:xrpl:1:" + normalizedIssuerAccount;
        }
        String normalizedIssuerDid = normalizeOptional(issuerDid);
        if (StringUtils.hasText(accountFromDid(normalizedIssuerDid))) {
            return normalizedIssuerDid;
        }
        return normalizeOptional(fallbackIssuerDid);
    }

    private String resolveCredentialStatusId(
            IssueKycCredentialApiResponse body, // Core 발급 응답
            Map<String, Object> status // Credential status 객체
    ) {
        String[] candidates = new String[]{
                body.credentialStatusId(),
                body.credentialStatusIdSnake(),
                body.statusId(),
                extractString(status, "credentialStatusId"),
                extractString(status, "credential_status_id"),
                extractString(status, "status_id"),
                extractString(status, "statusId"),
                extractString(status, "id")
        };
        for (String candidate : candidates) {
            String normalized = normalizeOptional(candidate);
            if (parseCredentialStatusId(normalized) != null) {
                return normalized;
            }
        }
        return firstText(candidates);
    }

    private String firstText(
            String... values // 문자열 후보
    ) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeOptional(value);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String firstXrplClassicAddress(
            String... values // XRPL classic 주소 후보
    ) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeOptional(value);
            if (isXrplClassicAddress(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String accountFromDid(
            String did // DID 문자열
    ) {
        String normalizedDid = normalizeOptional(did);
        String prefix = "did:xrpl:1:";
        if (normalizedDid == null || !normalizedDid.startsWith(prefix)) {
            return null;
        }
        String account = normalizedDid.substring(prefix.length());
        return isXrplClassicAddress(account) ? account : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(
            Map<String, Object> source, // 원본 Map
            String key // 조회 키
    ) {
        if (source == null) {
            return Map.of();
        }
        Object value = source.get(key);
        return value instanceof Map<?, ?> mapValue ? (Map<String, Object>) mapValue : Map.of();
    }

    private ParsedCredentialStatusId parseCredentialStatusId(
            String credentialStatusId // Credential 상태 ID
    ) {
        if (!StringUtils.hasText(credentialStatusId)) {
            return null;
        }

        String[] parts = credentialStatusId.trim().split(":");
        if (parts.length < 5
                || !DEFAULT_STATUS_MODE.equalsIgnoreCase(parts[0])
                || !CREDENTIAL_STATUS_ID_TYPE.equalsIgnoreCase(parts[1])) {
            return null;
        }

        String issuerAccount = parts[2].trim(); // Issuer XRPL 계정
        String holderAccount = parts[3].trim(); // Holder XRPL 계정
        String credentialType = String.join(":", java.util.Arrays.copyOfRange(parts, 4, parts.length)).trim(); // Credential 유형
        if (!isXrplClassicAddress(issuerAccount)
                || !isXrplClassicAddress(holderAccount)
                || !StringUtils.hasText(credentialType)) {
            return null;
        }
        return new ParsedCredentialStatusId(issuerAccount, holderAccount, credentialType);
    }

    private boolean isXrplClassicAddress(
            String value // XRPL classic 주소
    ) {
        return StringUtils.hasText(value)
                && value.startsWith("r")
                && value.length() >= 25
                && value.length() <= 35;
    }

    private record ParsedCredentialStatusId(
            String issuerAccount, // Issuer XRPL 계정
            String holderAccount, // Holder XRPL 계정
            String credentialType // Credential 유형
    ) {
    }

    private String serializeCredentialPayload(
            Object credential // Core credential 객체
    ) {
        try {
            return objectMapper.writeValueAsString(credential);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CORE_API_CALL_FAILED, "Core credential 원문 직렬화 실패", exception);
        }
    }

    private String mapCredentialStatusCode(
            CredentialStatusApiResponse body // Core 상태 응답
    ) {
        if (!body.found()) {
            return null;
        }
        if (body.active()) {
            return KyvcEnums.CredentialStatus.VALID.name();
        }
        Map<String, Object> entry = safeDetails(body.entry());
        if (extractBoolean(entry, "suspended", "is_suspended")) {
            return KyvcEnums.CredentialStatus.SUSPENDED.name();
        }
        if (extractBoolean(entry, "expired", "is_expired")) {
            return KyvcEnums.CredentialStatus.EXPIRED.name();
        }
        if (extractBoolean(entry, "revoked", "is_revoked")) {
            return KyvcEnums.CredentialStatus.REVOKED.name();
        }
        String entryStatus = extractString(entry, "status", "state");
        if (!StringUtils.hasText(entryStatus)) {
            return null;
        }
        try {
            return KyvcEnums.CredentialStatus.valueOf(entryStatus.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String mapCredentialStatusFromStatusObject(
            Map<String, Object> status // Core status 객체
    ) {
        if (extractBoolean(status, "active")) {
            return KyvcEnums.CredentialStatus.VALID.name();
        }
        if (extractBoolean(status, "suspended")) {
            return KyvcEnums.CredentialStatus.SUSPENDED.name();
        }
        if (extractBoolean(status, "expired")) {
            return KyvcEnums.CredentialStatus.EXPIRED.name();
        }
        if (extractBoolean(status, "revoked")) {
            return KyvcEnums.CredentialStatus.REVOKED.name();
        }
        return KyvcEnums.CredentialStatus.VALID.name();
    }

    private CoreVpVerificationResponse mapVpVerificationResponse(
            CoreVpVerificationRequest request, // VP 검증 요청
            VerificationApiResponse body // Core 검증 응답
    ) {
        List<String> errors = safeErrors(body.errors());
        Map<String, Object> details = safeDetails(body.details());
        boolean replaySuspected = containsReplaySignal(errors, details);
        if (body.ok()) {
            return new CoreVpVerificationResponse(
                    request.coreRequestId(),
                    KyvcEnums.VpVerificationStatus.VALID.name(),
                    "Core VP 검증 성공",
                    LocalDateTime.now(),
                    true,
                    true,
                    false,
                    resolveSummary(errors, details, "VP 검증 성공")
            );
        }
        if (replaySuspected) {
            return new CoreVpVerificationResponse(
                    request.coreRequestId(),
                    KyvcEnums.VpVerificationStatus.REPLAY_SUSPECTED.name(),
                    "Core VP 검증 Replay 의심",
                    LocalDateTime.now(),
                    true,
                    false,
                    true,
                    resolveSummary(errors, details, "VP Replay 의심")
            );
        }
        return new CoreVpVerificationResponse(
                request.coreRequestId(),
                KyvcEnums.VpVerificationStatus.INVALID.name(),
                "Core VP 검증 실패",
                LocalDateTime.now(),
                true,
                false,
                false,
                resolveSummary(errors, details, "VP 검증 실패")
        );
    }

    private void applyApiKeyHeader(
            HttpHeaders headers // HTTP 헤더
    ) {
        String apiKey = coreProperties.resolveApiKey();
        if (StringUtils.hasText(apiKey)) {
            headers.set(CORE_API_KEY_HEADER, apiKey);
            headers.set(INTERNAL_API_KEY_HEADER, apiKey);
        }
    }

    private <T> T requireResponseBody(
            T responseBody, // 응답 body
            String endpoint // 호출 endpoint
    ) {
        if (responseBody == null) {
            throw new ApiException(ErrorCode.CORE_API_RESPONSE_INVALID, "Core 응답 body 누락: " + endpoint);
        }
        return responseBody;
    }

    private ApiException mapCoreException(
            String endpoint, // 호출 endpoint
            Exception exception, // 원본 예외
            Map<String, Object> baseFields // 로그 필드
    ) {
        Map<String, Object> fields = new LinkedHashMap<>(baseFields);
        if (exception instanceof RestClientResponseException responseException) {
            int httpStatus = responseException.getStatusCode().value();
            fields.put("httpStatus", httpStatus);
            fields.put("responseBody", corePayloadSanitizer.sanitizeText(responseException.getResponseBodyAsString()));
            if (httpStatus >= 500) {
                logEventLogger.error("core.call.failed", "Core API call failed", fields, exception);
            } else {
                logEventLogger.warn("core.call.failed", "Core API call failed", fields);
            }
            return new ApiException(
                    ErrorCode.CORE_API_CALL_FAILED,
                    "Core API 호출 실패 [" + endpoint + "], status=" + httpStatus,
                    exception
            );
        }
        if (exception instanceof ResourceAccessException && isTimeoutException(exception)) {
            logEventLogger.warn("core.call.timeout", "Core API call timeout", fields);
            return new ApiException(ErrorCode.CORE_API_TIMEOUT, "Core API 타임아웃 [" + endpoint + "]", exception);
        }
        logEventLogger.error("core.call.failed", "Core API call failed", fields, exception);
        return new ApiException(ErrorCode.CORE_API_CALL_FAILED, "Core API 호출 실패 [" + endpoint + "]", exception);
    }

    private boolean isTimeoutException(
            Throwable throwable // 원본 예외
    ) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void logHttpCompleted(
            String endpoint, // 호출 endpoint
            int httpStatus, // HTTP 상태 코드
            long startedAt, // 시작 시각
            Map<String, Object> baseFields // 로그 필드
    ) {
        Map<String, Object> fields = new LinkedHashMap<>(baseFields);
        fields.put("httpStatus", httpStatus);
        fields.put("durationMillis", System.currentTimeMillis() - startedAt);
        logEventLogger.info("core.call.completed", "Core API call completed", fields);
    }

    private Map<String, Object> createHttpLogFields(
            String endpoint, // 호출 endpoint
            String coreRequestId, // Core 요청 ID
            Long credentialId, // Credential ID
            Long vpVerificationId // VP 검증 ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("endpoint", endpoint);
        fields.put("coreRequestId", coreRequestId);
        fields.put("credentialId", credentialId);
        fields.put("vpVerificationId", vpVerificationId);
        return fields;
    }

    private String resolveAccountFromDid(
            String did // DID 문자열
    ) {
        if (!StringUtils.hasText(did)) {
            return null;
        }
        String prefix = "did:xrpl:1:";
        String normalized = did.trim();
        if (!normalized.startsWith(prefix)) {
            return null;
        }
        String account = normalized.substring(prefix.length()).trim();
        return StringUtils.hasText(account) ? account : null;
    }

    private String extractKeyId(
            String verificationMethodId // Verification Method ID
    ) {
        if (!StringUtils.hasText(verificationMethodId)) {
            return "issuer-key-1";
        }
        String normalized = verificationMethodId.trim();
        int hashIndex = normalized.lastIndexOf('#');
        if (hashIndex < 0 || hashIndex + 1 >= normalized.length()) {
            return normalized;
        }
        return normalized.substring(hashIndex + 1);
    }

    private List<String> parseRequiredClaims(
            String requiredClaimsJson // claim JSON 문자열
    ) {
        if (!StringUtils.hasText(requiredClaimsJson)) {
            return CoreMockSeedData.requiredClaims();
        }
        try {
            return objectMapper.readValue(requiredClaimsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return CoreMockSeedData.requiredClaims();
        }
    }

    private String extractString(
            Map<String, Object> source, // 대상 Map
            String... keys // 조회 키 후보
    ) {
        if (source == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                return stringValue.trim();
            }
            if (value != null && !(value instanceof Map<?, ?>) && !(value instanceof List<?>)) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private boolean extractBoolean(
            Map<String, Object> source, // 대상 Map
            String... keys // 조회 키 후보
    ) {
        if (source == null || keys == null) {
            return false;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
            if (value instanceof String stringValue && StringUtils.hasText(stringValue)) {
                return Boolean.parseBoolean(stringValue.trim());
            }
            if (value instanceof Number numberValue) {
                return numberValue.intValue() != 0;
            }
        }
        return false;
    }

    private List<String> safeErrors(
            List<String> errors // 원본 오류 목록
    ) {
        return errors == null ? List.of() : errors;
    }

    private Map<String, Object> safeDetails(
            Map<String, Object> details // 원본 상세 Map
    ) {
        return details == null ? Map.of() : details;
    }

    private boolean containsReplaySignal(
            List<String> errors, // 오류 목록
            Map<String, Object> details // 상세 Map
    ) {
        for (String error : safeErrors(errors)) {
            if (StringUtils.hasText(error) && error.toLowerCase(Locale.ROOT).contains("replay")) {
                return true;
            }
        }
        String summary = extractString(details, "summary", "reason", "message", "error");
        return StringUtils.hasText(summary) && summary.toLowerCase(Locale.ROOT).contains("replay");
    }

    private String resolveSummary(
            List<String> errors, // 오류 목록
            Map<String, Object> details, // 상세 Map
            String defaultSummary // 기본 요약 문구
    ) {
        String summary = extractString(details, "summary", "reason", "message", "error");
        if (StringUtils.hasText(summary)) {
            return summary;
        }
        if (!safeErrors(errors).isEmpty() && StringUtils.hasText(errors.get(0))) {
            return errors.get(0);
        }
        return defaultSummary;
    }

    private LocalDateTime parsePresentationChallengeExpiresAt(
            String value // VP Challenge 만료 일시 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        try {
            return Instant.parse(normalized)
                    .atZone(KST_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignoreInstant) {
            try {
                return OffsetDateTime.parse(normalized)
                        .atZoneSameInstant(KST_ZONE)
                        .toLocalDateTime();
            } catch (DateTimeParseException ignoreOffsetDateTime) {
                try {
                    return LocalDateTime.parse(normalized);
                } catch (DateTimeParseException ignoreLocalDateTime) {
                    return null;
                }
            }
        }
    }

    private LocalDateTime parseDateTime(
            String value // 날짜 문자열
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ignoreLocalDateTime) {
            try {
                return OffsetDateTime.parse(value.trim()).toLocalDateTime();
            } catch (DateTimeParseException ignoreOffsetDateTime) {
                return null;
            }
        }
    }
}
