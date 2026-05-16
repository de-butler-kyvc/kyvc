package com.kyvc.backend.domain.vp.controller;

import com.kyvc.backend.domain.vp.application.VpVerificationService;
import com.kyvc.backend.domain.vp.dto.EligibleCredentialListResponse;
import com.kyvc.backend.domain.vp.dto.QrResolveRequest;
import com.kyvc.backend.domain.vp.dto.QrResolveResponse;
import com.kyvc.backend.domain.vp.dto.VpAttachmentPart;
import com.kyvc.backend.domain.vp.dto.VpPresentationRequest;
import com.kyvc.backend.domain.vp.dto.VpPresentationResponse;
import com.kyvc.backend.domain.vp.dto.VpPresentationResultResponse;
import com.kyvc.backend.domain.vp.dto.VpRequestResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 모바일 VP API Controller
 */
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
@Tag(name = "VP / QR 검증", description = "QR 해석, VP 요청 조회, 제출 가능 VC 조회, VP 제출 및 검증 결과 API")
public class MobileVpController {

    private final VpVerificationService vpVerificationService;

    /**
     * QR Payload를 해석한다.
     *
     * @param request QR 해석 요청
     * @return QR 해석 응답
     */
    @Operation(summary = "모바일 QR 해석")
    @ApiResponse(
            responseCode = "200",
            description = "QR 해석 응답 반환",
            content = @Content(schema = @Schema(implementation = QrResolveResponse.class))
    )
    @PostMapping("/qr/resolve")
    public CommonResponse<QrResolveResponse> resolveQr(
            @RequestBody QrResolveRequest request // QR 해석 요청
    ) {
        return CommonResponseFactory.success(vpVerificationService.resolveQr(request));
    }

    /**
     * VP 요청을 조회한다.
     *
     * @param requestId VP 요청 ID
     * @return VP 요청 응답
     */
    @Operation(
            summary = "모바일 VP 요청 조회",
            description = "모바일 앱이 VP 요청 정보를 조회합니다. Core를 직접 호출하지 않고 backend DB에 저장된 요청 정보만 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 요청 응답 반환",
            content = @Content(schema = @Schema(implementation = VpRequestResponse.class))
    )
    @GetMapping("/vp/requests/{requestId}")
    public CommonResponse<VpRequestResponse> getVpRequest(
            @PathVariable String requestId // VP 요청 ID
    ) {
        return CommonResponseFactory.success(vpVerificationService.getVpRequest(requestId));
    }

    /**
     * 제출 가능 Credential 목록을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param requestId VP 요청 ID
     * @return 제출 가능 Credential 목록 응답
     */
    @Operation(summary = "모바일 제출 가능 Credential 조회")
    @ApiResponse(
            responseCode = "200",
            description = "제출 가능 Credential 목록 응답 반환",
            content = @Content(schema = @Schema(implementation = EligibleCredentialListResponse.class))
    )
    @GetMapping("/vp/requests/{requestId}/eligible-credentials")
    public CommonResponse<EligibleCredentialListResponse> getEligibleCredentials(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable String requestId // VP 요청 ID
    ) {
        return CommonResponseFactory.success(vpVerificationService.getEligibleCredentials(userDetails, requestId));
    }

    /**
     * VP를 제출한다.
     *
     * @param request VP 제출 요청
     * @return VP 제출 응답
     */
    @Operation(
            summary = "모바일 일반 VP 제출",
            description = """
                    일반 VP 요청에 대해 VP를 제출합니다.
                    backend가 Core에 VP 검증을 동기 요청하고 검증 결과를 저장합니다.
                    이 API는 로그인 토큰을 발급하지 않습니다.
                    이 API는 VP 로그인 API가 아니며 POST /api/mobile/auth/vp-login을 대체하지 않습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 검증 결과 반환",
            content = @Content(schema = @Schema(implementation = VpPresentationResponse.class))
    )
    @PostMapping("/vp/presentations")
    public CommonResponse<VpPresentationResponse> submitPresentation(
            @RequestBody VpPresentationRequest request // VP 제출 요청
    ) {
        return CommonResponseFactory.success(vpVerificationService.submitPresentation(request));
    }

    /**
     * 원본 첨부 포함 VP를 제출한다.
     *
     * @param request VP 제출 요청
     * @param attachmentManifestJson 원본 첨부 manifest JSON
     * @param multipartRequest multipart 요청
     * @return VP 제출 응답
     */
    @Operation(
            summary = "모바일 원본 첨부 포함 VP 제출",
            description = "SD-JWT VP와 원본 PDF 첨부를 multipart/form-data로 제출한다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 검증 결과 반환",
            content = @Content(schema = @Schema(implementation = VpPresentationResponse.class))
    )
    @PostMapping(value = "/vp/presentations/with-attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<VpPresentationResponse> submitPresentationWithAttachments(
            @RequestPart("presentation") VpPresentationRequest request, // VP 제출 요청
            @RequestPart(value = "attachmentManifest", required = false) String attachmentManifestJson, // 원본 첨부 manifest JSON
            MultipartHttpServletRequest multipartRequest // multipart 요청
    ) {
        return CommonResponseFactory.success(vpVerificationService.submitPresentationWithAttachments(
                request,
                attachmentManifestJson,
                extractAttachmentParts(multipartRequest)
        ));
    }

    /**
     * VP 제출 결과를 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param presentationId VP 제출 ID
     * @return VP 제출 결과 응답
     */
    @Operation(
            summary = "모바일 VP 제출 이력 상세 조회",
            description = """
                    JWT/Cookie로 인증된 로그인 사용자의 VP 제출 결과를 조회합니다.
                    Core를 다시 호출하지 않고 backend DB에 저장된 검증 결과만 반환합니다.
                    VP 로그인 결과 조회 API가 아닙니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "VP 제출 결과 응답 반환",
            content = @Content(schema = @Schema(implementation = VpPresentationResultResponse.class))
    )
    @GetMapping("/vp/presentations/{presentationId}")
    public CommonResponse<VpPresentationResultResponse> getPresentationResult(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @PathVariable Long presentationId // VP 제출 ID
    ) {
        return CommonResponseFactory.success(
                vpVerificationService.getPresentationResult(userDetails, presentationId)
        );
    }

    private List<VpAttachmentPart> extractAttachmentParts(
            MultipartHttpServletRequest multipartRequest // multipart 요청
    ) {
        if (multipartRequest == null || multipartRequest.getMultiFileMap().isEmpty()) {
            return List.of();
        }
        List<VpAttachmentPart> attachmentParts = new ArrayList<>();
        multipartRequest.getMultiFileMap().forEach((partName, files) -> files.forEach(file -> {
            if (file != null) {
                attachmentParts.add(toAttachmentPart(partName, file));
            }
        }));
        return attachmentParts;
    }

    private VpAttachmentPart toAttachmentPart(
            String partName, // multipart 파트명
            MultipartFile file // 첨부 파일
    ) {
        try {
            return new VpAttachmentPart(
                    partName,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getBytes()
            );
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.FILE_UPLOAD_FAILED, exception);
        }
    }
}
