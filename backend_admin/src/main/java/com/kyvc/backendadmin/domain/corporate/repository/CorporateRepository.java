package com.kyvc.backendadmin.domain.corporate.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.corporate.domain.Corporate;
import com.kyvc.backendadmin.domain.user.domain.User;

import java.util.Optional;

/**
 * 법인 사용자 관리의 단건 조회 책임을 가지는 Repository입니다.
 *
 * <p>users와 corporates 테이블의 단건 조회, 상태 변경 감사로그 저장을 담당하고,
 * 목록/조인 검색은 {@link CorporateQueryRepository}로 분리합니다.</p>
 */
public interface CorporateRepository {

    /**
     * users 테이블에서 사용자 ID 기준으로 법인 사용자 계정을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 사용자 Optional
     */
    Optional<User> findUserById(Long userId);

    /**
     * corporates 테이블에서 법인 ID 기준으로 법인 정보를 조회합니다.
     *
     * @param corporateId 조회할 법인 ID
     * @return 법인 Optional
     */
    Optional<Corporate> findCorporateById(Long corporateId);

    /**
     * audit_logs 테이블에 사용자 상태 변경 감사로그를 저장합니다.
     *
     * @param auditLog 저장할 감사로그
     * @return 저장된 감사로그
     */
    AuditLog saveAuditLog(AuditLog auditLog);
}
