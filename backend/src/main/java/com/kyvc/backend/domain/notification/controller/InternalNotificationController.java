package com.kyvc.backend.domain.notification.controller;

import com.kyvc.backend.domain.notification.application.NotificationService;
import com.kyvc.backend.domain.notification.dto.InternalNotificationSendRequest;
import com.kyvc.backend.domain.notification.dto.InternalNotificationSendResponse;
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
 * 내부 알림 발송 API Controller
 */
@RestController
@RequestMapping("/api/internal/notifications")
@RequiredArgsConstructor
@Tag(name = "내부 알림", description = "내부 알림 발송 API")
public class InternalNotificationController {

    private final NotificationService notificationService;

    /**
     * 내부 알림 발송
     *
     * @param request 내부 알림 발송 요청
     * @return 내부 알림 발송 응답
     */
    @Operation(
            summary = "내부 알림 발송",
            description = "내부 시스템 요청 기준 사용자 알림 저장 처리"
    )
    @ApiResponse(
            responseCode = "200",
            description = "내부 알림 발송 응답 반환",
            content = @Content(schema = @Schema(implementation = InternalNotificationSendResponse.class))
    )
    @PostMapping("/send")
    public ResponseEntity<CommonResponse<InternalNotificationSendResponse>> send(
            @Valid @RequestBody InternalNotificationSendRequest request // 내부 알림 발송 요청
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                notificationService.sendInternalNotification(request)
        ));
    }
}
