package com.kyvc.backendadmin.domain.admin.application;

import com.kyvc.backendadmin.domain.admin.domain.AdminRole;
import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.domain.AdminUserRole;
import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserCreateRequest;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserDetailResponse;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserSearchRequest;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserSummaryResponse;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserUpdateRequest;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserQueryRepository;
import com.kyvc.backendadmin.domain.admin.repository.AdminUserRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.security.SecurityUtil;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Backend Admin 관리자 계정 관리 유스케이스 서비스
/**
 * Backend Admin 관리자 계정 관리 유스케이스를 처리하는 서비스입니다.
 *
 * <p>관리자 계정 목록/상세 조회, 생성, 수정을 담당합니다. 생성과 수정 흐름에서는
 * 상태 enum, 이메일 중복, 권한 존재 여부, 자기 자신 비활성화 방지를 검증하고,
 * 주요 변경 사항을 audit_logs에 기록합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    // 관리자 계정 기본 저장소
    private final AdminUserRepository adminUserRepository;
    // 관리자 목록/검색 전용 저장소
    private final AdminUserQueryRepository adminUserQueryRepository;
    // BCrypt 비밀번호 인코더
    private final PasswordEncoder passwordEncoder;

    // 관리자 목록 검색
    /**
     * 관리자 계정 목록을 검색합니다.
     *
     * <p>검색 조건의 상태와 권한 enum 값을 검증한 뒤 QueryRepository를 통해
     * admin_users, admin_user_roles, admin_roles 기준의 목록과 총 건수를 조회합니다.</p>
     *
     * @param request 페이징과 검색 조건
     * @return 관리자 계정 목록과 페이지 정보
     */
    @Transactional(readOnly = true)
    public AdminUserSummaryResponse search(AdminUserSearchRequest request) {
        validateSearchEnums(request);
        List<AdminUser> adminUsers = adminUserQueryRepository.search(request);
        long totalElements = adminUserQueryRepository.count(request);
        List<AdminUserSummaryResponse.Item> items = adminUsers.stream()
                .map(this::toSummaryItem)
                .toList();

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / request.size());
        return new AdminUserSummaryResponse(items, request.page(), request.size(), totalElements, totalPages);
    }

    // 관리자 상세 조회
    /**
     * 관리자 계정 상세 정보를 조회합니다.
     *
     * <p>admin_users.admin_id로 계정을 조회하고, 할당된 권한 목록을 함께 구성합니다.</p>
     *
     * @param adminUserId 조회할 관리자 ID
     * @return 관리자 계정 상세 정보
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse getDetail(Long adminUserId) {
        AdminUser adminUser = getAdminUser(adminUserId);
        return toDetail(adminUser);
    }

    // 관리자 계정 생성
    /**
     * 관리자 계정을 생성합니다.
     *
     * <p>이메일 중복, 상태 enum, 비밀번호 정책, 권한 ID 존재 여부를 검증합니다.
     * 비밀번호는 BCrypt로 저장하고 권한 매핑을 생성한 뒤 audit_logs에 생성 이력을 기록합니다.</p>
     *
     * @param request 관리자 생성 요청
     * @return 생성된 관리자 계정 상세 정보
     */
    @Transactional
    public AdminUserDetailResponse create(AdminUserCreateRequest request) {
        if (adminUserRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.ADMIN_ALREADY_EXISTS);
        }

        KyvcEnums.AdminUserStatus status = parseStatus(request.status());
        validatePasswordPolicy(request.password());
        List<AdminRole> roles = getValidRoles(request.roleIds());

        AdminUser adminUser = AdminUser.create(
                request.email(),
                request.name(),
                passwordEncoder.encode(request.password()),
                status
        );
        adminUserRepository.save(adminUser);
        roles.forEach(role -> adminUserRepository.saveUserRole(AdminUserRole.create(adminUser, role)));
        writeAuditLog(adminUser.getAdminId(), "ADMIN_USER_CREATE", "관리자 계정을 생성했습니다.");
        return toDetail(adminUser);
    }

    // 관리자 계정 수정
    /**
     * 관리자 계정을 수정합니다.
     *
     * <p>관리자 존재 여부와 상태 enum을 검증하고, 현재 인증된 관리자가 자기 자신을
     * 비활성화하거나 잠그는 것을 방지합니다. 수정 후 audit_logs에 변경 이력을 기록합니다.</p>
     *
     * @param adminUserId 수정할 관리자 ID
     * @param request 관리자 수정 요청
     * @return 수정된 관리자 계정 상세 정보
     */
    @Transactional
    public AdminUserDetailResponse update(Long adminUserId, AdminUserUpdateRequest request) {
        AdminUser adminUser = getAdminUser(adminUserId);
        KyvcEnums.AdminUserStatus status = parseStatus(request.status());
        if (SecurityUtil.getCurrentAdminId().equals(adminUserId) && KyvcEnums.AdminUserStatus.ACTIVE != status) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "자기 자신을 비활성화하거나 잠글 수 없습니다.");
        }

        adminUser.update(request.name(), status);
        writeAuditLog(adminUser.getAdminId(), "ADMIN_USER_UPDATE", "관리자 계정을 수정했습니다.");
        return toDetail(adminUser);
    }

    // 관리자 단건 조회, 없으면 ADMIN_NOT_FOUND
    private AdminUser getAdminUser(Long adminUserId) {
        return adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.ADMIN_NOT_FOUND));
    }

    // 목록 item DTO 변환
    private AdminUserSummaryResponse.Item toSummaryItem(AdminUser adminUser) {
        return new AdminUserSummaryResponse.Item(
                adminUser.getAdminId(),
                adminUser.getEmail(),
                adminUser.getDisplayName(),
                adminUser.getStatus().name(),
                adminUserRepository.findRoleCodesByAdminId(adminUser.getAdminId()),
                adminUser.getCreatedAt()
        );
    }

    // 상세 DTO 변환
    private AdminUserDetailResponse toDetail(AdminUser adminUser) {
        return new AdminUserDetailResponse(
                adminUser.getAdminId(),
                adminUser.getEmail(),
                adminUser.getDisplayName(),
                adminUser.getStatus().name(),
                adminUserRepository.findRoleCodesByAdminId(adminUser.getAdminId()),
                adminUser.getLastLoginAt(),
                adminUser.getCreatedAt(),
                adminUser.getUpdatedAt()
        );
    }

    // roleIds에 해당하는 권한 존재 여부 검증
    private List<AdminRole> getValidRoles(List<Long> roleIds) {
        Set<Long> uniqueRoleIds = new LinkedHashSet<>(roleIds);
        List<AdminRole> roles = adminUserRepository.findRolesByIds(List.copyOf(uniqueRoleIds));
        if (roles.size() != uniqueRoleIds.size()) {
            throw new ApiException(ErrorCode.ADMIN_ROLE_NOT_FOUND);
        }
        return roles;
    }

    // 검색 조건 enum 값 사전 검증
    private void validateSearchEnums(AdminUserSearchRequest request) {
        if (request.status() != null && !request.status().isBlank()) {
            parseStatus(request.status());
        }
        if (request.roleCode() != null && !request.roleCode().isBlank()) {
            parseRoleCode(request.roleCode());
        }
    }

    // 관리자 상태 문자열을 enum으로 변환
    private KyvcEnums.AdminUserStatus parseStatus(String status) {
        try {
            return KyvcEnums.AdminUserStatus.valueOf(status);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 관리자 상태입니다.");
        }
    }

    // 권한 코드 문자열을 enum으로 변환
    private KyvcEnums.RoleCode parseRoleCode(String roleCode) {
        try {
            return KyvcEnums.RoleCode.valueOf(roleCode);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "유효하지 않은 관리자 권한 코드입니다.");
        }
    }

    // 1차 관리자 비밀번호 정책 검증
    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "비밀번호는 8자 이상이어야 합니다.");
        }
    }

    // 관리자 계정 변경 audit 로그 기록
    private void writeAuditLog(Long targetAdminId, String action, String description) {
        adminUserRepository.saveAuditLog(AuditLog.adminUser(
                SecurityUtil.getCurrentAdminId(),
                targetAdminId,
                action,
                description
        ));
    }
}
