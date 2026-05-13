package com.kyvc.backend.domain.auth.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.auth.dto.WebVpLoginCompleteResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginResolveRequest;
import com.kyvc.backend.domain.auth.dto.WebVpLoginResolveResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginStartResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginStatusResponse;
import com.kyvc.backend.domain.auth.dto.WebVpLoginSubmitRequest;
import com.kyvc.backend.domain.auth.dto.WebVpLoginSubmitResponse;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeResponse;
import com.kyvc.backend.domain.core.dto.CorePresentationVerifyResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.user.domain.User;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 웹 VP 로그인 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class WebVpLoginService {

    private static final String VP_LOGIN_SESSION_COOKIE_NAME = "vp_login_session";
    private static final String QR_PAYLOAD_TYPE = "VP_LOGIN_REQUEST";
    private static final String PURPOSE = "WEB_VP_LOGIN";
    private static final String DEFINITION_ID = "kyvc-corporate-web-vp-login-v1";
    private static final String AUD = "kyvc-corporate-web-login";
    private static final String DOMAIN = "kyvc-corporate-web-login";
    private static final String CHALLENGE_FORMAT = "dc+sd-jwt";
    private static final String VERIFY_FORMAT = "kyvc-sd-jwt-presentation-v1";
    private static final String ACCEPTED_VCT = "https://kyvc.example/vct/legal-entity-kyc-v1";
    private static final String ACCEPTED_JURISDICTION = "KR";
    private static final String MINIMUM_ASSURANCE_LEVEL = "STANDARD";
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<String> REQUIRED_DISCLOSURES = List.of(
            "legalEntity.name",
            "legalEntity.registrationNumber"
    );
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final VpVerificationRepository vpVerificationRepository;
    private final CredentialRepository credentialRepository;
    private final CorporateRepository corporateRepository;
    private final UserRepository userRepository;
    private final CoreAdapter coreAdapter;
    private final AuthService authService;
    private final TokenCookieUtil tokenCookieUtil;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // 웹 VP 로그인 요청 생성
    public WebVpLoginStartResponse start(
            HttpServletResponse response // HTTP 응답
    ) {
        CorePresentationChallengeResponse coreResponse = issueChallenge();
        validateChallengeResponse(coreResponse);

        LocalDateTime now = LocalDateTime.now();
        String requestId = createRequestId();
        String qrToken = createOpaqueToken();
        String browserSessionToken = createOpaqueToken();
        String qrTokenHash = TokenHashUtil.sha256(qrToken);
        String browserSessionHash = TokenHashUtil.sha256(browserSessionToken);

        VpVerification vpVerification = VpVerification.createWebVpLoginRequest(
                requestId,
                qrTokenHash,
                browserSessionHash,
                coreResponse.nonce(),
                coreResponse.challenge(),
                coreResponse.domain(),
                toJson(REQUIRED_DISCLOSURES),
                coreResponse.expiresAt(),
                createMetadataJson(coreResponse)
        );
        VpVerification saved = vpVerificationRepository.save(vpVerification);
        addCookie(response, tokenCookieUtil.createHttpOnlyCookie(
                VP_LOGIN_SESSION_COOKIE_NAME,
                browserSessionToken,
                resolveSessionMaxAge(now, coreResponse.expiresAt())
        ));

        logEventLogger.info("auth.web_vp_login.request.created", "Web VP login request created", Map.of(
                "vpVerificationId", saved.getVpVerificationId(),
                "requestId", saved.getVpRequestId(),
                "status", saved.getVpVerificationStatus().name()
        ));

        return new WebVpLoginStartResponse(
                saved.getVpRequestId(),
                new WebVpLoginStartResponse.QrPayload(QR_PAYLOAD_TYPE, saved.getVpRequestId(), qrToken),
                toKstOffsetString(saved.getExpiresAt())
        );
    }

    // 모바일 QR 해석
    @Transactional(noRollbackFor = ApiException.class)
    public WebVpLoginResolveResponse resolve(
            WebVpLoginResolveRequest request // QR 해석 요청
    ) {
        requireText(request == null ? null : request.qrToken(), ErrorCode.INVALID_REQUEST);
        String qrTokenHash = TokenHashUtil.sha256(request.qrToken().trim());
        VpVerification vpVerification = vpVerificationRepository.findByQrTokenHash(qrTokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_REQUEST_NOT_FOUND));
        validateWebVpLoginRequest(vpVerification);
        validateNotCompleted(vpVerification);
        validateNotExpired(vpVerification, LocalDateTime.now());

        Map<String, Object> metadata = readMetadata(vpVerification);
        return new WebVpLoginResolveResponse(
                QR_PAYLOAD_TYPE,
                vpVerification.getVpRequestId(),
                vpVerification.getRequestNonce(),
                vpVerification.getChallenge(),
                resolveText(metadata.get("aud"), AUD),
                resolveText(metadata.get("domain"), DOMAIN),
                resolvePresentationDefinition(metadata),
                REQUIRED_DISCLOSURES,
                toKstOffsetString(vpVerification.getExpiresAt())
        );
    }

    // 모바일 VP 제출
    @Transactional(noRollbackFor = ApiException.class)
    public WebVpLoginSubmitResponse submit(
            String requestId, // VP 로그인 요청 ID
            WebVpLoginSubmitRequest request // VP 제출 요청
    ) {
        VpVerification vpVerification = getWebVpLoginRequest(requestId);
        requireText(request == null ? null : request.qrToken(), ErrorCode.INVALID_REQUEST);
        String qrTokenHash = TokenHashUtil.sha256(request.qrToken().trim());
        if (!vpVerification.matchesQrTokenHash(qrTokenHash)) {
            throw new ApiException(ErrorCode.VP_LOGIN_QR_TOKEN_INVALID);
        }
        validateNotCompleted(vpVerification);
        validateNotExpired(vpVerification, LocalDateTime.now());
        Credential credential = getCredentialForVpLogin(vpVerification.getVpRequestId(), request == null ? null : request.credentialId());
        validateCredential(vpVerification, credential);
        requirePresentation(request.vp());

        String vpHash = TokenHashUtil.sha256(toJson(request.vp()));
        LocalDateTime now = LocalDateTime.now();
        vpVerification.markWebVpLoginPresented(credential.getCorporateId(), credential.getCredentialId(), vpHash, now);
        vpVerificationRepository.save(vpVerification);

        CorePresentationVerifyResponse coreResponse = requestCoreVerify(vpVerification, request.vp());
        boolean verified = coreResponse != null && coreResponse.isVerified();
        if (!verified) {
            vpVerification.markInvalid(resolveCoreVerifySummary(coreResponse, "웹 VP 로그인 Core 검증 실패"), LocalDateTime.now());
            VpVerification saved = vpVerificationRepository.save(vpVerification);
            logEventLogger.info("auth.web_vp_login.presentation.invalid", "Web VP login presentation invalid", Map.of(
                    "vpVerificationId", saved.getVpVerificationId(),
                    "requestId", saved.getVpRequestId(),
                    "credentialId", saved.getCredentialId(),
                    "corporateId", saved.getCorporateId(),
                    "status", saved.getVpVerificationStatus().name()
            ));
            throw new ApiException(ErrorCode.VP_LOGIN_CORE_VERIFY_FAILED);
        }
        vpVerification.markValid(resolveCoreVerifySummary(coreResponse, "웹 VP 로그인 Core 검증 성공"), LocalDateTime.now());
        VpVerification saved = vpVerificationRepository.save(vpVerification);

        logEventLogger.info("auth.web_vp_login.presentation.submitted", "Web VP login presentation submitted", Map.of(
                "vpVerificationId", saved.getVpVerificationId(),
                "requestId", saved.getVpRequestId(),
                "credentialId", saved.getCredentialId(),
                "corporateId", saved.getCorporateId(),
                "status", saved.getVpVerificationStatus().name()
        ));

        return new WebVpLoginSubmitResponse(
                saved.getVpRequestId(),
                saved.getVpVerificationStatus().name(),
                verified
        );
    }

    // 웹 VP 로그인 상태 조회
    public WebVpLoginStatusResponse status(
            String requestId // VP 로그인 요청 ID
    ) {
        VpVerification vpVerification = getWebVpLoginRequest(requestId);
        LocalDateTime now = LocalDateTime.now();
        if (vpVerification.isExpired(now)
                && KyvcEnums.VpVerificationStatus.VALID != vpVerification.getVpVerificationStatus()) {
            vpVerification.markExpired("웹 VP 로그인 요청 만료", now);
            vpVerification = vpVerificationRepository.save(vpVerification);
        }
        boolean canComplete = KyvcEnums.VpVerificationStatus.VALID == vpVerification.getVpVerificationStatus()
                && vpVerification.getLoginCompletedAt() == null;
        return new WebVpLoginStatusResponse(
                vpVerification.getVpRequestId(),
                vpVerification.getVpVerificationStatus().name(),
                canComplete,
                toKstOffsetString(vpVerification.getExpiresAt())
        );
    }

    // 웹 VP 로그인 완료
    public WebVpLoginCompleteResponse complete(
            String requestId, // VP 로그인 요청 ID
            HttpServletRequest request, // HTTP 요청
            HttpServletResponse response // HTTP 응답
    ) {
        VpVerification vpVerification = getWebVpLoginRequest(requestId);
        String browserSessionToken = tokenCookieUtil.resolveCookie(request, VP_LOGIN_SESSION_COOKIE_NAME);
        requireText(browserSessionToken, ErrorCode.VP_LOGIN_SESSION_INVALID);
        String browserSessionHash = TokenHashUtil.sha256(browserSessionToken.trim());
        if (!vpVerification.matchesBrowserSessionHash(browserSessionHash)) {
            throw new ApiException(ErrorCode.VP_LOGIN_SESSION_INVALID);
        }
        if (KyvcEnums.VpVerificationStatus.VALID != vpVerification.getVpVerificationStatus()) {
            throw new ApiException(ErrorCode.VP_LOGIN_NOT_VERIFIED);
        }
        if (vpVerification.getLoginCompletedAt() != null) {
            throw new ApiException(ErrorCode.VP_LOGIN_ALREADY_COMPLETED);
        }

        Credential credential = credentialRepository.findById(vpVerification.getCredentialId())
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID));
        Corporate corporate = corporateRepository.findById(vpVerification.getCorporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        User user = userRepository.findById(corporate.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_USER_NOT_FOUND));

        AuthService.TokenIssueResult<Void> tokenResult = authService.issueTokensForVerifiedUser(user);
        vpVerification.markLoginCompleted(LocalDateTime.now());
        vpVerificationRepository.save(vpVerification);
        addCookie(response, tokenCookieUtil.createAccessTokenCookie(tokenResult.accessToken()));
        addCookie(response, tokenCookieUtil.createRefreshTokenCookie(tokenResult.refreshToken()));
        addCookie(response, tokenCookieUtil.deleteHttpOnlyCookie(VP_LOGIN_SESSION_COOKIE_NAME));

        logEventLogger.info("auth.web_vp_login.completed", "Web VP login completed", Map.of(
                "userId", user.getUserId(),
                "corporateId", corporate.getCorporateId(),
                "credentialId", credential.getCredentialId(),
                "vpVerificationId", vpVerification.getVpVerificationId()
        ));

        return new WebVpLoginCompleteResponse(
                user.getUserId(),
                corporate.getCorporateId(),
                user.getEmail(),
                user.getUserName()
        );
    }

    // Core challenge 발급 요청
    private CorePresentationChallengeResponse issueChallenge() {
        try {
            return coreAdapter.issuePresentationChallenge(new CorePresentationChallengeRequest(
                    DOMAIN,
                    AUD,
                    DEFINITION_ID,
                    CHALLENGE_FORMAT,
                    createPresentationDefinition()
            ));
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.CORE_VP_CHALLENGE_FAILED, exception);
        }
    }

    // Core Presentation 검증 요청
    private CorePresentationVerifyResponse requestCoreVerify(
            VpVerification vpVerification, // VP 로그인 요청
            Object vp // Wallet 생성 VP 객체
    ) {
        try {
            return coreAdapter.verifyWebVpLoginPresentation(vp);
        } catch (ApiException exception) {
            vpVerification.markFailed(exception.getMessage(), LocalDateTime.now());
            vpVerificationRepository.save(vpVerification);
            throw exception;
        } catch (RuntimeException exception) {
            ApiException apiException = new ApiException(ErrorCode.CORE_VP_VERIFY_FAILED, exception);
            vpVerification.markFailed(apiException.getMessage(), LocalDateTime.now());
            vpVerificationRepository.save(vpVerification);
            throw apiException;
        }
    }

    // Core challenge 응답 검증
    private void validateChallengeResponse(
            CorePresentationChallengeResponse response // Core challenge 응답
    ) {
        if (response == null
                || !StringUtils.hasText(response.challenge())
                || !StringUtils.hasText(response.nonce())
                || !StringUtils.hasText(response.aud())
                || !StringUtils.hasText(response.domain())
                || response.expiresAt() == null
                || response.presentationDefinition() == null) {
            throw new ApiException(ErrorCode.CORE_API_RESPONSE_INVALID);
        }
    }

    // 웹 VP 로그인 요청 검증
    private void validateWebVpLoginRequest(
            VpVerification vpVerification // VP 로그인 요청
    ) {
        if (vpVerification.getRequestTypeCode() != KyvcEnums.VpRequestType.VP_LOGIN
                || !PURPOSE.equals(vpVerification.getPurpose())) {
            throw new ApiException(ErrorCode.VP_LOGIN_REQUEST_NOT_FOUND);
        }
    }

    // 완료 여부 검증
    private void validateNotCompleted(
            VpVerification vpVerification // VP 로그인 요청
    ) {
        if (vpVerification.getLoginCompletedAt() != null) {
            throw new ApiException(ErrorCode.VP_LOGIN_ALREADY_COMPLETED);
        }
    }

    // 만료 여부 검증
    private void validateNotExpired(
            VpVerification vpVerification, // VP 로그인 요청
            LocalDateTime now // 기준 일시
    ) {
        if (vpVerification.isExpired(now)) {
            vpVerification.markExpired("웹 VP 로그인 요청 만료", now);
            vpVerificationRepository.save(vpVerification);
            throw new ApiException(ErrorCode.VP_LOGIN_REQUEST_EXPIRED);
        }
    }

    // Credential 사용 가능 여부 검증
    private Credential getCredentialForVpLogin(
            String requestId, // VP 로그인 요청 ID
            Long credentialId // Credential ID
    ) {
        if (credentialId == null) {
            logInvalidCredential(requestId, null, false, null, null, false, "credentialIdMissing");
            throw new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID);
        }
        return credentialRepository.findById(credentialId)
                .orElseThrow(() -> {
                    logInvalidCredential(requestId, credentialId, false, null, null, false, "credentialNotFound");
                    return new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID);
                });
    }

    // Credential 최소 사용 가능 여부 검증
    private void validateCredential(
            VpVerification vpVerification, // VP 로그인 요청
            Credential credential // Credential
    ) {
        if (credential.getCorporateId() == null) {
            logInvalidCredential(vpVerification.getVpRequestId(), credential, "corporateIdMissing");
            throw new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID);
        }
        if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()) {
            logInvalidCredential(vpVerification.getVpRequestId(), credential, "credentialStatusNotValid");
            throw new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID);
        }
        if (!credential.isWalletSaved()) {
            logInvalidCredential(vpVerification.getVpRequestId(), credential, "walletNotSaved");
            throw new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_NOT_WALLET_SAVED);
        }
    }

    // Credential 검증 실패 로그
    private void logInvalidCredential(
            String requestId, // VP 로그인 요청 ID
            Credential credential, // Credential
            String reason // 실패 사유
    ) {
        logInvalidCredential(
                requestId,
                credential == null ? null : credential.getCredentialId(),
                credential != null,
                credential == null || credential.getCredentialStatus() == null ? null : credential.getCredentialStatus().name(),
                credential == null ? null : credential.getWalletSavedYn(),
                credential != null && credential.getCorporateId() != null,
                reason
        );
    }

    // Credential 검증 실패 로그
    private void logInvalidCredential(
            String requestId, // VP 로그인 요청 ID
            Long credentialId, // Credential ID
            boolean credentialExists, // Credential 존재 여부
            String credentialStatusCode, // Credential 상태 코드
            String walletSavedYn, // Wallet 저장 여부
            boolean corporateIdExists, // 법인 ID 존재 여부
            String reason // 실패 사유
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("requestId", requestId);
        fields.put("credentialId", credentialId);
        fields.put("credentialExists", credentialExists);
        fields.put("credentialStatusCode", credentialStatusCode);
        fields.put("walletSavedYn", walletSavedYn);
        fields.put("corporateIdExists", corporateIdExists);
        fields.put("reason", reason);
        logEventLogger.warn("auth.web_vp_login.credential.invalid", "Web VP login credential invalid", fields);
    }

    // VP 객체 필수값 검증
    private void requirePresentation(
            Object vp // Wallet 생성 VP 객체
    ) {
        if (vp == null || (vp instanceof String vpText && !StringUtils.hasText(vpText))) {
            throw new ApiException(ErrorCode.VP_LOGIN_PRESENTATION_REQUIRED);
        }
    }

    // 문자열 필수값 검증
    private void requireText(
            String value, // 검증 대상 문자열
            ErrorCode errorCode // 실패 오류 코드
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(errorCode);
        }
    }

    // 웹 VP 로그인 요청 조회
    private VpVerification getWebVpLoginRequest(
            String requestId // VP 로그인 요청 ID
    ) {
        requireText(requestId, ErrorCode.VP_LOGIN_REQUEST_NOT_FOUND);
        VpVerification vpVerification = vpVerificationRepository.findByRequestId(requestId.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_REQUEST_NOT_FOUND));
        validateWebVpLoginRequest(vpVerification);
        return vpVerification;
    }

    // Presentation Definition 생성
    private Map<String, Object> createPresentationDefinition() {
        Map<String, Object> presentationDefinition = new LinkedHashMap<>();
        presentationDefinition.put("id", DEFINITION_ID);
        presentationDefinition.put("acceptedFormat", CHALLENGE_FORMAT);
        presentationDefinition.put("acceptedVct", List.of(ACCEPTED_VCT));
        presentationDefinition.put("acceptedJurisdictions", List.of(ACCEPTED_JURISDICTION));
        presentationDefinition.put("minimumAssuranceLevel", MINIMUM_ASSURANCE_LEVEL);
        presentationDefinition.put("requiredDisclosures", REQUIRED_DISCLOSURES);
        presentationDefinition.put("documentRules", List.of());
        return presentationDefinition;
    }

    // Core challenge 메타데이터 JSON 생성
    private String createMetadataJson(
            CorePresentationChallengeResponse response // Core challenge 응답
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("aud", response.aud());
        metadata.put("domain", response.domain());
        metadata.put("presentationDefinition", response.presentationDefinition());
        return toJson(metadata);
    }

    // 메타데이터 JSON 조회
    private Map<String, Object> readMetadata(
            VpVerification vpVerification // VP 로그인 요청
    ) {
        if (!StringUtils.hasText(vpVerification.getPermissionResultJson())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(vpVerification.getPermissionResultJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    // Presentation Definition 조회
    @SuppressWarnings("unchecked")
    private Map<String, Object> resolvePresentationDefinition(
            Map<String, Object> metadata // 저장 메타데이터
    ) {
        Object value = metadata.get("presentationDefinition");
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        return createPresentationDefinition();
    }

    // 문자열 메타데이터 조회
    private String resolveText(
            Object value, // 메타데이터 값
            String fallback // 기본값
    ) {
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text.trim();
        }
        return fallback;
    }

    // Core 검증 결과 요약
    private String resolveCoreVerifySummary(
            CorePresentationVerifyResponse response, // Core 검증 응답
            String fallback // 기본 요약
    ) {
        if (response != null && StringUtils.hasText(response.message())) {
            return response.message().trim();
        }
        if (response != null && response.errors() != null && !response.errors().isEmpty()) {
            String firstError = response.errors().get(0);
            if (StringUtils.hasText(firstError)) {
                return firstError.trim();
            }
        }
        return fallback;
    }

    // 세션 Cookie 유지 시간 계산
    private Duration resolveSessionMaxAge(
            LocalDateTime now, // 기준 일시
            LocalDateTime expiresAt // 만료 일시
    ) {
        Duration duration = Duration.between(now, expiresAt);
        if (duration.isNegative() || duration.isZero()) {
            throw new ApiException(ErrorCode.VP_LOGIN_REQUEST_EXPIRED);
        }
        return duration;
    }

    // 불투명 토큰 생성
    // KST offset 만료 일시 문자열 변환
    private String toKstOffsetString(
            LocalDateTime value // 만료 일시
    ) {
        if (value == null) {
            return null;
        }
        return value.truncatedTo(ChronoUnit.SECONDS)
                .atZone(KST_ZONE)
                .toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String createOpaqueToken() {
        byte[] bytes = new byte[32]; // 난수 바이트
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // 요청 ID 생성
    private String createRequestId() {
        return "vp-login-req-" + UUID.randomUUID();
    }

    // JSON 직렬화
    private String toJson(
            Object value // JSON 직렬화 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, exception);
        }
    }

    // Set-Cookie 헤더 추가
    private void addCookie(
            HttpServletResponse response, // HTTP 응답
            ResponseCookie cookie // 추가 Cookie
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
