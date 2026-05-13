package com.kyvc.backend.global.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

// Verifier API Key 인증 토큰
public class VerifierAuthenticationToken extends AbstractAuthenticationToken {

    private static final String VERIFIER_APP_ROLE = "ROLE_VERIFIER_APP"; // Verifier Runtime 권한

    private final VerifierPrincipal principal; // 인증된 Verifier 주체

    public VerifierAuthenticationToken(
            VerifierPrincipal principal // 인증된 Verifier 주체
    ) {
        super(List.of(new SimpleGrantedAuthority(VERIFIER_APP_ROLE)));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public VerifierPrincipal getPrincipal() {
        return principal;
    }
}
