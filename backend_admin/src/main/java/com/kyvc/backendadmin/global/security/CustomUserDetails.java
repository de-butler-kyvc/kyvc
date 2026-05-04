package com.kyvc.backendadmin.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

// JWT 기반 사용자 인증 정보
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String userType;
    private final List<String> roles;
    private final boolean enabled;

    public CustomUserDetails(
            Long userId, // 사용자 ID
            String email, // 사용자 이메일
            String userType, // 사용자 유형
            List<String> roles, // 권한 목록
            boolean enabled // 활성 여부
    ) {
        this.userId = userId;
        this.email = email;
        this.userType = userType;
        this.roles = List.copyOf(roles);
        this.enabled = enabled;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
