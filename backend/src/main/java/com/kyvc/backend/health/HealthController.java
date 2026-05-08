package com.kyvc.backend.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@Tag(name = "Core 연동 / Callback", description = "Core 내부 헬스체크, Issuer 정책, AI/VC/XRPL/VP callback 및 개발용 E2E API")
public class HealthController {

    @Operation(
            summary = "서비스 상태 확인",
            description = "백엔드 서비스가 정상 실행 중인지 확인합니다. 입력값은 없습니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "서비스 상태와 서비스 이름 반환",
            content = @Content(schema = @Schema(implementation = HealthResponse.class))
    )
    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "backend");
    }

    @Schema(description = "서비스 상태 응답")
    public record HealthResponse(
            @Schema(description = "서비스 상태", example = "UP")
            String status, // 서비스 상태
            @Schema(description = "서비스 이름", example = "backend")
            String service // 서비스 이름
    ) {
    }
}
