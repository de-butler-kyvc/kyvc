package com.kyvc.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyvc.backend.domain.verifier.application.VerifierApiKeyAuthService;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponseFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Verifier API Key 인증 필터
@Component
@RequiredArgsConstructor
public class VerifierApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String VERIFIER_PATH_PREFIX = "/api/verifier";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final VerifierApiKeyAuthService verifierApiKeyAuthService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request // 요청 정보
    ) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String requestPath = request.getRequestURI(); // 요청 경로
        return !VERIFIER_PATH_PREFIX.equals(requestPath)
                && !requestPath.startsWith(VERIFIER_PATH_PREFIX + "/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, // 요청 정보
            HttpServletResponse response, // 응답 정보
            FilterChain filterChain // 다음 필터 체인
    ) throws ServletException, IOException {
        try {
            VerifierPrincipal principal = verifierApiKeyAuthService.authenticate(request.getHeader(API_KEY_HEADER));
            VerifierAuthenticationToken authenticationToken = new VerifierAuthenticationToken(principal);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, exception.getErrorCode());
        }
    }

    // Verifier API Key 인증 실패 응답 작성
    private void writeUnauthorizedResponse(
            HttpServletResponse response, // 응답 정보
            ErrorCode errorCode // 오류 코드
    ) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                CommonResponseFactory.fail(errorCode)
        );
    }
}
