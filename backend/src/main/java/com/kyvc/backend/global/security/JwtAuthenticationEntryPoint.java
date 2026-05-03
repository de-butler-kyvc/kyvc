package com.kyvc.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponseFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증 실패 응답 처리기
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            AuthenticationException authenticationException // 인증 예외
    ) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED; // 기본 인증 실패 코드
        Throwable cause = authenticationException.getCause(); // 원인 예외
        if (cause instanceof ApiException apiException) {
            writeErrorResponse(response, apiException.getErrorCode(), apiException.getMessage());
            return;
        }

        writeErrorResponse(response, errorCode, errorCode.getMessage());
    }

    // 인증 실패 공통 응답 작성
    public void writeErrorResponse(
            HttpServletResponse response, // 응답 정보
            ErrorCode errorCode, // 응답 에러 코드
            String message // 응답 메시지
    ) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                CommonResponseFactory.fail(errorCode, message, null)
        );
    }
}
