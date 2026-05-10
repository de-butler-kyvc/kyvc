package com.kyvc.backendadmin.global.security;

import com.kyvc.backendadmin.global.jwt.JwtProperties;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Backend Admin JWT 기반 보안 설정입니다.
 *
 * <p>관리자 JWT 인증과 권한 정책을 유지하면서 frontend_admin 브라우저 요청을 위한 CORS 설정을 함께 제공합니다.</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, KyvcCorsProperties.class, DevTokenProperties.class})
public class SecurityConfig {

    private static final String ADMIN_USER_TYPE = "ADMIN";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String BACKEND_ADMIN_ROLE = ROLE_PREFIX + KyvcEnums.RoleCode.BACKEND_ADMIN.name();
    private static final String SYSTEM_ADMIN_ROLE = ROLE_PREFIX + KyvcEnums.RoleCode.SYSTEM_ADMIN.name();
    private static final Set<String> BACKEND_ADMIN_ACTION_ROLES = Set.of(
            BACKEND_ADMIN_ROLE,
            SYSTEM_ADMIN_ROLE
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
    private final Environment environment;

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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                AdminSecurityPatterns.PUBLIC_PATTERNS
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                AdminSecurityPatterns.PUBLIC_GET_PATTERNS
                        ).permitAll()
                        .requestMatchers(
                                AdminSecurityPatterns.FORBIDDEN_PATTERNS
                        ).denyAll()
                        .requestMatchers(
                                AdminSecurityPatterns.SYSTEM_ADMIN_ONLY_PATTERNS
                        ).access(adminRoleAuthorizationManager(SYSTEM_ADMIN_ROLES))
                        .requestMatchers(
                                HttpMethod.POST,
                                AdminSecurityPatterns.SYSTEM_ADMIN_ONLY_POST_PATTERNS
                        ).access(adminRoleAuthorizationManager(SYSTEM_ADMIN_ROLES))
                        .requestMatchers(
                                HttpMethod.GET,
                                AdminSecurityPatterns.ADMIN_WORK_GET_PATTERNS
                        ).access(adminRoleAuthorizationManager(BACKEND_ADMIN_ACTION_ROLES))
                        .requestMatchers(
                                HttpMethod.POST,
                                AdminSecurityPatterns.ADMIN_WORK_POST_PATTERNS
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

    /**
     * frontend_admin 브라우저 요청을 처리하기 위한 CORS 설정을 생성합니다.
     *
     * <p>관리자 화면은 로컬 개발 서버와 개발/운영 관리자 도메인에서 호출되므로 해당 Origin을 허용합니다.
     * 인증 쿠키를 사용할 수 있어 credentials를 허용하되, allowedOrigins에는 wildcard를 넣지 않습니다.</p>
     *
     * @return Spring Security가 사용할 CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration(); // 관리자 프론트엔드 호출을 허용하는 CORS 설정입니다.
        corsConfiguration.setAllowedOrigins(resolveAllowedOrigins());
        corsConfiguration.setAllowedOriginPatterns(resolveAllowedOriginPatterns());
        corsConfiguration.setAllowCredentials(kyvcCorsProperties.isAllowCredentials());
        corsConfiguration.setAllowedMethods(configuredAllowedMethods());
        corsConfiguration.setAllowedHeaders(configuredAllowedHeaders());
        corsConfiguration.setExposedHeaders(configuredExposedHeaders());
        corsConfiguration.setMaxAge(kyvcCorsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); // 모든 관리자 API 경로에 동일한 CORS 정책을 적용합니다.
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    private List<String> resolveAllowedOrigins() {
        return normalizedList(configuredAllowedOrigins()).stream()
                .filter(origin -> !"*".equals(origin))
                .toList();
    }

    private List<String> resolveAllowedOriginPatterns() {
        List<String> patterns = new ArrayList<>(normalizedList(configuredAllowedOriginPatterns()).stream()
                .filter(pattern -> !"*".equals(pattern))
                .toList());
        if (isProdProfile()) {
            return patterns.stream()
                    .filter(pattern -> !"*".equals(pattern))
                    .toList();
        }
        return patterns;
    }

    private List<String> configuredAllowedOrigins() {
        return kyvcCorsProperties.getAllowedOrigins() == null
                ? List.of()
                : kyvcCorsProperties.getAllowedOrigins();
    }

    private List<String> configuredAllowedOriginPatterns() {
        return kyvcCorsProperties.getAllowedOriginPatterns() == null
                ? List.of()
                : kyvcCorsProperties.getAllowedOriginPatterns();
    }

    private List<String> configuredAllowedMethods() {
        return normalizedList(kyvcCorsProperties.getAllowedMethods());
    }

    private List<String> configuredAllowedHeaders() {
        return normalizedList(kyvcCorsProperties.getAllowedHeaders());
    }

    private List<String> configuredExposedHeaders() {
        return normalizedList(kyvcCorsProperties.getExposedHeaders());
    }

    private List<String> normalizedList(List<String> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList();
    }

    private boolean isLocalOrDevProfile() {
        return activeProfiles().stream()
                .anyMatch(profile -> "local".equals(profile) || "dev".equals(profile));
    }

    private boolean isProdProfile() {
        return activeProfiles().stream()
                .anyMatch("prod"::equals);
    }

    private List<String> activeProfiles() {
        return Arrays.stream(environment.getActiveProfiles())
                .toList();
    }
}
