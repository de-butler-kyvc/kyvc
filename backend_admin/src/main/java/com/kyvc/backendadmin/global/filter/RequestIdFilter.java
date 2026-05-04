package com.kyvc.backendadmin.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// 요청 추적 ID 필터
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            FilterChain filterChain // 다음 필터 체인
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request); // MDC 및 응답 헤더 추적값

        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    // 요청 헤더 기반 requestId 결정
    private String resolveRequestId(HttpServletRequest request // 요청 정보
    ) {
        String requestId = request.getHeader(REQUEST_ID_HEADER); // 외부 전달 추적 ID
        if (!StringUtils.hasText(requestId) || hasLineBreak(requestId)) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }

    // 응답 헤더 주입 차단 문자 검증
    private boolean hasLineBreak(String value // 검사 대상 문자열
    ) {
        return value.contains("\r") || value.contains("\n");
    }
}