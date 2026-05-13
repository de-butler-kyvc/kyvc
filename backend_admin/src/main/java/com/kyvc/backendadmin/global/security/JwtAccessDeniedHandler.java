package com.kyvc.backendadmin.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.logging.LogEventLogger;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

// 인가 실패 응답 처리기
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final LogEventLogger logEventLogger;

    @Override
    public void handle(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            AccessDeniedException accessDeniedException // 인가 예외
    ) throws IOException, ServletException {
        logAccessDenied(request);
        response.setStatus(ErrorCode.FORBIDDEN.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), CommonResponseFactory.fail(ErrorCode.FORBIDDEN));
    }

    private void logAccessDenied(HttpServletRequest request // 요청 정보
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("path", request.getRequestURI());
        fields.put("method", request.getMethod());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            fields.put("adminId", userDetails.getUserId());
            fields.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());
        }

        logEventLogger.warn("security.access.denied", ErrorCode.FORBIDDEN.getMessage(), fields);
    }
}
