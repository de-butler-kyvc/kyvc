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
     * users 테이블에 법인 사용자 계정을 저장합니다.
     *
     * @param user 저장할 사용자 엔티티
     * @return 저장된 사용자 엔티티
     */
    User saveUser(User user);

    /**
     * users 테이블에서 이메일 중복 여부를 확인합니다.
     *
     * @param email 확인할 로그인 이메일
     * @return 이미 존재하면 true
     */
    boolean existsUserByEmail(String email);

    /**
     * corporates 테이블에서 법인 ID 기준으로 법인 정보를 조회합니다.
     *
     * @param corporateId 조회할 법인 ID
     * @return 법인 Optional
     */
    Optional<Corporate> findCorporateById(Long corporateId);

    /**
     * corporates 테이블에 법인 정보를 저장합니다.
     *
     * @param corporate 저장할 법인 엔티티
     * @return 저장된 법인 엔티티
     */
    Corporate saveCorporate(Corporate corporate);

    /**
     * 진행 중인 KYC 신청 존재 여부를 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 진행 중인 KYC가 있으면 true
     */
    boolean existsActiveKycByUserId(Long userId);

    /**
     * 발급 중이거나 유효한 Credential 존재 여부를 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 발급 중이거나 유효한 Credential이 있으면 true
     */
    boolean existsValidCredentialByUserId(Long userId);

    /**
     * audit_logs 테이블에 사용자 상태 변경 감사로그를 저장합니다.
     *
     * @param auditLog 저장할 감사로그
     * @return 저장된 감사로그
     */
    AuditLog saveAuditLog(AuditLog auditLog);
}
