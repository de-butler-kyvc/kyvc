package com.kyvc.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.core.config.CoreInternalProperties;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponseFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 내부 API Key 검증 필터
@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_CORE_PATH_PREFIX = "/api/internal/core";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final CoreInternalProperties coreInternalProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request // 요청 정보
    ) {
        String requestPath = request.getRequestURI(); // 요청 경로
        return !(INTERNAL_CORE_PATH_PREFIX.equals(requestPath)
                || requestPath.startsWith(INTERNAL_CORE_PATH_PREFIX + "/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            FilterChain filterChain // 다음 필터 체인
    ) throws ServletException, IOException {
        String expectedInternalApiKey = coreInternalProperties.getInternalApiKey(); // 설정 API Key
        String requestInternalApiKey = request.getHeader(INTERNAL_API_KEY_HEADER); // 요청 API Key

        if (!StringUtils.hasText(expectedInternalApiKey)
                || !StringUtils.hasText(requestInternalApiKey)
                || !expectedInternalApiKey.equals(requestInternalApiKey)) {
            writeUnauthorizedResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    // 내부 API Key 인증 실패 응답 작성
    private void writeUnauthorizedResponse(
            HttpServletResponse response // 응답 정보
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED; // 인증 실패 코드
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                CommonResponseFactory.fail(errorCode)
        );
    }
}
