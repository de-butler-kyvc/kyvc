package com.kyvc.backend.global.security;

import com.kyvc.backend.global.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// JWT 기반 보안 설정
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, KyvcCorsProperties.class, DevTokenProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final VerifierApiKeyAuthenticationFilter verifierApiKeyAuthenticationFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;
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
                                "/api/auth/signup/corporate",
                                "/api/auth/login",
                                "/api/auth/token/refresh",
                                "/api/auth/password-reset/request",
                                "/api/auth/password-reset/confirm",
                                "/api/auth/email-verifications/request",
                                "/api/auth/email-verifications/verify",
                                "/api/auth/dev/token",
                                "/api/mobile/auth/login",
                                "/api/mobile/auth/vp-login/challenge",
                                "/api/mobile/auth/vp-login"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/common/session",
                                "/api/common/dids/*/institution",
                                "/health",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/internal/core/health").permitAll()
                        .requestMatchers("/api/internal/dev/**").permitAll()
                        .requestMatchers("/api/admin/**").denyAll()
                        .requestMatchers("/api/verifier/**").authenticated()
                        .requestMatchers(
                                "/api/auth/logout",
                                "/api/user/**",
                                "/api/corporate/**",
                                "/api/mobile/**"
                        ).authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(verifierApiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration(); // CORS 설정
        corsConfiguration.setAllowedOrigins(resolveAllowedOrigins());
        corsConfiguration.setAllowedOriginPatterns(resolveAllowedOriginPatterns());
        corsConfiguration.setAllowCredentials(kyvcCorsProperties.isAllowCredentials());
        corsConfiguration.setAllowedMethods(configuredAllowedMethods());
        corsConfiguration.setAllowedHeaders(configuredAllowedHeaders());
        corsConfiguration.setExposedHeaders(configuredExposedHeaders());
        corsConfiguration.setMaxAge(kyvcCorsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(); // 경로별 CORS 설정 소스
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    private List<String> resolveAllowedOrigins() {
        return normalizedList(configuredAllowedOrigins()).stream()
                .filter(origin -> !"*".equals(origin))
                .toList();
    }

    private List<String> resolveAllowedOriginPatterns() {
        List<String> patterns = new ArrayList<>(normalizedList(configuredAllowedOriginPatterns())); // Origin Pattern 목록
        boolean wildcardOriginConfigured = normalizedList(configuredAllowedOrigins()).stream()
                .anyMatch("*"::equals); // wildcard Origin 설정 여부
        if (wildcardOriginConfigured && patterns.isEmpty() && isLocalOrDevProfile()) {
            patterns.add("*");
        }
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

    private List<String> normalizedList(
            List<String> values // 설정값 목록
    ) {
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
