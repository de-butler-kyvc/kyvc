package com.kyvc.backendadmin.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 템플릿 관리자 API DTO 모음입니다.
 */
public final class AdminNotificationTemplateDtos {
    private AdminNotificationTemplateDtos() {
    }

    @Schema(description = "알림 템플릿 등록 요청")
    public record CreateRequest(
            /** 템플릿 코드 */
            @NotBlank(message = "templateCode는 필수입니다.")
            @Schema(description = "템플릿 코드", example = "KYC_APPROVED")
            String templateCode,
            /** 템플릿 이름 */
            @NotBlank(message = "templateName은 필수입니다.")
            @Schema(description = "템플릿 이름", example = "KYC 승인 알림")
            String templateName,
            /** 채널 코드 */
            @NotBlank(message = "channelCode는 필수입니다.")
            @Schema(description = "채널 코드", example = "EMAIL")
            String channelCode,
            /** 제목 템플릿 */
            @Schema(description = "제목 템플릿", example = "KYC 심사가 승인되었습니다.")
            String titleTemplate,
            /** 본문 템플릿 */
            @NotBlank(message = "messageTemplate은 필수입니다.")
            @Schema(description = "본문 템플릿", example = "안녕하세요. {{corporateName}}님의 KYC가 승인되었습니다.")
            String messageTemplate,
            /** 사용 여부 */
            @Schema(description = "사용 여부", example = "Y")
            String enabledYn
    ) {
    }

    @Schema(description = "알림 템플릿 수정 요청")
    public record UpdateRequest(
            /** 템플릿 이름 */
            @Schema(description = "템플릿 이름", example = "KYC 승인 알림")
            String templateName,
            /** 채널 코드 */
            @Schema(description = "채널 코드", example = "EMAIL")
            String channelCode,
            /** 제목 템플릿 */
            @Schema(description = "제목 템플릿")
            String titleTemplate,
            /** 본문 템플릿 */
            @Schema(description = "본문 템플릿")
            String messageTemplate,
            /** 사용 여부 */
            @Schema(description = "사용 여부", example = "Y")
            String enabledYn
    ) {
    }

    @Schema(description = "알림 템플릿 응답")
    public record Response(
            /** 템플릿 ID */
            @Schema(description = "템플릿 ID", example = "1")
            Long templateId,
            /** 템플릿 코드 */
            @Schema(description = "템플릿 코드", example = "KYC_APPROVED")
            String templateCode,
            /** 템플릿 이름 */
            @Schema(description = "템플릿 이름", example = "KYC 승인 알림")
            String templateName,
            /** 채널 코드 */
            @Schema(description = "채널 코드", example = "EMAIL")
            String channelCode,
            /** 제목 템플릿 */
            @Schema(description = "제목 템플릿")
            String titleTemplate,
            /** 본문 템플릿 */
            @Schema(description = "본문 템플릿")
            String messageTemplate,
            /** 사용 여부 */
            @Schema(description = "사용 여부", example = "Y")
            String enabledYn,
            /** 생성 시각 */
            @Schema(description = "생성 시각")
            LocalDateTime createdAt,
            /** 수정 시각 */
            @Schema(description = "수정 시각")
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "알림 템플릿 목록 응답")
    public record PageResponse(
            /** 템플릿 목록 */
            @Schema(description = "템플릿 목록")
            List<Response> items,
            /** 현재 페이지 */
            @Schema(description = "현재 페이지", example = "0")
            int page,
            /** 페이지 크기 */
            @Schema(description = "페이지 크기", example = "20")
            int size,
            /** 전체 건수 */
            @Schema(description = "전체 건수", example = "100")
            long totalElements,
            /** 전체 페이지 수 */
            @Schema(description = "전체 페이지 수", example = "5")
            int totalPages
    ) {
    }
}
