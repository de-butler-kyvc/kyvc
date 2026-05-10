package com.kyvc.backend.domain.did.controller;

import com.kyvc.backend.domain.did.application.DidInstitutionService;
import com.kyvc.backend.domain.did.dto.DidInstitutionResponse;
import com.kyvc.backend.global.response.CommonResponse;
import com.kyvc.backend.global.response.CommonResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DID 기관명 API Controller
 */
@RestController
@RequestMapping("/api/common/dids")
@RequiredArgsConstructor
@Tag(name = "Common DID", description = "DID 기관 매핑 조회 API")
public class DidInstitutionController {

    private final DidInstitutionService didInstitutionService;

    /**
     * DID 기관명을 조회합니다.
     *
     * @param did DID
     * @return DID 기관명 조회 응답
     */
    @Operation(
            summary = "DID 기관명 조회",
            description = "DID에 매핑된 기관명을 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 DID 기관 매핑 정보를 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "DID 기관명 조회 성공",
            content = @Content(schema = @Schema(implementation = DidInstitutionResponse.class))
    )
    @GetMapping("/{did}/institution")
    public CommonResponse<DidInstitutionResponse> getInstitution(
            @Parameter(description = "DID", required = true)
            @PathVariable String did // DID
    ) {
        return CommonResponseFactory.success(didInstitutionService.getInstitution(did));
    }
}
