package com.kyvc.backend.domain.audit.controller;

import com.kyvc.backend.domain.audit.application.AuditLogService;
import com.kyvc.backend.domain.audit.dto.InternalAuditLogRequest;
import com.kyvc.backend.domain.audit.dto.InternalAuditLogResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 감사로그 기록 API Controller
 */
@RestController
@RequestMapping("/api/internal/audit-logs")
@RequiredArgsConstructor
@Tag(name = "내부 감사로그", description = "내부 감사로그 기록 API")
public class InternalAuditLogController {

    private final AuditLogService auditLogService;

    /**
     * 내부 감사로그 기록
     *
     * @param request 내부 감사로그 기록 요청
     * @return 내부 감사로그 기록 응답
     */
    @Operation(
            summary = "내부 감사로그 기록",
            description = "내부 시스템 요청 기준 감사로그 저장 처리"
    )
    @ApiResponse(
            responseCode = "200",
            description = "내부 감사로그 기록 응답 반환",
            content = @Content(schema = @Schema(implementation = InternalAuditLogResponse.class))
    )
    @PostMapping
    public ResponseEntity<CommonResponse<InternalAuditLogResponse>> create(
            @Valid @RequestBody InternalAuditLogRequest request // 내부 감사로그 기록 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                auditLogService.createInternalAuditLog(request)
        ));
    }
}
