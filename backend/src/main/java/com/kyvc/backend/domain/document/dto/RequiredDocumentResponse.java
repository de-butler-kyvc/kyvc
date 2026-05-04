package com.kyvc.backend.domain.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 필수서류 안내 응답
 *
 * @param documentTypeCode 문서 유형 코드
 * @param documentTypeName 문서 유형 표시명
 * @param required 필수 여부
 * @param uploaded 업로드 여부
 * @param description 제출 안내 문구
 * @param allowedExtensions 허용 확장자 목록
 * @param maxFileSizeMb 최대 파일 크기 MB
 */
@Schema(description = "필수서류 안내 응답")
public record RequiredDocumentResponse(
        @Schema(description = "문서 유형 코드", example = "BUSINESS_REGISTRATION")
        String documentTypeCode, // 문서 유형 코드
        @Schema(description = "문서 유형 표시명", example = "사업자등록증")
        String documentTypeName, // 문서 유형 표시명
        @Schema(description = "필수 여부", example = "true")
        boolean required, // 필수 여부
        @Schema(description = "업로드 여부", example = "false")
        boolean uploaded, // 업로드 여부
        @Schema(description = "제출 안내 문구", example = "사업자등록증을 업로드한다.")
        String description, // 제출 안내 문구
        @Schema(description = "허용 확장자 목록", example = "[\"pdf\",\"jpg\",\"jpeg\",\"png\"]")
        List<String> allowedExtensions, // 허용 확장자 목록
        @Schema(description = "최대 파일 크기 MB", example = "10")
        int maxFileSizeMb // 최대 파일 크기 MB
) {

    public RequiredDocumentResponse {
        allowedExtensions = allowedExtensions == null ? List.of() : List.copyOf(allowedExtensions);
    }
}
