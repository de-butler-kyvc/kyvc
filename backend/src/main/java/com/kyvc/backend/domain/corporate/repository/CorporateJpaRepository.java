package com.kyvc.backend.domain.corporate.repository;

import com.kyvc.backend.domain.corporate.domain.Corporate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 법인 JPA Repository
 */
public interface CorporateJpaRepository extends JpaRepository<Corporate, Long> {

    /**
     * 사용자 ID 기준 법인 조회
     *
     * @param userId 사용자 ID
     * @return 법인 조회 결과
     */
    Optional<Corporate> findByUserId(
            Long userId // 사용자 ID
    );

    /**
     * 사업자등록번호 기준 법인 조회
     *
     * @param businessRegistrationNo 사업자등록번호
     * @return 법인 조회 결과
     */
    Optional<Corporate> findByBusinessRegistrationNo(
            String businessRegistrationNo // 사업자등록번호
    );

    /**
     * 사용자 ID 기준 법인 존재 여부
     *
     * @param userId 사용자 ID
     * @return 법인 존재 여부
     */
    boolean existsByUserId(
            Long userId // 사용자 ID
    );

    /**
     * 사업자등록번호 기준 법인 존재 여부
     *
     * @param businessRegistrationNo 사업자등록번호
     * @return 법인 존재 여부
     */
    boolean existsByBusinessRegistrationNo(
            String businessRegistrationNo // 사업자등록번호
    );

    /**
     * 다른 법인의 사업자등록번호 사용 여부
     *
     * @param businessRegistrationNo 사업자등록번호
     * @param corporateId 제외할 법인 ID
     * @return 다른 법인 사용 여부
     */
    boolean existsByBusinessRegistrationNoAndCorporateIdNot(
            String businessRegistrationNo, // 사업자등록번호
            Long corporateId // 제외할 법인 ID
    );
}
