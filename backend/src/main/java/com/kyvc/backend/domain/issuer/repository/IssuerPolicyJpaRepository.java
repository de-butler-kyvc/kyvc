package com.kyvc.backend.domain.issuer.repository;

import com.kyvc.backend.domain.issuer.domain.IssuerPolicy;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Issuer 정책 JPA Repository
public interface IssuerPolicyJpaRepository extends JpaRepository<IssuerPolicy, Long> {

    List<IssuerPolicy> findByIssuerPolicyStatusOrderByUpdatedAtDesc(
            KyvcEnums.IssuerPolicyStatus issuerPolicyStatus // Issuer 정책 상태
    );

    List<IssuerPolicy> findByIssuerPolicyStatusAndCredentialTypeCodeOrderByUpdatedAtDesc(
            KyvcEnums.IssuerPolicyStatus issuerPolicyStatus, // Issuer 정책 상태
            String credentialTypeCode // Credential 유형 코드
    );
}
