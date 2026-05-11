package com.kyvc.backend.domain.auth.application;

import com.kyvc.backend.domain.auth.dto.AuthRoleResponse;
import com.kyvc.backend.domain.auth.dto.AuthRoleSelectRequest;
import com.kyvc.backend.domain.auth.dto.AuthRoleSelectResponse;
import com.kyvc.backend.domain.auth.repository.RoleRepository;
import com.kyvc.backend.domain.auth.repository.UserRoleRepository;
import com.kyvc.backend.domain.user.domain.User;
import com.kyvc.backend.domain.user.repository.UserRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.jwt.JwtTokenProvider;
import com.kyvc.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

// 세션 역할 선택 서비스
@Service
@Transactional
@RequiredArgsConstructor
public class AuthRoleService {

    private static final String SPRING_ROLE_PREFIX = "ROLE_"; // Spring Security 권한 접두어

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // 선택 가능 역할 목록 조회
    @Transactional(readOnly = true)
    public AuthRoleResponse getRoles(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        Long userId = getAuthenticatedUserId(userDetails);
        List<RoleRepository.RoleRow> roles = roleRepository.findEnabledRolesByUserId(userId); // 보유 역할 목록
        String selectedRoleCode = resolveSelectedRoleCode(userDetails, roles); // 선택 역할 코드

        List<AuthRoleResponse.RoleItem> items = roles.stream()
                .map(role -> new AuthRoleResponse.RoleItem(
                        role.roleId(),
                        role.roleCode(),
                        role.roleName(),
                        role.roleTypeCode(),
                        role.roleCode().equals(selectedRoleCode)
                ))
                .toList();
        return new AuthRoleResponse(items);
    }

    // 세션 역할 선택
    public SelectedRoleResult selectRole(
            Long userId, // 사용자 ID
            AuthRoleSelectRequest request // 역할 선택 요청
    ) {
        validateUserId(userId);
        validateRequest(request);

        String roleCode = normalizeRoleCode(request.roleCode()); // 선택 역할 코드
        roleRepository.findEnabledByRoleCode(roleCode)
                .orElseThrow(() -> new ApiException(ErrorCode.ROLE_NOT_FOUND));
        if (!userRoleRepository.existsEnabledUserRole(userId, roleCode)) {
            throw new ApiException(ErrorCode.INVALID_ROLE_SELECTION);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.USER_INACTIVE);
        }

        String accessToken = jwtTokenProvider.createAccessToken(
                user,
                List.of(toSpringRole(roleCode))
        ); // 선택 역할 반영 Access Token
        return new SelectedRoleResult(
                new AuthRoleSelectResponse(roleCode, true),
                accessToken
        );
    }

    // 선택 역할 코드 산정
    private String resolveSelectedRoleCode(
            CustomUserDetails userDetails, // 인증 사용자 정보
            List<RoleRepository.RoleRow> roles // 보유 역할 목록
    ) {
        if (roles.isEmpty()) {
            return null;
        }

        List<String> selectedRoles = userDetails.getRoles(); // Access Token 권한 목록
        return roles.stream()
                .map(RoleRepository.RoleRow::roleCode)
                .filter(roleCode -> selectedRoles.contains(toSpringRole(roleCode)))
                .findFirst()
                .orElse(roles.getFirst().roleCode());
    }

    // 인증 사용자 ID 조회
    private Long getAuthenticatedUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }

    // 사용자 ID 검증
    private void validateUserId(
            Long userId // 사용자 ID
    ) {
        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    // 역할 선택 요청 검증
    private void validateRequest(
            AuthRoleSelectRequest request // 역할 선택 요청
    ) {
        if (request == null || !StringUtils.hasText(request.roleCode())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
    }

    // 역할 코드 정규화
    private String normalizeRoleCode(
            String roleCode // 원본 역할 코드
    ) {
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }

    // Spring Security 권한명 변환
    private String toSpringRole(
            String roleCode // 역할 코드
    ) {
        return SPRING_ROLE_PREFIX + roleCode;
    }

    /**
     * 선택 역할 처리 결과
     *
     * @param body 응답 본문
     * @param accessToken Access Token 원문
     */
    public record SelectedRoleResult(
            AuthRoleSelectResponse body,
            String accessToken
    ) {
    }
}
