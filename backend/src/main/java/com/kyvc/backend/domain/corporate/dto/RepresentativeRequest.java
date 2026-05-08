package com.kyvc.backend.domain.corporate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 대표자 정보 저장 요청
 *
 * @param name 대표자명
 * @param birthDate 생년월일
 * @param nationalityCode 대표자 국적 코드
 * @param phoneNumber 대표자 연락처
 * @param email 대표자 이메일
 * @param identityFile 신분증 사본 파일
 */
@Schema(description = "대표자 정보 저장 요청")
public record RepresentativeRequest(
        @Schema(description = "대표자명", example = "홍길동")
        String name, // 대표자명
        @Schema(description = "생년월일", example = "1980-01-01")
        LocalDate birthDate, // 생년월일
        @Schema(description = "대표자 국적 코드", example = "KR")
        @NotBlank(message = "국적은 필수입니다.")
        @Size(max = 30, message = "국적 코드는 30자 이하여야 합니다.")
        String nationalityCode, // 대표자 국적 코드
        @Schema(description = "대표자 연락처", example = "010-1234-5678")
        String phoneNumber, // 대표자 연락처
        @Schema(description = "대표자 이메일", example = "representative@kyvc.local")
        @Email(message = "대표자 이메일은 올바른 이메일 형식이어야 합니다.")
        String email, // 대표자 이메일
        @Schema(description = "신분증 사본 파일", type = "string", format = "binary")
        MultipartFile identityFile // 신분증 사본 파일
) {
}
