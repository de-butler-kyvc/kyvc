package com.kyvc.backend.domain.issuer.repository;

import com.kyvc.backend.domain.issuer.domain.IssuerPolicy;
import com.kyvc.backend.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class IssuerPolicyRepositoryImpl implements IssuerPolicyRepository {

    private final IssuerPolicyJpaRepository issuerPolicyJpaRepository;

    @Override
    public List<IssuerPolicy> findActivePolicies() {
        return issuerPolicyJpaRepository.findByIssuerPolicyStatusOrderByUpdatedAtDesc(
                KyvcEnums.IssuerPolicyStatus.ACTIVE
        );
    }

    @Override
    public List<IssuerPolicy> findActivePoliciesByCredentialType(
            String credentialTypeCode // Credential 유형 코드
    ) {
        return issuerPolicyJpaRepository.findByIssuerPolicyStatusAndCredentialTypeCodeOrderByUpdatedAtDesc(
                KyvcEnums.IssuerPolicyStatus.ACTIVE,
                credentialTypeCode
        );
    }
}
