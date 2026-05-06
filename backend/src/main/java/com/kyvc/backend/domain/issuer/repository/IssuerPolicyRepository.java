package com.kyvc.backend.domain.issuer.repository;

import com.kyvc.backend.domain.issuer.domain.IssuerPolicy;

import java.util.List;

// Issuer 정책 Repository
public interface IssuerPolicyRepository {

    List<IssuerPolicy> findActivePolicies();

    List<IssuerPolicy> findActivePoliciesByCredentialType(
            String credentialTypeCode // Credential 유형 코드
    );
}
