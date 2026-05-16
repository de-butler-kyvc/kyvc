package com.kyvc.backend.domain.corporate.application;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Locale;

// 회사 유형 코드 정규화 유틸
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CorporateTypeCodeNormalizer {

    private static final String CORPORATION = "CORPORATION"; // 표준 주식회사 코드
    private static final String JOINT_STOCK_COMPANY = "JOINT_STOCK_COMPANY"; // 주식회사 alias 코드

    public static String normalize(
            String corporateTypeCode // 원본 회사 유형 코드
    ) {
        if (!StringUtils.hasText(corporateTypeCode)) {
            return null;
        }
        String normalized = corporateTypeCode.trim().toUpperCase(Locale.ROOT); // 정규화 회사 유형 코드
        return JOINT_STOCK_COMPANY.equals(normalized) ? CORPORATION : normalized;
    }
}
