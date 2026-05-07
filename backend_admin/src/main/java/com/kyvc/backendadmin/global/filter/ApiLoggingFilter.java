package com.kyvc.backendadmin.global.filter;

import com.kyvc.backendadmin.global.logging.LogMessageBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

// API 요청 로그 필터
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ApiLoggingFilter extends OncePerRequestFilter {

    private final Environment environment;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            FilterChain filterChain // 다음 필터 체인
    ) throws ServletException, IOException {
        long startTime = System.nanoTime(); // 요청 처리 시간 기준점
        String path = request.getRequestURI(); // 쿼리 문자열 제외 경로
        String method = request.getMethod(); // HTTP 메서드
        String requestId = MDC.get(RequestIdFilter.MDC_KEY); // 요청 추적 ID

        log.info(LogMessageBuilder.create()
                .level("INFO")
                .env(resolveEnv())
                .requestId(requestId)
                .event("api.request.start")
                .message("API request started")
                .path(path)
                .method(method)
                .build());

        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            log.error(LogMessageBuilder.create()
                    .level("ERROR")
                    .env(resolveEnv())
                    .requestId(requestId)
                    .event("api.request.error")
                    .message("API request failed")
                    .path(path)
                    .method(method)
                    .status(resolveErrorStatus(response))
                    .durationMs(resolveDurationMs(startTime))
                    .exception(exception.getClass().getName())
                    .build());
            throw exception;
        }

        log.info(LogMessageBuilder.create()
                .level("INFO")
                .env(resolveEnv())
                .requestId(requestId)
                .event("api.request.complete")
                .message("API request completed")
                .path(path)
                .method(method)
                .status(response.getStatus())
                .durationMs(resolveDurationMs(startTime))
                .build());
    }

    // 예외 로그용 HTTP 상태 결정
    private int resolveErrorStatus(HttpServletResponse response // 응답 정보
    ) {
        if (response.isCommitted()) {
            return response.getStatus();
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    // 요청 처리 시간 계산
    private long resolveDurationMs(long startTime // 시작 시각 나노초
    ) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    // 현재 실행 환경명 조회
    private String resolveEnv() {
        String[] activeProfiles = environment.getActiveProfiles(); // 활성 Spring 프로필
        if (activeProfiles.length > 0) {
            return String.join(",", activeProfiles);
        }
        return environment.getProperty("spring.profiles.active", "default");
    }
}
