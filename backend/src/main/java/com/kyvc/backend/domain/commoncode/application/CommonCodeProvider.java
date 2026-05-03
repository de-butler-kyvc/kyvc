package com.kyvc.backend.domain.commoncode.application;

import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// 공통코드 내부 사용 전용 프로바이더
@Component
@RequiredArgsConstructor
public class CommonCodeProvider {

    private final CommonCodeService commonCodeService;

    // 특정 그룹의 활성 코드 목록 조회
    public List<CommonCodeItem> getEnabledCodes(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeService.getEnabledCodes(codeGroup);
    }

    // 특정 그룹의 활성 코드 존재 여부 확인
    public boolean existsEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        return commonCodeService.existsEnabledCode(codeGroup, code);
    }

    // 특정 그룹의 활성 코드 유효성 검증
    public void validateEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        commonCodeService.validateEnabledCode(codeGroup, code);
    }

    // 특정 그룹의 활성 코드 단건 조회
    public CommonCodeItem getEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        return commonCodeService.getEnabledCode(codeGroup, code);
    }
}
