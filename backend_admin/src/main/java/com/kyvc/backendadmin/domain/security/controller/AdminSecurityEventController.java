package com.kyvc.backendadmin.domain.security.controller;

import com.kyvc.backendadmin.domain.security.application.AdminSecurityEventQueryService;
import com.kyvc.backendadmin.domain.security.dto.AdminSecurityDtos;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 보안 이벤트 및 민감정보 접근 로그 API입니다. */
@Tag(name = "Backend Admin Security Event", description = "보안 이벤트와 민감정보 접근 로그 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend")
public class AdminSecurityEventController {
    private final AdminSecurityEventQueryService service;

    @Operation(summary = "보안 이벤트 조회", description = "로그인 실패, MFA 실패, 권한 거부, API Key 폐기, Verifier 중지 등 보안 이벤트를 조회한다.")
    @GetMapping("/security-events")
    public CommonResponse<AdminSecurityDtos.PageResponse> securityEvents(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return CommonResponseFactory.success(service.searchSecurityEvents(page, size));
    }

    @Operation(summary = "민감정보 접근 로그 조회", description = "audit_logs에서 민감정보 접근으로 분류되는 행위를 조회하며 원문 민감정보는 응답하지 않는다.")
    @GetMapping("/data-access-logs")
    public CommonResponse<AdminSecurityDtos.PageResponse> dataAccessLogs(@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return CommonResponseFactory.success(service.searchDataAccessLogs(page, size));
    }
}
