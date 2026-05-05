package com.kyvc.backend.domain.notification.controller;

import com.kyvc.backend.domain.notification.application.NotificationService;
import com.kyvc.backend.domain.notification.dto.NotificationPageResponse;
import com.kyvc.backend.domain.notification.dto.NotificationResponse;
import com.kyvc.backend.domain.notification.dto.NotificationUnreadCountResponse;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import com.kyvc.backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 알림 API Controller
 */
@RestController
@RequestMapping("/api/common/notifications")
@RequiredArgsConstructor
@Tag(name = "사용자 알림", description = "사용자 알림 조회 및 읽음 처리 API")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 페이지 응답
     */
    @Operation(
            summary = "알림 목록 조회",
            description = "로그인 사용자의 알림 목록을 페이지 단위로 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "알림 페이지 반환",
            content = @Content(schema = @Schema(implementation = NotificationPageResponse.class))
    )
    @GetMapping
    public ResponseEntity<CommonResponse<NotificationPageResponse>> getNotifications(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page, // 페이지 번호
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size // 페이지 크기
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                notificationService.getNotifications(getAuthenticatedUserId(userDetails), page, size)
        ));
    }

    /**
     * 읽지 않은 알림 수 조회
     *
     * @param userDetails 인증 사용자 정보
     * @return 읽지 않은 알림 수 응답
     */
    @Operation(
            summary = "읽지 않은 알림 수 조회",
            description = "로그인 사용자의 읽지 않은 알림 수를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "읽지 않은 알림 수 반환",
            content = @Content(schema = @Schema(implementation = NotificationUnreadCountResponse.class))
    )
    @GetMapping("/unread-count")
    public ResponseEntity<CommonResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                notificationService.getUnreadCount(getAuthenticatedUserId(userDetails))
        ));
    }

    /**
     * 알림 읽음 처리
     *
     * @param userDetails 인증 사용자 정보
     * @param notificationId 알림 ID
     * @return 알림 응답
     */
    @Operation(
            summary = "알림 읽음 처리",
            description = "로그인 사용자의 단건 알림을 읽음 상태로 처리합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "읽음 처리된 알림 반환",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class))
    )
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CommonResponse<NotificationResponse>> markAsRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "알림 ID", example = "1")
            @PathVariable Long notificationId // 알림 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                notificationService.markAsRead(getAuthenticatedUserId(userDetails), notificationId)
        ));
    }

    /**
     * 알림 전체 읽음 처리
     *
     * @param userDetails 인증 사용자 정보
     * @return 읽지 않은 알림 수 응답
     */
    @Operation(
            summary = "알림 전체 읽음 처리",
            description = "로그인 사용자의 읽지 않은 알림 전체를 읽음 상태로 처리합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "남은 읽지 않은 알림 수 반환",
            content = @Content(schema = @Schema(implementation = NotificationUnreadCountResponse.class))
    )
    @PatchMapping("/read-all")
    public ResponseEntity<CommonResponse<NotificationUnreadCountResponse>> markAllAsRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                notificationService.markAllAsRead(getAuthenticatedUserId(userDetails))
        ));
    }

    // 인증 사용자 ID 조회
    private Long getAuthenticatedUserId(
            CustomUserDetails userDetails // 인증 사용자 정보
    ) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return userDetails.getUserId();
    }
}
