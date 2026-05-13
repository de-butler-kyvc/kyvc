package com.kyvc.backend.domain.mobile.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.auth.application.AuthService;
import com.kyvc.backend.domain.core.application.CoreRequestService;
import com.kyvc.backend.domain.core.domain.CoreRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeRequest;
import com.kyvc.backend.domain.core.dto.CorePresentationChallengeResponse;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationRequest;
import com.kyvc.backend.domain.core.dto.CoreVpVerificationResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.mobile.domain.MobileDeviceBinding;
import com.kyvc.backend.domain.mobile.dto.MobileDeviceRegisterRequest;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginChallengeRequest;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginChallengeResponse;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginRequest;
import com.kyvc.backend.domain.mobile.dto.MobileVpLoginResponse;
import com.kyvc.backend.domain.mobile.repository.MobileDeviceBindingRepository;
import com.kyvc.backend.domain.user.domain.User;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.domain.vp.domain.VpVerification;
import com.kyvc.backend.domain.vp.repository.VpVerificationRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.JwtTokenProvider;
import com.kyvc.backend.global.jwt.TokenCookieUtil;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// 모바일 VP 로그인 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class MobileVpLoginService {

    private static final String DOMAIN = "kyvc-mobile";
    private static final String AUD = "kyvc-mobile-login";
    private static final String DEFINITION_ID = "kyvc-mobile-vp-login-v1";
    private static final String PRESENTATION_FORMAT = "vp+jwt";
    private static final String CHALLENGE_FORMAT = "dc+sd-jwt";
    private static final String PURPOSE = "MOBILE_VP_LOGIN";
    private static final List<String> REQUIRED_CLAIMS = List.of("businessRegistrationNo", "corporateName");

    private final VpVerificationRepository vpVerificationRepository;
    private final CredentialRepository credentialRepository;
    private final CorporateRepository corporateRepository;
    private final UserRepository userRepository;
    private final MobileDeviceBindingRepository mobileDeviceBindingRepository;
    private final CoreRequestService coreRequestService;
    private final CoreAdapter coreAdapter;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenCookieUtil tokenCookieUtil;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // VP 로그인 challenge 생성
    public MobileVpLoginChallengeResponse createChallenge(
            MobileVpLoginChallengeRequest request // VP 로그인 challenge 요청
    ) {
        validateChallengeRequest(request);
        Credential placeholderCredential = credentialRepository.findLatestValid()
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID));

        CorePresentationChallengeResponse coreResponse = issueChallenge();
        validateChallengeResponse(coreResponse);
        String requestId = createRequestId();
        String requiredClaimsJson = toJson(REQUIRED_CLAIMS);
        String metadataJson = toJson(Map.of(
                "domain", coreResponse.domain(),
                "aud", coreResponse.aud(),
                "deviceId", request.deviceId(),
                "deviceName", normalizeOptional(request.deviceName()),
                "os", normalizeOptional(request.os()),
                "appVersion", normalizeOptional(request.appVersion())
        ));

        VpVerification vpVerification = VpVerification.createRequest(
                null,
                placeholderCredential.getCorporateId(),
                requestId,
                coreResponse.nonce(),
                coreResponse.challenge(),
                PURPOSE,
                DOMAIN,
                requiredClaimsJson,
                coreResponse.expiresAt(),
                null,
                null,
                KyvcEnums.VpRequestType.VP_LOGIN,
                KyvcEnums.Yn.N,
                KyvcEnums.Yn.N,
                metadataJson
        );
        VpVerification saved = vpVerificationRepository.save(vpVerification);

        logEventLogger.info("mobile.vp_login.challenge.created", "Mobile VP login challenge created", Map.of(
                "vpVerificationId", saved.getVpVerificationId(),
                "requestId", saved.getVpRequestId()
        ));

        return new MobileVpLoginChallengeResponse(
                saved.getVpRequestId(),
                saved.getChallenge(),
                saved.getRequestNonce(),
                coreResponse.domain(),
                coreResponse.aud(),
                saved.getExpiresAt(),
                REQUIRED_CLAIMS
        );
    }

    // VP 로그인 처리
    public MobileVpLoginResponse login(
            MobileVpLoginRequest request, // VP 로그인 요청
            HttpServletResponse response // HTTP 응답
    ) {
        validateLoginRequest(request);
        LocalDateTime now = LocalDateTime.now();
        VpVerification vpVerification = getVpLoginRequest(request.requestId());
        validateVpLoginRequest(vpVerification, request, now);

        Credential credential = credentialRepository.findById(request.credentialId())
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID));
        validateCredential(credential, now);
        Corporate corporate = corporateRepository.findById(credential.getCorporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        User user = userRepository.findById(corporate.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }

        String vpJwt = request.vpJwt().trim();
        String vpJwtHash = TokenHashUtil.sha256(vpJwt);
        if (vpVerificationRepository.existsReplayCandidate(vpVerification.getRequestNonce(), vpJwtHash)) {
            vpVerification.markReplaySuspected("VP 로그인 재제출 의심", now);
            vpVerificationRepository.save(vpVerification);
            throw new ApiException(ErrorCode.VP_PRESENTATION_REPLAY_SUSPECTED);
        }

        CoreRequest coreRequest = coreRequestService.createVpVerificationRequest(vpVerification.getVpVerificationId(), null);
        vpVerification.markPresentedForCorporate(corporate.getCorporateId(), credential.getCredentialId(), vpJwtHash, coreRequest.getCoreRequestId(), now);
        CoreVpVerificationRequest coreRequestDto = buildCoreVpVerificationRequest(vpVerification, credential, coreRequest.getCoreRequestId(), now);
        coreRequestService.updateRequestPayloadJson(coreRequest.getCoreRequestId(), toJson(coreRequestDto));
        coreRequestService.markRunning(coreRequest.getCoreRequestId());

        CoreVpVerificationResponse coreResponse = requestCoreVerification(vpVerification, coreRequest, coreRequestDto, resolveFormat(request), vpJwt);
        applyCoreVerificationResult(vpVerification, coreRequest.getCoreRequestId(), coreResponse);
        VpVerification saved = vpVerificationRepository.save(vpVerification);

        AuthService.TokenIssueResult<Void> tokenResult = authService.issueTokensForVerifiedUser(user);
        addAuthCookies(response, tokenResult.accessToken(), tokenResult.refreshToken());
        upsertDeviceBinding(user.getUserId(), request.deviceId(), now);

        logEventLogger.info("mobile.vp_login.success", "Mobile VP login success", Map.of(
                "userId", user.getUserId(),
                "corporateId", corporate.getCorporateId(),
                "credentialId", credential.getCredentialId(),
                "vpVerificationId", saved.getVpVerificationId()
        ));

        return new MobileVpLoginResponse(
                true,
                user.getUserId(),
                corporate.getCorporateId(),
                user.getEmail(),
                user.getUserTypeCode().name(),
                authService.resolveUserRoles(user),
                credential.getCredentialId(),
                saved.getVpVerificationId(),
                toLocalDateTime(jwtTokenProvider.getExpiration(tokenResult.accessToken()))
        );
    }

    // Core challenge 요청
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

    // Core VP 검증 요청
    private CoreVpVerificationResponse requestCoreVerification(
            VpVerification vpVerification, // VP 로그인 검증 요청
            CoreRequest coreRequest, // Core 요청
            CoreVpVerificationRequest coreRequestDto, // Core 요청 DTO
            String format, // VP format
            String vpJwt // VP JWT 원문
    ) {
        try {
            CoreVpVerificationResponse coreResponse = coreAdapter.requestVpVerification(coreRequestDto, format, vpJwt);
            logEventLogger.info("mobile.vp_login.core.completed", "Mobile VP login core verification completed", Map.of(
                    "vpVerificationId", vpVerification.getVpVerificationId(),
                    "coreRequestId", coreRequest.getCoreRequestId()
            ));
            return coreResponse;
        } catch (ApiException exception) {
            markCoreFailure(coreRequest.getCoreRequestId(), exception);
            vpVerification.markFailed(exception.getMessage(), LocalDateTime.now());
            vpVerificationRepository.save(vpVerification);
            throw exception;
        } catch (RuntimeException exception) {
            ApiException apiException = new ApiException(ErrorCode.CORE_VP_VERIFY_FAILED, exception);
            markCoreFailure(coreRequest.getCoreRequestId(), apiException);
            vpVerification.markFailed(apiException.getMessage(), LocalDateTime.now());
            vpVerificationRepository.save(vpVerification);
            throw apiException;
        }
    }

    // Core 검증 결과 반영
    private void applyCoreVerificationResult(
            VpVerification vpVerification, // VP 로그인 검증 요청
            String coreRequestId, // Core 요청 ID
            CoreVpVerificationResponse coreResponse // Core 검증 응답
    ) {
        if (coreResponse == null || !Boolean.TRUE.equals(coreResponse.completed())) {
            vpVerification.markInvalid("VP 로그인 Core 검증 결과 확인 실패", LocalDateTime.now());
            coreRequestService.markFailed(coreRequestId, "VP 로그인 Core 검증 결과 확인 실패");
            throw new ApiException(ErrorCode.VP_LOGIN_CORE_VERIFY_FAILED);
        }
        if (Boolean.TRUE.equals(coreResponse.replaySuspected())) {
            vpVerification.markReplaySuspected(resolveResultSummary(coreResponse), LocalDateTime.now());
            coreRequestService.markFailed(coreRequestId, "VP 로그인 replay 의심");
            throw new ApiException(ErrorCode.VP_PRESENTATION_REPLAY_SUSPECTED);
        }
        if (!Boolean.TRUE.equals(coreResponse.valid())) {
            vpVerification.markInvalid(resolveResultSummary(coreResponse), LocalDateTime.now());
            coreRequestService.markFailed(coreRequestId, "VP 로그인 Core 검증 실패");
            throw new ApiException(ErrorCode.VP_LOGIN_CORE_VERIFY_FAILED);
        }
        vpVerification.markValid(resolveResultSummary(coreResponse), LocalDateTime.now());
        coreRequestService.markSuccess(coreRequestId, toJson(Map.of(
                "completed", true,
                "valid", true,
                "status", normalizeOptional(coreResponse.status())
        )));
    }

    // Core 요청 실패 반영
    private void markCoreFailure(
            String coreRequestId, // Core 요청 ID
            ApiException exception // 실패 예외
    ) {
        if (exception.getErrorCode() == ErrorCode.CORE_API_TIMEOUT) {
            coreRequestService.markTimeout(coreRequestId, exception.getMessage());
            return;
        }
        coreRequestService.markFailed(coreRequestId, exception.getMessage());
    }

    // VP 로그인 요청 조회
    private VpVerification getVpLoginRequest(
            String requestId // VP 로그인 요청 ID
    ) {
        VpVerification vpVerification = vpVerificationRepository.findByRequestId(requestId.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.VP_LOGIN_REQUEST_NOT_FOUND));
        if (vpVerification.getRequestTypeCode() != KyvcEnums.VpRequestType.VP_LOGIN) {
            throw new ApiException(ErrorCode.VP_LOGIN_REQUEST_NOT_FOUND);
        }
        return vpVerification;
    }

    // VP 로그인 요청 검증
    private void validateVpLoginRequest(
            VpVerification vpVerification, // VP 로그인 검증 요청
            MobileVpLoginRequest request, // VP 로그인 요청
            LocalDateTime now // 기준 일시
    ) {
        if (!vpVerification.isRequested()) {
            throw new ApiException(ErrorCode.VP_LOGIN_REQUEST_ALREADY_USED);
        }
        if (vpVerification.isExpired(now)) {
            vpVerification.markExpired("VP 로그인 요청 만료", now);
            vpVerificationRepository.save(vpVerification);
            throw new ApiException(ErrorCode.VP_LOGIN_REQUEST_EXPIRED);
        }
        if (!vpVerification.matchesNonce(request.nonce().trim())) {
            throw new ApiException(ErrorCode.VP_LOGIN_NONCE_MISMATCH);
        }
        if (!vpVerification.matchesChallenge(request.challenge().trim())) {
            throw new ApiException(ErrorCode.VP_LOGIN_CHALLENGE_MISMATCH);
        }
    }

    // Credential 검증
    private void validateCredential(
            Credential credential, // Credential
            LocalDateTime now // 기준 일시
    ) {
        if (credential == null || !credential.isValid(now)) {
            throw new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_INVALID);
        }
        if (!credential.isWalletSaved()) {
            throw new ApiException(ErrorCode.VP_LOGIN_CREDENTIAL_NOT_WALLET_SAVED);
        }
    }

    // challenge 요청 검증
    private void validateChallengeRequest(
            MobileVpLoginChallengeRequest request // VP 로그인 challenge 요청
    ) {
        if (request == null || !StringUtils.hasText(request.deviceId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 로그인 요청 검증
    private void validateLoginRequest(
            MobileVpLoginRequest request // VP 로그인 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.requestId())
                || request.credentialId() == null
                || !StringUtils.hasText(request.vpJwt())
                || !StringUtils.hasText(request.nonce())
                || !StringUtils.hasText(request.challenge())
                || !StringUtils.hasText(request.deviceId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // Core challenge 응답 검증
    private void validateChallengeResponse(
            CorePresentationChallengeResponse response // Core challenge 응답
    ) {
        if (response == null
                || !StringUtils.hasText(response.challenge())
                || !StringUtils.hasText(response.nonce())
                || !StringUtils.hasText(response.domain())
                || !StringUtils.hasText(response.aud())
                || response.expiresAt() == null) {
            throw new ApiException(ErrorCode.CORE_API_RESPONSE_INVALID);
        }
    }

    // Core VP 검증 요청 생성
    private CoreVpVerificationRequest buildCoreVpVerificationRequest(
            VpVerification vpVerification, // VP 로그인 검증 요청
            Credential credential, // Credential
            String coreRequestId, // Core 요청 ID
            LocalDateTime requestedAt // 요청 일시
    ) {
        return new CoreVpVerificationRequest(
                coreRequestId,
                vpVerification.getVpVerificationId(),
                credential.getCredentialId(),
                credential.getCorporateId(),
                vpVerification.getRequestNonce(),
                vpVerification.getChallenge(),
                vpVerification.getPurpose(),
                resolveAud(vpVerification),
                vpVerification.getRequiredClaimsJson(),
                requestedAt
        );
    }

    // VP aud 산정
    private String resolveAud(
            VpVerification vpVerification // VP 로그인 검증 요청
    ) {
        if (!StringUtils.hasText(vpVerification.getPermissionResultJson())) {
            return AUD;
        }
        try {
            String aud = objectMapper.readTree(vpVerification.getPermissionResultJson()).path("aud").asText();
            return StringUtils.hasText(aud) ? aud.trim() : AUD;
        } catch (JsonProcessingException exception) {
            return AUD;
        }
    }

    // 기기 바인딩 등록 또는 갱신
    private void upsertDeviceBinding(
            Long userId, // 사용자 ID
            String deviceId, // 모바일 기기 ID
            LocalDateTime now // 기준 일시
    ) {
        MobileDeviceRegisterRequest deviceRequest = new MobileDeviceRegisterRequest(
                deviceId,
                null,
                null,
                null,
                null
        );
        MobileDeviceBinding deviceBinding = mobileDeviceBindingRepository
                .findByUserIdAndDeviceId(userId, deviceId)
                .orElse(null);
        if (deviceBinding == null) {
            mobileDeviceBindingRepository.save(MobileDeviceBinding.create(userId, deviceRequest, now));
            return;
        }
        if (deviceBinding.isActive()) {
            deviceBinding.updateLastUsedAt(now);
        } else {
            deviceBinding.updateDeviceInfo(deviceRequest, now);
        }
        mobileDeviceBindingRepository.save(deviceBinding);
    }

    // 인증 Cookie 추가
    private void addAuthCookies(
            HttpServletResponse response, // HTTP 응답
            String accessToken, // Access Token 원문
            String refreshToken // Refresh Token 원문
    ) {
        addCookie(response, tokenCookieUtil.createAccessTokenCookie(accessToken));
        addCookie(response, tokenCookieUtil.createRefreshTokenCookie(refreshToken));
    }

    // Cookie 헤더 추가
    private void addCookie(
            HttpServletResponse response, // HTTP 응답
            ResponseCookie cookie // 추가 Cookie
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // Presentation definition 생성
    private Map<String, Object> createPresentationDefinition() {
        return Map.of(
                "id", DEFINITION_ID,
                "name", "KYvC Mobile VP Login",
                "requiredClaims", REQUIRED_CLAIMS
        );
    }

    // 요청 ID 생성
    private String createRequestId() {
        return "vp-login-req-" + UUID.randomUUID();
    }

    // VP format 산정
    private String resolveFormat(
            MobileVpLoginRequest request // VP 로그인 요청
    ) {
        if (!StringUtils.hasText(request.format())) {
            return PRESENTATION_FORMAT;
        }
        return request.format().trim().toLowerCase(Locale.ROOT);
    }

    // 결과 요약 산정
    private String resolveResultSummary(
            CoreVpVerificationResponse response // Core 검증 응답
    ) {
        if (response != null && StringUtils.hasText(response.resultSummary())) {
            return response.resultSummary().trim();
        }
        if (response != null && StringUtils.hasText(response.message())) {
            return response.message().trim();
        }
        return "VP 로그인 검증 완료";
    }

    // 선택 문자열 정규화
    private String normalizeOptional(
            String value // 원본 문자열
    ) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    // JSON 변환
    private String toJson(
            Object value // JSON 변환 대상
    ) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, exception);
        }
    }

    // Instant LocalDateTime 변환
    private LocalDateTime toLocalDateTime(
            Instant instant // 변환 대상 시각
    ) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
