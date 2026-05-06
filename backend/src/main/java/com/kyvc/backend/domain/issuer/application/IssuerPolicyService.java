package com.kyvc.backend.domain.issuer.application;

import com.kyvc.backend.domain.issuer.domain.IssuerPolicy;
import com.kyvc.backend.domain.issuer.dto.EffectiveIssuerPolicyResponse;
import com.kyvc.backend.domain.issuer.dto.IssuerPolicyResponse;
import com.kyvc.backend.domain.issuer.repository.IssuerPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuerPolicyService {

    private final IssuerPolicyRepository issuerPolicyRepository;

    public EffectiveIssuerPolicyResponse getEffectivePolicies(
            String credentialTypeCode // Credential 유형 코드
    ) {
        String normalizedCredentialTypeCode = normalizeCredentialTypeCode(credentialTypeCode);

        List<IssuerPolicyResponse> policies = getPolicies(normalizedCredentialTypeCode).stream()
                .sorted(buildPolicyComparator())
                .map(this::toResponse)
                .toList();

        return new EffectiveIssuerPolicyResponse(
                policies,
                LocalDateTime.now()
        );
    }

    private String normalizeCredentialTypeCode(
            String credentialTypeCode // Credential 유형 코드
    ) {
        if (!StringUtils.hasText(credentialTypeCode)) {
            return null;
        }
        return credentialTypeCode.trim();
    }

    private List<IssuerPolicy> getPolicies(
            String credentialTypeCode // Credential 유형 코드
    ) {
        if (!StringUtils.hasText(credentialTypeCode)) {
            return issuerPolicyRepository.findActivePolicies();
        }
        return issuerPolicyRepository.findActivePoliciesByCredentialType(credentialTypeCode);
    }

    private Comparator<IssuerPolicy> buildPolicyComparator() {
        return Comparator.comparingInt(this::getPolicyPriority)
                .thenComparing(
                        this::getSortDateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        IssuerPolicy::getIssuerPolicyId,
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private int getPolicyPriority(
            IssuerPolicy policy // Issuer 정책
    ) {
        if (policy.isBlacklist()) {
            return 1;
        }
        if (policy.isWhitelist()) {
            return 2;
        }
        return 3;
    }

    private LocalDateTime getSortDateTime(
            IssuerPolicy policy // Issuer 정책
    ) {
        if (policy.getUpdatedAt() != null) {
            return policy.getUpdatedAt();
        }
        return policy.getCreatedAt();
    }

    private IssuerPolicyResponse toResponse(
            IssuerPolicy policy // Issuer 정책
    ) {
        return new IssuerPolicyResponse(
                policy.getIssuerPolicyId(),
                policy.getIssuerDid(),
                policy.getIssuerName(),
                policy.getIssuerPolicyType() != null ? policy.getIssuerPolicyType().name() : null,
                policy.getCredentialTypeCode(),
                policy.getIssuerPolicyStatus() != null ? policy.getIssuerPolicyStatus().name() : null,
                policy.getReason(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
