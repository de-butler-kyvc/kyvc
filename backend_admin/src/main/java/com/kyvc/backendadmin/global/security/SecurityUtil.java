package com.kyvc.backendadmin.global.security;

import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.StringUtils;

// Spring SecurityContext에서 현재 인증된 관리자 정보를 꺼내는 유틸
/**
 * Spring SecurityContext에서 현재 인증된 관리자 principal을 조회하는 보안 유틸입니다.
 *
 * <p>Backend Admin API에서 현재 관리자 ID, 이메일, 권한을 조회할 때 사용합니다.
 * 인증 정보가 없거나 principal 유형이 관리자와 맞지 않으면 UNAUTHORIZED ApiException을 발생시킵니다.</p>
 */
public final class SecurityUtil {

    // Backend Admin API에서 허용하는 principal userType
    private static final String ADMIN_USER_TYPE = "ADMIN";
    // Spring Security 권한 문자열 표준 접두사
    private static final String ROLE_PREFIX = "ROLE_";

    // 유틸 클래스 인스턴스 생성을 막음
    private SecurityUtil() {
    }

    // 현재 인증된 관리자 ID 조회
    /**
     * 현재 인증된 관리자 ID를 조회합니다.
     *
     * @return 현재 관리자 ID
     */
    public static Long getCurrentAdminId() {
        return getCurrentUserDetails().getUserId();
    }

    // 현재 인증된 관리자 이메일 조회
    /**
     * 현재 인증된 관리자 이메일을 조회합니다.
     *
     * @return 현재 관리자 이메일
     */
    public static String getCurrentAdminEmail() {
        return getCurrentUserDetails().getEmail();
    }

    // 현재 관리자가 특정 역할을 보유하는지 확인
    /**
     * 현재 인증된 관리자가 특정 역할을 보유하는지 확인합니다.
     *
     * @param role 확인할 역할 문자열, ROLE_ 접두사가 없으면 자동 보정
     * @return 역할을 보유하면 true
     */
    public static boolean hasRole(String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }

        String normalizedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return getCurrentUserDetails().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(normalizedRole::equals);
    }

    // 현재 principal을 CustomUserDetails로 검증해 반환
    /**
     * 현재 인증 principal을 Backend Admin용 CustomUserDetails로 조회합니다.
     *
     * @return 현재 인증된 관리자 CustomUserDetails
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)
                || !ADMIN_USER_TYPE.equals(userDetails.getUserType())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        return userDetails;
    }
}
