package com.kyvc.backend.domain.commoncode.application;

import com.kyvc.backend.domain.commoncode.domain.CommonCode;
import com.kyvc.backend.domain.commoncode.domain.CommonCodeGroup;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;
import com.kyvc.backend.domain.commoncode.repository.CommonCodeGroupRepository;
import com.kyvc.backend.domain.commoncode.repository.CommonCodeQueryRepository;
import com.kyvc.backend.domain.commoncode.repository.CommonCodeRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 공통코드 내부 조회 및 검증 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommonCodeService {

    private final CommonCodeGroupRepository commonCodeGroupRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final CommonCodeQueryRepository commonCodeQueryRepository;

    // 공통코드 그룹 조회
    public CommonCodeGroup getCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeGroupRepository.findByCodeGroup(codeGroup)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.COMMON_CODE_GROUP_NOT_FOUND,
                        codeGroup + " 공통 코드 그룹을 찾을 수 없습니다."
                ));
    }

    // 특정 그룹의 활성 코드 목록 조회
    public List<CommonCodeItem> getEnabledCodes(
            String codeGroup // 공통코드 그룹 코드
    ) {
        getEnabledCodeGroup(codeGroup);
        return commonCodeQueryRepository.findEnabledCodeItemsByCodeGroup(codeGroup);
    }

    // 특정 그룹의 활성 코드 존재 여부 확인
    public boolean existsEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        getEnabledCodeGroup(codeGroup);
        return commonCodeRepository.existsEnabledCode(codeGroup, code);
    }

    // 특정 그룹의 활성 코드 유효성 검증
    public void validateEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        getEnabledCodeGroup(codeGroup);

        if (!commonCodeRepository.existsEnabledCode(codeGroup, code)) {
            throw commonCodeNotFound(codeGroup, code);
        }
    }

    // 특정 그룹의 활성 코드 단건 조회
    public CommonCodeItem getEnabledCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        getEnabledCodeGroup(codeGroup);

        CommonCode commonCode = commonCodeRepository.findEnabledByCodeGroupAndCode(codeGroup, code)
                .orElseThrow(() -> commonCodeNotFound(codeGroup, code));

        return toItem(commonCode);
    }

    // 사용 가능한 공통코드 그룹 조회
    private CommonCodeGroup getEnabledCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return commonCodeGroupRepository.findEnabledByCodeGroup(codeGroup)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.COMMON_CODE_GROUP_NOT_FOUND,
                        codeGroup + " 공통 코드 그룹을 찾을 수 없습니다."
                ));
    }

    // 공통코드 조회 실패 예외 생성
    private ApiException commonCodeNotFound(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        return new ApiException(
                ErrorCode.COMMON_CODE_NOT_FOUND,
                codeGroup + ":" + code + " 공통 코드를 찾을 수 없습니다."
        );
    }

    // 공통코드 엔티티 DTO 변환
    private CommonCodeItem toItem(
            CommonCode commonCode // 공통코드 엔티티
    ) {
        return new CommonCodeItem(
                commonCode.getCodeGroup().getCodeGroup(),
                commonCode.getCode(),
                commonCode.getCodeName(),
                commonCode.getDescription(),
                commonCode.getSortOrder()
        );
    }
}
