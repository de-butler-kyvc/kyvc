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
 * @param groupCode 선택 필수 그룹 코드
 * @param groupName 선택 필수 그룹 표시명
 * @param minRequiredCount 그룹 최소 제출 개수
 * @param groupCandidate 선택 필수 그룹 후보 여부
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
        int maxFileSizeMb, // 최대 파일 크기 MB
        @Schema(description = "선택 필수 그룹 코드", example = "OWNERSHIP_DOC")
        String groupCode, // 선택 필수 그룹 코드
        @Schema(description = "선택 필수 그룹 표시명", example = "소유구조 확인 문서")
        String groupName, // 선택 필수 그룹 표시명
        @Schema(description = "그룹 최소 제출 개수", example = "1")
        Integer minRequiredCount, // 그룹 최소 제출 개수
        @Schema(description = "선택 필수 그룹 후보 여부", example = "true")
        boolean groupCandidate // 선택 필수 그룹 후보 여부
) {

    public RequiredDocumentResponse {
        allowedExtensions = allowedExtensions == null ? List.of() : List.copyOf(allowedExtensions);
    }

    public RequiredDocumentResponse(
            String documentTypeCode, // 문서 유형 코드
            String documentTypeName, // 문서 유형 표시명
            boolean required, // 필수 여부
            boolean uploaded, // 업로드 여부
            String description, // 제출 안내 문구
            List<String> allowedExtensions, // 허용 확장자 목록
            int maxFileSizeMb // 최대 파일 크기 MB
    ) {
        this(
                documentTypeCode,
                documentTypeName,
                required,
                uploaded,
                description,
                allowedExtensions,
                maxFileSizeMb,
                null,
                null,
                null,
                false
        );
    }
}
