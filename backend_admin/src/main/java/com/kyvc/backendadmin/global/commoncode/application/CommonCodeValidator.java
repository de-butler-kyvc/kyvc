package com.kyvc.backendadmin.global.commoncode.application;

import com.kyvc.backendadmin.global.commoncode.repository.CommonCodeRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import com.kyvc.backendadmin.global.util.KyvcEnums;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 업무 요청값의 공통코드 유효성 검증을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class CommonCodeValidator {

    private final CommonCodeRepository commonCodeRepository;

    /**
     * 공통코드 그룹과 코드값이 활성 공통코드인지 검증합니다.
     *
     * <p>검증 기준은 1단계 code_group 존재, 2단계 code_group enabled_yn=Y,
     * 3단계 code 존재, 4단계 common_codes enabled_yn=Y입니다. 네 단계 중 하나라도 만족하지 않으면
     * COMMON_CODE_NOT_FOUND 예외를 발생시켜 업무 도메인이 유효하지 않은 요청값을 처리하지 않도록 합니다.</p>
     *
     * @param codeGroup 공통코드 그룹
     * @param code 검증할 코드값
     */
    public void validateEnabledCode(String codeGroup, String code) {
        // enabledYn 검증: 그룹과 코드가 모두 활성 상태인 경우에만 업무 요청값으로 허용한다.
        if (!commonCodeRepository.existsEnabledCode(codeGroup, code)) {
            throw new ApiException(ErrorCode.COMMON_CODE_NOT_FOUND);
        }
    }

    /**
     * 공통코드 그룹 enum과 코드값이 활성 공통코드인지 검증합니다.
     *
     * <p>검증 기준은 1단계 code_group 존재, 2단계 code_group enabled_yn=Y,
     * 3단계 code 존재, 4단계 common_codes enabled_yn=Y입니다. enum 이름을 code_group 값으로 사용합니다.</p>
     *
     * @param codeGroup 공통코드 그룹 enum
     * @param code 검증할 코드값
     */
    public void validateEnabledCode(KyvcEnums.CommonCodeGroup codeGroup, String code) {
        validateEnabledCode(codeGroup.name(), code);
    }
}
