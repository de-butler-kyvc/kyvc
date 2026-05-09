package com.kyvc.backend.domain.did.repository;

import com.kyvc.backend.domain.did.domain.DidInstitution;
import com.kyvc.backend.global.util.KyvcEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * DID 기관 매핑 JPA Repository
 */
public interface DidInstitutionJpaRepository extends JpaRepository<DidInstitution, String> {

    /**
     * DID와 상태 기준 기관 매핑 조회
     *
     * @param did DID
     * @param statusCode 상태 코드
     * @return DID 기관 매핑 조회 결과
     */
    Optional<DidInstitution> findByDidAndStatusCode(
            String did, // DID
            KyvcEnums.DidInstitutionStatus statusCode // 상태 코드
    );
}
