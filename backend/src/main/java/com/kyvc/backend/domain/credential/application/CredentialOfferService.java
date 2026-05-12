package com.kyvc.backend.domain.credential.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.corporate.domain.Corporate;
import com.kyvc.backend.domain.corporate.repository.CorporateRepository;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.credential.domain.CredentialOffer;
import com.kyvc.backend.domain.credential.dto.CredentialOfferCreateResponse;
import com.kyvc.backend.domain.credential.dto.CredentialOfferStatusResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialConfirmRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialConfirmResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialOfferResponse;
import com.kyvc.backend.domain.credential.dto.WalletCredentialPrepareRequest;
import com.kyvc.backend.domain.credential.dto.WalletCredentialPrepareResponse;
import com.kyvc.backend.domain.credential.repository.CredentialOfferRepository;
import com.kyvc.backend.domain.credential.repository.CredentialRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.domain.kyc.repository.KycApplicationRepository;
import com.kyvc.backend.domain.mobile.application.MobileDeviceService;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.logging.LogEventLogger;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// Credential Offer 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class CredentialOfferService {

    private static final Duration OFFER_TTL = Duration.ofMinutes(10);
    private static final String QR_TYPE = KyvcEnums.QrType.CREDENTIAL_OFFER.name();
    private static final String ACCEPT_DEPRECATED_MESSAGE = "Credential Offer 저장은 prepare/confirm API를 사용해야 합니다.";

    private final CredentialOfferRepository credentialOfferRepository;
    private final CredentialRepository credentialRepository;
    private final KycApplicationRepository kycApplicationRepository;
    private final CorporateRepository corporateRepository;
    private final MobileDeviceService mobileDeviceService;
    private final CredentialClaimsAssembler credentialClaimsAssembler;
    private final CredentialIssuerResolver credentialIssuerResolver;
    private final CredentialIssuanceService credentialIssuanceService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    // PC Credential Offer 생성
    public CredentialOfferCreateResponse createOffer(
            Long userId, // 사용자 ID
            Long kycId // KYC 신청 ID
    ) {
        validateUserId(userId);
        validateId(kycId);

        Corporate corporate = getCorporateByUserId(userId);
        KycApplication kycApplication = kycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        validateKycOwnership(userId, corporate.getCorporateId(), kycApplication);
        if (!kycApplication.isCredentialIssuable()) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_CREATE_NOT_ALLOWED);
        }
        if (credentialRepository.existsWalletSavedValidByKycId(kycId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_ISSUANCE_ALREADY_COMPLETED);
        }

        LocalDateTime now = LocalDateTime.now(); // 기준 일시
        closeActiveOffersForNewQr(kycId, now);

        String rawQrToken = UUID.randomUUID().toString(); // QR 토큰 원문
        LocalDateTime expiresAt = now.plus(OFFER_TTL); // 만료 일시
        CredentialOffer offer = credentialOfferRepository.save(CredentialOffer.create(
                kycId,
                kycApplication.getCorporateId(),
                TokenHashUtil.sha256(rawQrToken),
                expiresAt
        ));

        logEventLogger.info(
                "credential.offer.created",
                "Credential offer created",
                createOfferLogFields(userId, kycApplication.getCorporateId(), offer.getCredentialOfferId(), kycId, null)
        );
        saveAuditLog(userId, "CREDENTIAL_OFFER_CREATED", offer.getCredentialOfferId(), "Credential Offer 생성");

        return new CredentialOfferCreateResponse(
                offer.getCredentialOfferId(),
                offer.getKycId(),
                createQrPayload(offer, rawQrToken),
                offer.getExpiresAt(),
                enumName(offer.getOfferStatus())
        );
    }

    // PC Credential Offer 상태 조회
    public CredentialOfferStatusResponse getOfferStatus(
            Long userId, // 사용자 ID
            Long offerId // Offer ID
    ) {
        validateUserId(userId);
        validateId(offerId);

        CredentialOffer offer = credentialOfferRepository.getById(offerId);
        KycApplication kycApplication = kycApplicationRepository.findById(offer.getKycId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        Corporate corporate = getCorporateByUserId(userId);
        validateKycOwnership(userId, corporate.getCorporateId(), kycApplication);
        expireOfferIfRequired(offer, LocalDateTime.now());

        Credential credential = offer.getCredentialId() == null
                ? null
                : credentialRepository.findById(offer.getCredentialId()).orElse(null);
        return toStatusResponse(offer, credential);
    }

    // 모바일 Credential Offer 상세 조회
    @Transactional(readOnly = true)
    public WalletCredentialOfferResponse getMobileOffer(
            Long userId, // 사용자 ID
            Long offerId // Offer ID
    ) {
        validateUserId(userId);
        validateId(offerId);

        Corporate corporate = getCorporateByUserId(userId);
        CredentialOffer offer = credentialOfferRepository.getById(offerId);
        validateOfferCorporate(corporate.getCorporateId(), offer);
        validateOfferActiveForRead(offer, LocalDateTime.now());

        KycApplication kycApplication = kycApplicationRepository.findById(offer.getKycId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        Corporate offerCorporate = corporateRepository.findById(offer.getCorporateId())
                .orElseThrow(() -> new ApiException(ErrorCode.CORPORATE_NOT_FOUND));
        Credential credential = offer.getCredentialId() == null
                ? null
                : credentialRepository.findById(offer.getCredentialId()).orElse(null);
        ResolvedIssuer issuer = credential == null ? credentialIssuerResolver.resolveKycIssuer() : null;

        return new WalletCredentialOfferResponse(
                offer.getCredentialOfferId(),
                offer.getKycId(),
                offer.getCredentialId(),
                credential == null ? issuer.credentialType() : credential.getCredentialTypeCode(),
                credential == null ? issuer.issuerDid() : credential.getIssuerDid(),
                offerCorporate.getCorporateName(),
                offerCorporate.getBusinessRegistrationNo(),
                offer.getExpiresAt(),
                credential != null && credential.isWalletSaved()
        );
    }

    // 모바일 Wallet Credential 준비
    public WalletCredentialPrepareResponse prepareWalletCredential(
            Long userId, // 사용자 ID
            Long offerId, // Offer ID
            WalletCredentialPrepareRequest request // 준비 요청
    ) {
        validateUserId(userId);
        validateId(offerId);
        validatePrepareRequest(request);

        Corporate corporate = getCorporateByUserId(userId);
        CredentialOffer offer = credentialOfferRepository.getById(offerId);
        validateOfferCorporate(corporate.getCorporateId(), offer);
        validateOfferActiveForPrepare(offer, LocalDateTime.now());
        validateQrToken(offer, request.qrToken());
        mobileDeviceService.getActiveDeviceBinding(userId, normalizeRequiredText(request.deviceId()));

        if (offer.isPrepared()) {
            Credential credential = credentialRepository.getById(offer.getCredentialId());
            validatePreparedRequestMatchesOffer(offer, request);
            validatePreparedCredential(credential);
            return new WalletCredentialPrepareResponse(
                    offer.getCredentialOfferId(),
                    credential.getCredentialId(),
                    true,
                    createCredentialPayload(credential, resolveIssuerAccount(credential))
            );
        }

        KycApplication kycApplication = kycApplicationRepository.findById(offer.getKycId())
                .orElseThrow(() -> new ApiException(ErrorCode.KYC_NOT_FOUND));
        if (!kycApplication.isCredentialIssuable()) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_ACTIVE);
        }

        ResolvedIssuer issuer = credentialIssuerResolver.resolveKycIssuer();
        Map<String, Object> claims = credentialClaimsAssembler.assemble(kycApplication);
        Credential credential = issueCredentialForOffer(userId, offer, request, kycApplication, claims, issuer);
        validatePreparedCredential(credential);

        offer.bindPreparedCredential(
                credential.getCredentialId(),
                normalizeRequiredText(request.deviceId()),
                normalizeRequiredText(request.holderDid()),
                normalizeRequiredText(request.holderXrplAddress())
        );
        credentialOfferRepository.save(offer);

        logEventLogger.info(
                "credential.offer.prepared",
                "Credential offer prepared",
                createOfferLogFields(userId, offer.getCorporateId(), offer.getCredentialOfferId(), offer.getKycId(), credential.getCredentialId())
        );
        saveAuditLog(userId, "CREDENTIAL_OFFER_PREPARED", offer.getCredentialOfferId(), "Credential Offer 준비");

        return new WalletCredentialPrepareResponse(
                offer.getCredentialOfferId(),
                credential.getCredentialId(),
                true,
                createCredentialPayload(credential, issuer.issuerAccount())
        );
    }

    // 모바일 Wallet Credential 저장 확정
    public WalletCredentialConfirmResponse confirmWalletCredential(
            Long userId, // 사용자 ID
            Long offerId, // Offer ID
            WalletCredentialConfirmRequest request // 저장 확정 요청
    ) {
        validateUserId(userId);
        validateId(offerId);
        validateConfirmRequest(request);

        Corporate corporate = getCorporateByUserId(userId);
        CredentialOffer offer = credentialOfferRepository.getById(offerId);
        validateOfferCorporate(corporate.getCorporateId(), offer);
        validateConfirmMatchesOffer(offer, request);
        mobileDeviceService.getActiveDeviceBinding(userId, normalizeRequiredText(request.deviceId()));

        Credential credential = credentialRepository.getById(request.credentialId());
        validateCredentialOwnership(offer, credential);
        if (offer.isUsed()) {
            return toConfirmResponse(offer, credential);
        }
        if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.CREDENTIAL_NOT_VALID);
        }

        if (!credential.isWalletSaved()) {
            credential.acceptToWallet(
                    normalizeRequiredText(request.deviceId()),
                    offer.getHolderDid(),
                    offer.getHolderXrplAddress(),
                    request.walletSavedAt() == null ? LocalDateTime.now() : request.walletSavedAt()
            );
            credentialRepository.save(credential);
        }
        offer.markUsed();
        credentialOfferRepository.save(offer);

        logEventLogger.info(
                "credential.offer.confirmed",
                "Credential offer confirmed",
                createConfirmLogFields(userId, offer, credential, StringUtils.hasText(request.credentialAcceptHash()))
        );
        saveAuditLog(userId, "CREDENTIAL_OFFER_CONFIRMED", offer.getCredentialOfferId(), "Credential Offer 확정");

        return toConfirmResponse(offer, credential);
    }

    // 신규 Credential Offer accept 호출 차단
    public void rejectDeprecatedAcceptIfCredentialOfferExists(
            Long offerId // Offer ID
    ) {
        if (offerId == null || offerId <= 0) {
            return;
        }
        credentialOfferRepository.findById(offerId)
                .ifPresent(offer -> {
                    throw new ApiException(ErrorCode.DEPRECATED_API, ACCEPT_DEPRECATED_MESSAGE);
                });
    }

    private Credential issueCredentialForOffer(
            Long userId, // 사용자 ID
            CredentialOffer offer, // Credential Offer
            WalletCredentialPrepareRequest request, // 준비 요청
            KycApplication kycApplication, // KYC 신청
            Map<String, Object> claims, // VC claims
            ResolvedIssuer issuer // 발급 Issuer
    ) {
        try {
            Credential credential = credentialIssuanceService.issueKycCredentialForHolder(
                    kycApplication,
                    userId,
                    normalizeRequiredText(request.holderDid()),
                    normalizeRequiredText(request.holderXrplAddress()),
                    claims,
                    issuer
            );
            if (KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()) {
                offer.markFailed(ErrorCode.WALLET_CREDENTIAL_PREPARE_FAILED.getCode());
                credentialOfferRepository.save(offer);
                throw new ApiException(ErrorCode.WALLET_CREDENTIAL_PREPARE_FAILED);
            }
            return credential;
        } catch (ApiException exception) {
            offer.markFailed(exception.getErrorCode().getCode());
            credentialOfferRepository.save(offer);
            logEventLogger.warn(
                    "credential.offer.prepare.failed",
                    exception.getMessage(),
                    createOfferLogFields(userId, offer.getCorporateId(), offer.getCredentialOfferId(), offer.getKycId(), offer.getCredentialId())
            );
            throw exception;
        }
    }

    private void closeActiveOffersForNewQr(
            Long kycId, // KYC 신청 ID
            LocalDateTime now // 기준 일시
    ) {
        credentialOfferRepository.findActiveOffersByKycId(kycId)
                .forEach(offer -> {
                    if (offer.isExpired(now)) {
                        offer.markExpired();
                    } else {
                        offer.cancel();
                    }
                    credentialOfferRepository.save(offer);
                });
    }

    private void expireOfferIfRequired(
            CredentialOffer offer, // Credential Offer
            LocalDateTime now // 기준 일시
    ) {
        if (KyvcEnums.CredentialOfferStatus.ACTIVE == offer.getOfferStatus() && offer.isExpired(now)) {
            offer.markExpired();
            credentialOfferRepository.save(offer);
        }
    }

    private CredentialOfferStatusResponse toStatusResponse(
            CredentialOffer offer, // Credential Offer
            Credential credential // Credential
    ) {
        return new CredentialOfferStatusResponse(
                offer.getCredentialOfferId(),
                offer.getKycId(),
                enumName(offer.getOfferStatus()),
                offer.getCredentialId(),
                credential == null ? null : enumName(credential.getCredentialStatus()),
                credential != null && credential.isWalletSaved(),
                offer.getUsedAt(),
                offer.getExpiresAt()
        );
    }

    private WalletCredentialConfirmResponse toConfirmResponse(
            CredentialOffer offer, // Credential Offer
            Credential credential // Credential
    ) {
        return new WalletCredentialConfirmResponse(
                offer.getCredentialOfferId(),
                credential.getCredentialId(),
                credential.isWalletSaved(),
                enumName(offer.getOfferStatus()),
                enumName(credential.getCredentialStatus()),
                credential.getWalletSavedAt()
        );
    }

    private Map<String, Object> createQrPayload(
            CredentialOffer offer, // Credential Offer
            String rawQrToken // QR 토큰 원문
    ) {
        Map<String, Object> qrPayload = new LinkedHashMap<>();
        qrPayload.put("type", QR_TYPE);
        qrPayload.put("offerId", offer.getCredentialOfferId());
        qrPayload.put("qrToken", rawQrToken);
        qrPayload.put("expiresAt", offer.getExpiresAt());
        return qrPayload;
    }

    private Map<String, Object> createCredentialPayload(
            Credential credential, // Credential
            String issuerAccount // Issuer XRPL Account
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("format", credential.getVcFormat());
        payload.put("credentialJwt", credential.getVcJwt());
        payload.put("credential", parseCredentialPayloadJson(credential.getVcPayloadJson()));
        payload.put("metadata", createCredentialMetadata(credential, issuerAccount));
        return payload;
    }

    private Map<String, Object> createCredentialMetadata(
            Credential credential, // Credential
            String issuerAccount // Issuer XRPL Account
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("credentialId", credential.getCredentialId());
        metadata.put("credentialType", credential.getCredentialTypeCode());
        metadata.put("issuerDid", credential.getIssuerDid());
        metadata.put("issuerAccount", issuerAccount);
        metadata.put("holderDid", credential.getHolderDid());
        metadata.put("holderXrplAddress", credential.getHolderXrplAddress());
        metadata.put("vcHash", credential.getVcHash());
        metadata.put("xrplTxHash", credential.getXrplTxHash());
        metadata.put("credentialStatusId", credential.getCredentialStatusId());
        metadata.put("issuedAt", credential.getIssuedAt());
        metadata.put("expiresAt", credential.getExpiresAt());
        metadata.put("format", credential.getVcFormat());
        return metadata;
    }

    private Map<String, Object> parseCredentialPayloadJson(
            String vcPayloadJson // VC JSON 원문
    ) {
        if (!StringUtils.hasText(vcPayloadJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(vcPayloadJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.CORE_API_RESPONSE_INVALID, exception);
        }
    }

    private String resolveIssuerAccount(
            Credential credential // Credential
    ) {
        String issuerDid = credential == null ? null : credential.getIssuerDid();
        if (!StringUtils.hasText(issuerDid)) {
            return null;
        }
        String prefix = "did:xrpl:1:";
        String normalized = issuerDid.trim();
        return normalized.startsWith(prefix) ? normalized.substring(prefix.length()) : null;
    }

    private void validateOfferActiveForRead(
            CredentialOffer offer, // Credential Offer
            LocalDateTime now // 기준 일시
    ) {
        if (offer.isUsed()) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_ALREADY_USED);
        }
        if (!offer.isActive(now)) {
            if (offer.isExpired(now) && KyvcEnums.CredentialOfferStatus.ACTIVE == offer.getOfferStatus()) {
                throw new ApiException(ErrorCode.CREDENTIAL_OFFER_EXPIRED);
            }
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_ACTIVE);
        }
    }

    private void validateOfferActiveForPrepare(
            CredentialOffer offer, // Credential Offer
            LocalDateTime now // 기준 일시
    ) {
        if (offer.isUsed()) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_ALREADY_USED);
        }
        if (!offer.isActive(now)) {
            if (offer.isExpired(now) && KyvcEnums.CredentialOfferStatus.ACTIVE == offer.getOfferStatus()) {
                offer.markExpired();
                credentialOfferRepository.save(offer);
                throw new ApiException(ErrorCode.CREDENTIAL_OFFER_EXPIRED);
            }
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_NOT_ACTIVE);
        }
    }

    private void validateQrToken(
            CredentialOffer offer, // Credential Offer
            String qrToken // QR 토큰 원문
    ) {
        String tokenHash = TokenHashUtil.sha256(normalizeRequiredText(qrToken)); // QR 토큰 해시
        if (!tokenHash.equals(offer.getOfferTokenHash())) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_INVALID_TOKEN);
        }
    }

    private void validatePrepareRequest(
            WalletCredentialPrepareRequest request // 준비 요청
    ) {
        if (request == null
                || !StringUtils.hasText(request.qrToken())
                || !StringUtils.hasText(request.deviceId())
                || !StringUtils.hasText(request.holderDid())
                || !StringUtils.hasText(request.holderXrplAddress())
                || !Boolean.TRUE.equals(request.accepted())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateConfirmRequest(
            WalletCredentialConfirmRequest request // 저장 확정 요청
    ) {
        if (request == null
                || request.credentialId() == null
                || request.credentialId() <= 0
                || !StringUtils.hasText(request.deviceId())
                || !Boolean.TRUE.equals(request.walletSaved())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validatePreparedRequestMatchesOffer(
            CredentialOffer offer, // Credential Offer
            WalletCredentialPrepareRequest request // 준비 요청
    ) {
        if (!normalizeRequiredText(request.deviceId()).equals(offer.getDeviceId())
                || !normalizeRequiredText(request.holderDid()).equals(offer.getHolderDid())
                || !normalizeRequiredText(request.holderXrplAddress()).equals(offer.getHolderXrplAddress())) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_PREPARE_FAILED);
        }
    }

    private void validateConfirmMatchesOffer(
            CredentialOffer offer, // Credential Offer
            WalletCredentialConfirmRequest request // 저장 확정 요청
    ) {
        if (!offer.isPrepared()) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_CONFIRM_FAILED);
        }
        if (!request.credentialId().equals(offer.getCredentialId())) {
            if (offer.isUsed()) {
                throw new ApiException(ErrorCode.CREDENTIAL_OFFER_ALREADY_USED);
            }
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_CONFIRM_FAILED);
        }
        if (!normalizeRequiredText(request.deviceId()).equals(offer.getDeviceId())) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_CONFIRM_FAILED);
        }
    }

    private void validatePreparedCredential(
            Credential credential // Credential
    ) {
        if (credential == null || KyvcEnums.CredentialStatus.VALID != credential.getCredentialStatus()) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_PREPARE_FAILED);
        }
        if (credential.isWalletSaved()) {
            throw new ApiException(ErrorCode.WALLET_CREDENTIAL_ALREADY_SAVED);
        }
    }

    private void validateCredentialOwnership(
            CredentialOffer offer, // Credential Offer
            Credential credential // Credential
    ) {
        if (credential == null
                || !credential.isOwnedByCorporate(offer.getCorporateId())
                || !credential.getKycId().equals(offer.getKycId())) {
            throw new ApiException(ErrorCode.CREDENTIAL_ACCESS_DENIED);
        }
    }

    private void validateKycOwnership(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            KycApplication kycApplication // KYC 신청
    ) {
        if (kycApplication == null
                || !kycApplication.isOwnedBy(userId)
                || !corporateId.equals(kycApplication.getCorporateId())) {
            throw new ApiException(ErrorCode.KYC_ACCESS_DENIED);
        }
    }

    private void validateOfferCorporate(
            Long corporateId, // 법인 ID
            CredentialOffer offer // Credential Offer
    ) {
        if (offer == null || !offer.isOwnedByCorporate(corporateId)) {
            throw new ApiException(ErrorCode.CREDENTIAL_OFFER_ACCESS_DENIED);
        }
    }

    private Corporate getCorporateByUserId(
            Long userId // 사용자 ID
    ) {
        return corporateRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN));
    }

    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateId(
            Long id // 식별자
    ) {
        if (id == null || id <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
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

    private void saveAuditLog(
            Long userId, // 사용자 ID
            String actionType, // 감사 작업 유형
            Long offerId, // Offer ID
            String summary // 감사 요약
    ) {
        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.USER.name(),
                userId,
                actionType,
                KyvcEnums.AuditTargetType.CREDENTIAL_OFFER.name(),
                offerId,
                summary,
                null
        ));
    }

    private Map<String, Object> createOfferLogFields(
            Long userId, // 사용자 ID
            Long corporateId, // 법인 ID
            Long offerId, // Offer ID
            Long kycId, // KYC 신청 ID
            Long credentialId // Credential ID
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("userId", userId);
        fields.put("corporateId", corporateId);
        fields.put("offerId", offerId);
        fields.put("kycId", kycId);
        fields.put("credentialId", credentialId);
        return fields;
    }

    private Map<String, Object> createConfirmLogFields(
            Long userId, // 사용자 ID
            CredentialOffer offer, // Credential Offer
            Credential credential, // Credential
            boolean credentialAcceptHashProvided // CredentialAccept 해시 제공 여부
    ) {
        Map<String, Object> fields = createOfferLogFields(
                userId,
                offer.getCorporateId(),
                offer.getCredentialOfferId(),
                offer.getKycId(),
                credential.getCredentialId()
        );
        fields.put("credentialAcceptHashProvided", credentialAcceptHashProvided);
        return fields;
    }

    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
