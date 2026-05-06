package com.kyvc.backend.domain.core.controller;

import com.kyvc.backend.domain.core.dto.CoreHealthResponse;
import com.kyvc.backend.domain.core.infrastructure.CoreAdapter;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 헬스 체크 API Controller
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "내부 헬스", description = "내부 상태 점검 API")
public class CoreInternalHealthController {

    private final CoreAdapter coreAdapter;

    /**
     * Backend 내부 헬스 체크
     *
     * @return Backend 내부 헬스 체크 응답
     */
    @Operation(
            summary = "Backend 내부 헬스 체크",
            description = "Backend 서비스 상태 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Backend 내부 헬스 체크 응답 반환",
            content = @Content(schema = @Schema(implementation = InternalHealthResponse.class))
    )
    @GetMapping("/health")
    public ResponseEntity<CommonResponse<InternalHealthResponse>> health() {
        return ResponseEntity.ok(CommonResponseFactory.success(
                new InternalHealthResponse("backend", "UP")
        ));
    }

    /**
     * Core 내부 헬스 체크
     *
     * @return Core 내부 헬스 체크 응답
     */
    @Operation(
            summary = "Core 내부 헬스 체크",
            description = "Core 어댑터 상태 조회"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Core 내부 헬스 체크 응답 반환",
            content = @Content(schema = @Schema(implementation = CoreHealthResponse.class))
    )
    @GetMapping("/core/health")
    public ResponseEntity<CommonResponse<CoreHealthResponse>> coreHealth() {
        return ResponseEntity.ok(CommonResponseFactory.success(
                coreAdapter.checkHealth()
        ));
    }

    @Schema(description = "내부 헬스 체크 응답")
    private record InternalHealthResponse(
            @Schema(description = "서비스 이름", example = "backend")
            String service, // 서비스 이름
            @Schema(description = "서비스 상태", example = "UP")
            String status // 서비스 상태
    ) {
    }
}
