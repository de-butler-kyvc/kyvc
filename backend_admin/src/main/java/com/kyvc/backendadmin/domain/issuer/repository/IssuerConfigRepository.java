package com.kyvc.backendadmin.domain.issuer.repository;

/** Issuer 발급 설정 기본 Repository입니다. */
public interface IssuerConfigRepository {

    /** Issuer 발급 설정 존재 여부를 조회합니다. */
    boolean existsById(Long issuerConfigId);
}
