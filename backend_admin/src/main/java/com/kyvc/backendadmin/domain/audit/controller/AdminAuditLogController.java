package com.kyvc.backendadmin.domain.audit.controller;

import com.kyvc.backendadmin.domain.audit.application.AdminAuditLogQueryService;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogListResponse;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogResponse;
import com.kyvc.backendadmin.domain.audit.dto.AdminAuditLogSearchRequest;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 백엔드 관리자 감사로그 조회 API를 담당합니다.
 */
@Tag(name = "Audit Log Admin", description = "백엔드 관리자 감사로그 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/audit-logs")
public class AdminAuditLogController {

    private final AdminAuditLogQueryService adminAuditLogQueryService;

    /**
     * 감사로그 목록을 조회합니다.
     *
     * <p>page는 0부터 시작하는 페이지 번호, size는 페이지 크기입니다.
     * actorType은 행위자 유형, actorId는 행위자 ID, actionType은 작업 유형,
     * targetType은 감사 대상 유형, targetId는 감사 대상 ID입니다.
     * from과 to는 감사로그 생성일시 검색 범위를 의미합니다.</p>
     *
     * @return 감사로그 목록 응답
     */
    @Operation(
            summary = "감사로그 목록 조회",
            description = "관리자 권한이 필요한 감사로그 목록 조회 API입니다. 응답 data.items에는 목록, data.page에는 페이지 정보가 포함됩니다."
    )
    @GetMapping
    public CommonResponse<AdminAuditLogListResponse> search(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "행위자 유형", example = "ADMIN")
            @RequestParam(required = false) String actorType,
            @Parameter(description = "행위자 ID", example = "1")
            @RequestParam(required = false) Long actorId,
            @Parameter(description = "작업 유형", example = "COMMON_CODE_CREATE")
            @RequestParam(required = false) String actionType,
            @Parameter(description = "대상 유형", example = "COMMON_CODE")
            @RequestParam(required = false) String targetType,
            @Parameter(description = "대상 ID", example = "10")
            @RequestParam(required = false) Long targetId,
            @Parameter(description = "조회 시작 일시", example = "2026-05-01T00:00:00")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false) LocalDateTime from,
            @Parameter(description = "조회 종료 일시", example = "2026-05-31T23:59:59")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false) LocalDateTime to
    ) {
        return CommonResponseFactory.success(adminAuditLogQueryService.search(AdminAuditLogSearchRequest.of(
                page,
                size,
                actorType,
                actorId,
                actionType,
                targetType,
                targetId,
                from,
                to
        )));
    }

    /**
     * 감사로그 상세를 조회합니다.
     *
     * @param auditId 감사로그 ID
     * @return 감사로그 상세 응답
     */
    @Operation(summary = "감사로그 상세 조회", description = "관리자 권한이 필요한 감사로그 상세 조회 API입니다.")
    @ApiResponse(responseCode = "404", description = "감사로그가 없는 경우")
    @GetMapping("/{auditId}")
    public CommonResponse<AdminAuditLogResponse> get(
            @Parameter(description = "감사로그 ID", required = true)
            @PathVariable Long auditId
    ) {
        return CommonResponseFactory.success(adminAuditLogQueryService.get(auditId));
    }
}
