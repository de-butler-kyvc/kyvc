package com.kyvc.backendadmin.domain.notification.controller;

import com.kyvc.backendadmin.domain.notification.application.AdminNotificationTemplateService;
import com.kyvc.backendadmin.domain.notification.dto.AdminNotificationTemplateDtos;
import com.kyvc.backendadmin.global.response.CommonResponse;
import com.kyvc.backendadmin.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 템플릿 관리자 API입니다.
 */
@Tag(name = "Backend Admin Notification Template", description = "알림 템플릿 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/backend/notification-templates")
public class AdminNotificationTemplateController {
    private final AdminNotificationTemplateService service;

    @Operation(summary = "알림 템플릿 목록 조회", description = "채널, 사용 여부, 키워드 조건으로 알림 템플릿을 조회한다.")
    @GetMapping
    public CommonResponse<AdminNotificationTemplateDtos.PageResponse> search(@RequestParam(required = false) String channelCode, @RequestParam(required = false) String enabledYn, @RequestParam(required = false) String keyword, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return CommonResponseFactory.success(service.search(channelCode, enabledYn, keyword, page, size));
    }

    @Operation(summary = "알림 템플릿 등록", description = "템플릿 코드를 중복 검증하고 알림 템플릿을 등록한다.")
    @PostMapping
    public CommonResponse<AdminNotificationTemplateDtos.Response> create(@Valid @RequestBody AdminNotificationTemplateDtos.CreateRequest request) {
        return CommonResponseFactory.success(service.create(request));
    }

    @Operation(summary = "알림 템플릿 상세 조회", description = "템플릿 ID 기준으로 상세를 조회한다.")
    @GetMapping("/{templateId}")
    public CommonResponse<AdminNotificationTemplateDtos.Response> get(@PathVariable Long templateId) {
        return CommonResponseFactory.success(service.get(templateId));
    }

    @Operation(summary = "알림 템플릿 수정", description = "템플릿명, 본문, 제목, 사용 여부 등을 수정한다.")
    @PatchMapping("/{templateId}")
    public CommonResponse<AdminNotificationTemplateDtos.Response> update(@PathVariable Long templateId, @RequestBody AdminNotificationTemplateDtos.UpdateRequest request) {
        return CommonResponseFactory.success(service.update(templateId, request));
    }

    @Operation(summary = "알림 템플릿 삭제", description = "물리 삭제 대신 enabledYn=N으로 비활성화한다.")
    @DeleteMapping("/{templateId}")
    public CommonResponse<Void> delete(@PathVariable Long templateId) {
        service.delete(templateId);
        return CommonResponseFactory.successWithoutData();
    }
}
