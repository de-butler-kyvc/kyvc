package com.kyvc.backendadmin.domain.issuer.application;

import com.kyvc.backendadmin.domain.audit.application.AuditLogWriter;
import com.kyvc.backendadmin.domain.auth.domain.AuthToken;
import com.kyvc.backendadmin.domain.auth.repository.AuthTokenRepository;
import com.kyvc.backendadmin.domain.issuer.domain.IssuerPolicy;
import com.kyvc.backendadmin.domain.issuer.dto.*;
import com.kyvc.backendadmin.domain.issuer.repository.IssuerPolicyQueryRepository;
import com.kyvc.backendadmin.domain.issuer.repository.IssuerPolicyRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.jwt.TokenHashUtil;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Issuer 정책 등록/수정/비활성화 유스케이스를 처리하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class IssuerPolicyService {

    private final IssuerPolicyRepository issuerPolicyRepository;
    private final IssuerPolicyQueryRepository issuerPolicyQueryRepository;
    private final AuthTokenRepository authTokenRepository;
    private final AuditLogWriter auditLogWriter;

    /** Issuer 화이트리스트 정책을 등록합니다. */
    @Transactional
    public IssuerPolicyResponse createWhitelist(IssuerPolicyWhitelistCreateRequest request) {
        validateRequiredIssuer(request.issuerDid(), request.issuerName());
        String credentialTypes = joinCredentialTypes(request.credentialTypes());
        validateMfaToken(request.mfaToken());
        // issuerDid 중복 검증: 같은 Issuer DID에 활성 화이트리스트가 이미 있으면 중복 정책으로 처리한다.
        if (issuerPolicyRepository.existsActiveByIssuerDidAndType(request.issuerDid(), KyvcEnums.IssuerPolicyType.WHITELIST)) {
            throw new ApiException(ErrorCode.ISSUER_POLICY_DUPLICATED);
        }
        // whitelist/blacklist 충돌 검증: 활성 블랙리스트가 있으면 검증 거부 정책이 우선되므로 화이트리스트 등록을 막는다.
        if (issuerPolicyRepository.existsActiveByIssuerDidAndType(request.issuerDid(), KyvcEnums.IssuerPolicyType.BLACKLIST)) {
            throw new ApiException(ErrorCode.ISSUER_POLICY_CONFLICT);
        }
        IssuerPolicy policy = issuerPolicyRepository.save(IssuerPolicy.create(
                request.issuerDid(), request.issuerName(), KyvcEnums.IssuerPolicyType.WHITELIST,
                credentialTypes, KyvcEnums.IssuerPolicyStatus.ACTIVE, request.reason()
        ));
        writeAudit("ISSUER_POLICY_WHITELIST_CREATED", policy.getPolicyId(), null, summarize(policy));
        return getSavedDetail(policy.getPolicyId());
    }

    /** Issuer 블랙리스트 정책을 등록합니다. */
    @Transactional
    public IssuerPolicyResponse createBlacklist(IssuerPolicyBlacklistCreateRequest request) {
        validateRequiredIssuer(request.issuerDid(), request.issuerName());
        if (!StringUtils.hasText(request.reasonCode())) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "reasonCode는 필수입니다.");
        }
        validateMfaToken(request.mfaToken());
        // issuerDid 중복 검증: 같은 Issuer DID에 활성 블랙리스트가 이미 있으면 중복 정책으로 처리한다.
        if (issuerPolicyRepository.existsActiveByIssuerDidAndType(request.issuerDid(), KyvcEnums.IssuerPolicyType.BLACKLIST)) {
            throw new ApiException(ErrorCode.ISSUER_POLICY_DUPLICATED);
        }
        // 블랙리스트 정책은 화이트리스트보다 우선한다.
        // 동일 Issuer DID에 화이트리스트가 존재해도 블랙리스트 등록 시 검증 거부 정책으로 처리한다.
        String reason = "[%s] %s".formatted(request.reasonCode(), request.reason() == null ? "" : request.reason());
        IssuerPolicy policy = issuerPolicyRepository.save(IssuerPolicy.create(
                request.issuerDid(), request.issuerName(), KyvcEnums.IssuerPolicyType.BLACKLIST,
                null, KyvcEnums.IssuerPolicyStatus.ACTIVE, reason
        ));
        writeAudit("ISSUER_POLICY_BLACKLIST_CREATED", policy.getPolicyId(), null, summarize(policy));
        return getSavedDetail(policy.getPolicyId());
    }

    /** Issuer 정책을 수정합니다. */
    @Transactional
    public IssuerPolicyResponse update(Long policyId, IssuerPolicyUpdateRequest request) {
        IssuerPolicy policy = findPolicy(policyId);
        if (StringUtils.hasText(request.mfaToken())) {
            validateMfaToken(request.mfaToken());
        }
        String before = summarize(policy);
        String credentialTypes = request.credentialTypes() == null ? null : joinCredentialTypes(request.credentialTypes());
        KyvcEnums.IssuerPolicyStatus status = parseStatus(request.status());
        policy.update(blankToNull(request.issuerName()), credentialTypes, status, request.reason());
        writeAudit("ISSUER_POLICY_UPDATED", policyId, before, summarize(policy));
        return getSavedDetail(policyId);
    }

    /** Issuer 정책을 비활성화합니다. */
    @Transactional
    public IssuerPolicyResponse disable(Long policyId) {
        IssuerPolicy policy = findPolicy(policyId);
        String before = summarize(policy);
        // 정책 비활성화 처리: 물리 삭제하지 않고 상태만 INACTIVE로 변경한다.
        policy.disable();
        writeAudit("ISSUER_POLICY_DISABLED", policyId, before, summarize(policy));
        return getSavedDetail(policyId);
    }

    private void validateRequiredIssuer(String issuerDid, String issuerName) {
        if (!StringUtils.hasText(issuerDid) || !StringUtils.hasText(issuerName)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String joinCredentialTypes(List<String> credentialTypes) {
        if (credentialTypes == null || credentialTypes.isEmpty()) return null;
        return credentialTypes.stream().map(this::validateCredentialType).collect(Collectors.joining(","));
    }

    private String validateCredentialType(String credentialType) {
        try {
            return KyvcEnums.CredentialType.valueOf(credentialType).name();
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "credentialTypes 값이 유효하지 않습니다.");
        }
    }

    private KyvcEnums.IssuerPolicyStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return KyvcEnums.IssuerPolicyStatus.valueOf(status);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_CODE_VALUE, "status 값이 유효하지 않습니다.");
        }
    }

    private void validateMfaToken(String rawMfaToken) {
        // MFA 토큰 검증: 중요한 Issuer 정책 변경은 현재 관리자 소유 ACTIVE MFA_SESSION 토큰만 허용한다.
        Long adminId = SecurityUtil.getCurrentAdminId();
        AuthToken token = authTokenRepository.findByTokenHashAndTokenType(TokenHashUtil.sha256(rawMfaToken), KyvcEnums.TokenType.MFA_SESSION)
                .orElseThrow(() -> new ApiException(ErrorCode.MFA_TOKEN_INVALID));
        if (KyvcEnums.ActorType.ADMIN != token.getActorType() || !adminId.equals(token.getActorId())
                || !token.isActive() || token.isExpired(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.MFA_TOKEN_INVALID);
        }
        token.markUsed(LocalDateTime.now());
    }

    private IssuerPolicy findPolicy(Long policyId) {
        return issuerPolicyRepository.findById(policyId).orElseThrow(() -> new ApiException(ErrorCode.ISSUER_POLICY_NOT_FOUND));
    }

    private IssuerPolicyResponse getSavedDetail(Long policyId) {
        return issuerPolicyQueryRepository.findDetailById(policyId).orElseThrow(() -> new ApiException(ErrorCode.ISSUER_POLICY_NOT_FOUND));
    }

    private void writeAudit(String action, Long targetId, String before, String after) {
        // audit_logs 기록: Issuer 정책 변경은 모두 감사 대상이므로 변경 전후 요약을 남긴다.
        auditLogWriter.write(KyvcEnums.ActorType.ADMIN, SecurityUtil.getCurrentAdminId(), action,
                KyvcEnums.AuditTargetType.ISSUER_POLICY, targetId, action, before, after);
    }

    private String summarize(IssuerPolicy policy) {
        return "issuerDid=%s, issuerName=%s, policyType=%s, credentialType=%s, status=%s, reason=%s"
                .formatted(policy.getIssuerDid(), policy.getIssuerName(), policy.getPolicyType(),
                        policy.getCredentialTypeCode(), policy.getStatus(), policy.getReason());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
