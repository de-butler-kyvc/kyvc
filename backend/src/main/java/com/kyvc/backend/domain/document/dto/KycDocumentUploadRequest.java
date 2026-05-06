package com.kyvc.backend.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * KYC 문서 업로드 요청
 *
 * @param documentTypeCode 문서 유형 코드
 * @param file 업로드 파일
 */
@Schema(description = "KYC 문서 업로드 요청")
public record KycDocumentUploadRequest(
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        @NotBlank(message = "문서 유형 코드는 필수입니다.")
        String documentTypeCode, // 문서 유형 코드
        @Schema(description = "업로드 파일", type = "string", format = "binary")
        @NotNull(message = "업로드 파일은 필수입니다.")
        MultipartFile file // 업로드 파일
) {
}
