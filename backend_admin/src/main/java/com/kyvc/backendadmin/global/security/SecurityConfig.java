package com.kyvc.backendadmin.global.security;

import com.kyvc.backendadmin.global.jwt.JwtProperties;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

// JWT 기반 보안 설정
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, KyvcCorsProperties.class, DevTokenProperties.class})
public class SecurityConfig {

    private static final String ADMIN_USER_TYPE = "ADMIN";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String BACKEND_ADMIN_ROLE = ROLE_PREFIX + KyvcEnums.RoleCode.BACKEND_ADMIN.name();
    private static final String SYSTEM_ADMIN_ROLE = ROLE_PREFIX + KyvcEnums.RoleCode.SYSTEM_ADMIN.name();
    private static final Set<String> BACKEND_ADMIN_ACTION_ROLES = Set.of(
            BACKEND_ADMIN_ROLE
    );
    private static final Set<String> ADMIN_COMMON_ROLES = Set.of(
            BACKEND_ADMIN_ROLE,
            SYSTEM_ADMIN_ROLE
    );
    private static final Set<String> SYSTEM_ADMIN_ROLES = Set.of(
            SYSTEM_ADMIN_ROLE
    );

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final KyvcCorsProperties kyvcCorsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http // HttpSecurity 설정 객체
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                AdminSecurityPatterns.PUBLIC_PATTERNS
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                AdminSecurityPatterns.PUBLIC_GET_PATTERNS
                        ).permitAll()
                        .requestMatchers(
                                AdminSecurityPatterns.SYSTEM_ADMIN_ONLY_PATTERNS
                        ).access(adminRoleAuthorizationManager(SYSTEM_ADMIN_ROLES))
                        .requestMatchers(
                                HttpMethod.POST,
                                AdminSecurityPatterns.BACKEND_ADMIN_ONLY_PATTERNS
                        ).access(adminRoleAuthorizationManager(BACKEND_ADMIN_ACTION_ROLES))
                        .requestMatchers(
                                AdminSecurityPatterns.ADMIN_COMMON_PATTERNS
                        ).access(adminRoleAuthorizationManager(ADMIN_COMMON_ROLES))
                        .requestMatchers(
                                AdminSecurityPatterns.BACKEND_API_FALLBACK_PATTERN
                        ).access(adminRoleAuthorizationManager(SYSTEM_ADMIN_ROLES))
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private AuthorizationManager<RequestAuthorizationContext> adminRoleAuthorizationManager(Set<String> allowedRoles) {
        return (Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) -> {
            Authentication authentication = authenticationSupplier.get();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }

            Object principal = authentication.getPrincipal();
            boolean granted = principal instanceof CustomUserDetails userDetails
                    && ADMIN_USER_TYPE.equals(userDetails.getUserType())
                    && authentication.getAuthorities().stream()
                    .anyMatch(authority -> allowedRoles.contains(authority.getAuthority()));
            return new AuthorizationDecision(granted);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration(); // CORS 설정
        corsConfiguration.setAllowedOrigins(kyvcCorsProperties.getAllowedOrigins());
        // 외부/로컬 개발 환경에서 credentials 요청도 테스트할 수 있도록 origin pattern을 env로 열 수 있다.
        corsConfiguration.setAllowedOriginPatterns(kyvcCorsProperties.getAllowedOriginPatterns());
        corsConfiguration.setAllowCredentials(kyvcCorsProperties.isAllowCredentials());
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setExposedHeaders(List.of("X-Request-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); // 경로별 CORS 설정 소스
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
