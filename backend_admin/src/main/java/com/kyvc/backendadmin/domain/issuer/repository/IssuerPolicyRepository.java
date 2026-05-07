package com.kyvc.backendadmin.domain.issuer.repository;

import com.kyvc.backendadmin.domain.issuer.domain.IssuerPolicy;
import com.kyvc.backendadmin.global.util.KyvcEnums;

import java.util.List;
import java.util.Optional;

/** Issuer 정책 저장/단건 조회 Repository입니다. */
public interface IssuerPolicyRepository {

    /** Issuer 정책을 저장합니다. */
    IssuerPolicy save(IssuerPolicy policy);

    /** Issuer 정책 ID로 정책을 조회합니다. */
    Optional<IssuerPolicy> findById(Long policyId);

    /** Issuer DID와 정책 유형으로 활성 정책 존재 여부를 조회합니다. */
    boolean existsActiveByIssuerDidAndType(String issuerDid, KyvcEnums.IssuerPolicyType policyType);

    /** Issuer DID 기준 활성 정책 목록을 조회합니다. */
    List<IssuerPolicy> findActiveByIssuerDid(String issuerDid);
}
