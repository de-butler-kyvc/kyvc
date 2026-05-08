package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.CorporateRepresentative;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 법인 대표자 JPA Repository
 */
public interface CorporateRepresentativeJpaRepository extends JpaRepository<CorporateRepresentative, Long> {

    /**
     * 법인 ID 기준 대표자 조회
     *
     * @param corporateId 법인 ID
     * @return 대표자 조회 결과
     */
    Optional<CorporateRepresentative> findByCorporateId(
            Long corporateId // 법인 ID
    );
}
