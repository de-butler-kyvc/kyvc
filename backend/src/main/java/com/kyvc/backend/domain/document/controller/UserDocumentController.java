package com.kyvc.backend.domain.document.controller;

import com.kyvc.backend.domain.document.application.UserDocumentService;
import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestCreateRequest;
import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestListResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentDetailResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentListResponse;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 문서함 API Controller
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "사용자 문서", description = "사용자 제출서류 목록, 상세, 삭제 요청 API")
public class UserDocumentController {

    private final UserDocumentService userDocumentService;

    /**
     * 사용자 제출서류 목록 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param documentTypeCode 문서 유형 코드
     * @param status 문서 상태 코드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자 문서 목록 페이지 응답
     */
    @Operation(
            summary = "사용자 제출서류 목록 조회",
            description = "로그인 사용자 소유 법인의 전체 KYC 제출서류를 문서함 형태로 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 문서 목록 페이지 반환",
            content = @Content(schema = @Schema(implementation = UserDocumentListResponse.class))
    )
    @GetMapping("/documents")
    public ResponseEntity<CommonResponse<UserDocumentListResponse>> getDocuments(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
            @RequestParam(required = false) String documentTypeCode, // 문서 유형 코드
            @Parameter(description = "문서 상태 코드", example = "UPLOADED")
            @RequestParam(required = false) String status, // 문서 상태 코드
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page, // 페이지 번호
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size // 페이지 크기
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                userDocumentService.getDocuments(
                        getAuthenticatedUserId(userDetails),
                        documentTypeCode,
                        status,
                        page,
                        size
                )
        ));
    }

    /**
     * 사용자 제출서류 상세 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param documentId 문서 ID
     * @return 사용자 문서 상세 응답
     */
    @Operation(
            summary = "사용자 제출서류 상세 조회",
            description = "로그인 사용자 소유 법인의 제출서류 메타데이터와 삭제 요청 상태를 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "사용자 문서 상세 반환",
            content = @Content(schema = @Schema(implementation = UserDocumentDetailResponse.class))
    )
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<CommonResponse<UserDocumentDetailResponse>> getDocument(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "문서 ID", example = "1")
            @PathVariable Long documentId // 문서 ID
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                userDocumentService.getDocument(getAuthenticatedUserId(userDetails), documentId)
        ));
    }

    /**
     * 사용자 제출서류 삭제 요청 생성
     *
     * @param userDetails 인증 사용자 정보
     * @param documentId 문서 ID
     * @param request 삭제 요청 생성 요청
     * @return 삭제 요청 응답
     */
    @Operation(
            summary = "사용자 제출서류 삭제 요청 생성",
            description = "로그인 사용자 소유 문서의 삭제 요청을 생성합니다. 실제 파일은 삭제하지 않습니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "생성된 삭제 요청 반환",
            content = @Content(schema = @Schema(implementation = UserDocumentDeleteRequestResponse.class))
    )
    @PostMapping("/documents/{documentId}/delete-request")
    public ResponseEntity<CommonResponse<UserDocumentDeleteRequestResponse>> createDeleteRequest(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "문서 ID", example = "1")
            @PathVariable Long documentId, // 문서 ID
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "문서 삭제 요청 생성 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UserDocumentDeleteRequestCreateRequest.class))
            )
            @Valid @RequestBody UserDocumentDeleteRequestCreateRequest request // 삭제 요청 생성 요청
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponseFactory.success(
                        userDocumentService.createDeleteRequest(
                                getAuthenticatedUserId(userDetails),
                                documentId,
                                request
                        )
                ));
    }

    /**
     * 사용자 문서 삭제 요청 이력 조회
     *
     * @param userDetails 인증 사용자 정보
     * @param status 삭제 요청 상태 코드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 삭제 요청 이력 페이지 응답
     */
    @Operation(
            summary = "사용자 문서 삭제 요청 이력 조회",
            description = "로그인 사용자 소유 문서의 삭제 요청 이력을 조회합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "문서 삭제 요청 이력 페이지 반환",
            content = @Content(schema = @Schema(implementation = UserDocumentDeleteRequestListResponse.class))
    )
    @GetMapping("/document-delete-requests")
    public ResponseEntity<CommonResponse<UserDocumentDeleteRequestListResponse>> getDeleteRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails, // 인증 사용자 정보
            @Parameter(description = "삭제 요청 상태 코드", example = "REQUESTED")
            @RequestParam(required = false) String status, // 삭제 요청 상태 코드
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page, // 페이지 번호
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size // 페이지 크기
    ) {
        return ResponseEntity.ok(CommonResponseFactory.success(
                userDocumentService.getDeleteRequests(
                        getAuthenticatedUserId(userDetails),
                        status,
                        page,
                        size
                )
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
