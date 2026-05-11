package com.kyvc.backend.domain.finance.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.AuditLogCreateCommand;
import com.kyvc.backend.domain.credential.application.CredentialIssuanceService;
import com.kyvc.backend.domain.credential.domain.Credential;
import com.kyvc.backend.domain.finance.dto.FinanceKycIssueQrRequest;
import com.kyvc.backend.domain.finance.dto.FinanceKycIssueQrResponse;
import com.kyvc.backend.domain.finance.dto.FinanceKycQrStatusResponse;
import com.kyvc.backend.domain.finance.repository.FinanceKycApplicationRepository;
import com.kyvc.backend.domain.finance.repository.FinanceKycQrRepository;
import com.kyvc.backend.domain.kyc.domain.KycApplication;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.TokenHashUtil;
import com.kyvc.backend.global.security.CustomUserDetails;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// 금융사 방문 KYC QR 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class FinanceKycQrService {

    private final FinanceContextService financeContextService;
    private final FinanceKycApplicationRepository financeKycApplicationRepository;
    private final FinanceKycQrRepository financeKycQrRepository;
    private final CredentialIssuanceService credentialIssuanceService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // VC 수령 QR 발급
    public FinanceKycIssueQrResponse issueQr(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId, // KYC 신청 ID
            FinanceKycIssueQrRequest request // QR 발급 요청
    ) {
        validateKycId(kycId);
        validateIssueRequest(request);
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        KycApplication kycApplication = findAccessibleFinanceKyc(context, kycId);
        validateQrIssuableStatus(kycApplication);

        LocalDateTime now = LocalDateTime.now(); // 기준 일시
        Credential credential = resolveCredentialForOffer(kycApplication, context.userId(), now);
        String qrToken = UUID.randomUUID().toString(); // QR 전달용 토큰
        LocalDateTime expiresAt = now.plusMinutes(request.expiresMinutes()); // QR 만료 일시
        credential.issueOffer(null, TokenHashUtil.sha256(qrToken), expiresAt);
        Credential savedCredential = financeKycQrRepository.save(credential);

        auditLogService.saveSafely(new AuditLogCreateCommand(
                KyvcEnums.ActorType.FINANCE.name(),
                context.userId(),
                "FINANCE_KYC_QR_ISSUE",
                KyvcEnums.AuditTargetType.CREDENTIAL.name(),
                savedCredential.getCredentialId(),
                "금융사 방문 KYC VC 수령 QR 발급",
                null
        ));
        return new FinanceKycIssueQrResponse(
                kycApplication.getKycId(),
                savedCredential.getCredentialId(),
                toQrPayload(savedCredential.getCredentialId(), qrToken, expiresAt),
                expiresAt,
                KyvcEnums.FinanceKycQrStatus.ACTIVE.name()
        );
    }

    // VC 수령 QR 상태 조회
    @Transactional(readOnly = true)
    public FinanceKycQrStatusResponse getQrStatus(
            CustomUserDetails userDetails, // 인증 사용자 정보
            Long kycId // KYC 신청 ID
    ) {
        validateKycId(kycId);
        FinanceContextService.FinanceContext context = financeContextService.requireFinanceStaff(userDetails);
        findAccessibleFinanceKyc(context, kycId);

        Credential credential = financeKycQrRepository.findLatestByKycId(kycId).orElse(null); // 최신 Credential
        if (credential == null) {
            return new FinanceKycQrStatusResponse(
                    kycId,
                    null,
                    KyvcEnums.FinanceKycQrStatus.NOT_ISSUED.name(),
                    null,
                    null,
                    KyvcEnums.Yn.N.name(),
                    KyvcEnums.Yn.N.name()
            );
        }

        return new FinanceKycQrStatusResponse(
                kycId,
                credential.getCredentialId(),
                resolveQrStatus(credential, LocalDateTime.now()).name(),
                enumName(credential.getCredentialStatus()),
                resolveOfferExpiresAt(credential),
                resolveOfferUsedYn(credential),
                resolveWalletSavedYn(credential)
        );
    }

    // QR 발급 대상 Credential 결정
    private Credential resolveCredentialForOffer(
            KycApplication kycApplication, // KYC 신청
            Long userId, // 금융사 직원 사용자 ID
            LocalDateTime now // 기준 일시
    ) {
        Credential latestCredential = financeKycQrRepository.findLatestByKycId(kycApplication.getKycId()).orElse(null);
        if (latestCredential != null && latestCredential.isIssuing()) {
            throw new ApiException(ErrorCode.CREDENTIAL_ALREADY_ISSUING);
        }
        if (latestCredential != null && latestCredential.isValid(now)) {
            return latestCredential;
        }
        if (latestCredential != null
                && KyvcEnums.CredentialStatus.VALID == latestCredential.getCredentialStatus()
                && latestCredential.isExpired(now)) {
            latestCredential.refreshStatus(KyvcEnums.CredentialStatus.EXPIRED);
            financeKycQrRepository.save(latestCredential);
        }
        if (!kycApplication.isCredentialIssuable()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_QR_ISSUE_NOT_ALLOWED);
        }

        Credential issuedCredential = credentialIssuanceService.issueKycCredentialForUser(kycApplication, userId);
        if (!issuedCredential.isValid(now)) {
            throw new ApiException(ErrorCode.CORE_VC_ISSUANCE_FAILED);
        }
        return issuedCredential;
    }

    // 접근 가능한 금융사 방문 KYC 조회
    private KycApplication findAccessibleFinanceKyc(
            FinanceContextService.FinanceContext context, // 금융사 직원 컨텍스트
            Long kycId // KYC 신청 ID
    ) {
        KycApplication kycApplication = financeKycApplicationRepository.findById(kycId)
                .orElseThrow(() -> new ApiException(ErrorCode.FINANCE_KYC_NOT_FOUND));
        if (!kycApplication.isFinanceVisit()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        if (!kycApplication.isFinanceVisitByStaff(context.userId())) {
            throw new ApiException(ErrorCode.FINANCE_KYC_ACCESS_DENIED);
        }
        return kycApplication;
    }

    // QR 발급 가능 상태 검증
    private void validateQrIssuableStatus(
            KycApplication kycApplication // KYC 신청
    ) {
        if (KyvcEnums.KycStatus.APPROVED != kycApplication.getKycStatus()
                && KyvcEnums.KycStatus.VC_ISSUED != kycApplication.getKycStatus()) {
            throw new ApiException(ErrorCode.FINANCE_KYC_QR_ISSUE_NOT_ALLOWED);
        }
    }

    // QR 상태 계산
    private KyvcEnums.FinanceKycQrStatus resolveQrStatus(
            Credential credential, // Credential
            LocalDateTime now // 기준 일시
    ) {
        if (credential.isWalletSaved()) {
            return KyvcEnums.FinanceKycQrStatus.WALLET_SAVED;
        }
        if (KyvcEnums.Yn.Y == credential.getOfferUsedYn()) {
            return KyvcEnums.FinanceKycQrStatus.USED;
        }
        if (!hasOfferToken(credential)) {
            return KyvcEnums.FinanceKycQrStatus.NOT_ISSUED;
        }
        LocalDateTime expiresAt = resolveOfferExpiresAt(credential); // QR 만료 일시
        if (expiresAt == null || expiresAt.isBefore(now)) {
            return KyvcEnums.FinanceKycQrStatus.EXPIRED;
        }
        return KyvcEnums.FinanceKycQrStatus.ACTIVE;
    }

    // Offer 토큰 존재 여부
    private boolean hasOfferToken(
            Credential credential // Credential
    ) {
        return StringUtils.hasText(credential.getOfferTokenHash()) || StringUtils.hasText(credential.getQrToken());
    }

    // Offer 만료 일시 결정
    private LocalDateTime resolveOfferExpiresAt(
            Credential credential // Credential
    ) {
        return credential.getOfferExpiresAt() == null ? credential.getQrExpiresAt() : credential.getOfferExpiresAt();
    }

    // Offer 사용 여부 결정
    private String resolveOfferUsedYn(
            Credential credential // Credential
    ) {
        return credential.getOfferUsedYn() == null ? KyvcEnums.Yn.N.name() : credential.getOfferUsedYn().name();
    }

    // Wallet 저장 여부 결정
    private String resolveWalletSavedYn(
            Credential credential // Credential
    ) {
        return StringUtils.hasText(credential.getWalletSavedYn())
                ? credential.getWalletSavedYn()
                : KyvcEnums.Yn.N.name();
    }

    // QR payload 생성
    private String toQrPayload(
            Long credentialId, // Credential ID
            String qrToken, // QR 전달용 토큰
            LocalDateTime expiresAt // QR 만료 일시
    ) {
        Map<String, Object> payload = new LinkedHashMap<>(); // QR payload 데이터
        payload.put("type", KyvcEnums.QrType.CREDENTIAL_OFFER.name());
        payload.put("offerId", credentialId);
        payload.put("qrToken", qrToken);
        payload.put("expiresAt", expiresAt);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    // QR 발급 요청 검증
    private void validateIssueRequest(
            FinanceKycIssueQrRequest request // QR 발급 요청
    ) {
        if (request == null || request.expiresMinutes() == null || request.expiresMinutes() <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // KYC ID 검증
    private void validateKycId(
            Long kycId // KYC 신청 ID
    ) {
        if (kycId == null || kycId <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // enum 이름 변환
    private String enumName(
            Enum<?> value // enum 값
    ) {
        return value == null ? null : value.name();
    }
}
