package com.kyvc.backend.domain.commoncode.application;

import com.kyvc.backend.domain.commoncode.domain.CommonCode;
import com.kyvc.backend.domain.commoncode.domain.CommonCodeGroup;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeBatchResponse;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeGroupListResponse;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeGroupResponse;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeItemResponse;
import com.kyvc.backend.domain.commoncode.dto.CommonCodeResponse;
import com.kyvc.backend.domain.commoncode.repository.CommonCodeGroupRepository;
import com.kyvc.backend.domain.commoncode.repository.CommonCodeQueryRepository;
import com.kyvc.backend.domain.commoncode.repository.CommonCodeRepository;
import com.kyvc.backend.global.exception.ApiException;
import com.kyvc.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;

// 공통코드 조회 및 검증 서비스
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

    // 특정 그룹의 코드 존재 여부 확인
    public boolean existsCode(
            String codeGroup, // 공통코드 그룹 코드
            String code // 공통코드 값
    ) {
        getEnabledCodeGroup(codeGroup);
        return commonCodeRepository.findByCodeGroupAndCode(codeGroup, code).isPresent();
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

    // 공통코드 배치 조회
    public CommonCodeBatchResponse getCodes(
            String codeGroups, // 공통코드 그룹 코드 목록
            boolean enabledOnly, // 사용 여부 필터
            boolean includeMetadata // 메타데이터 포함 여부
    ) {
        List<String> parsedCodeGroups = parseCodeGroups(codeGroups);
        List<CommonCodeResponse> groups = parsedCodeGroups.stream()
                .map(codeGroup -> getCodeGroupCodes(codeGroup, enabledOnly, includeMetadata))
                .toList();
        return new CommonCodeBatchResponse(groups);
    }

    // 공통코드 그룹 목록 조회
    public CommonCodeGroupListResponse getCodeGroups(
            boolean enabledOnly // 사용 여부 필터
    ) {
        List<CommonCodeGroup> codeGroups = enabledOnly
                ? commonCodeGroupRepository.findEnabledAllOrderBySortOrderAscCodeGroupAsc()
                : commonCodeGroupRepository.findAllOrderBySortOrderAscCodeGroupAsc();

        return new CommonCodeGroupListResponse(
                codeGroups.stream()
                        .map(this::toGroupResponse)
                        .toList()
        );
    }

    // 공통코드 그룹별 코드 목록 조회
    public CommonCodeResponse getCodeGroupCodes(
            String codeGroup, // 공통코드 그룹 코드
            boolean enabledOnly, // 사용 여부 필터
            boolean includeMetadata // 메타데이터 포함 여부
    ) {
        CommonCodeGroup group = findCodeGroup(codeGroup, enabledOnly);
        List<CommonCode> codes = enabledOnly
                ? commonCodeRepository.findEnabledByCodeGroup(group.getCodeGroup())
                : commonCodeRepository.findByCodeGroup(group.getCodeGroup());

        return new CommonCodeResponse(
                group.getCodeGroup(),
                group.getGroupName(),
                codes.stream()
                        .map(code -> toItemResponse(code, includeMetadata))
                        .toList()
        );
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

    // 조회 대상 공통코드 그룹 조회
    private CommonCodeGroup findCodeGroup(
            String codeGroup, // 공통코드 그룹 코드
            boolean enabledOnly // 사용 여부 필터
    ) {
        validateCodeGroup(codeGroup);

        if (enabledOnly) {
            return commonCodeGroupRepository.findEnabledByCodeGroup(codeGroup.trim())
                    .orElseThrow(() -> new ApiException(ErrorCode.COMMON_CODE_GROUP_NOT_FOUND));
        }
        return commonCodeGroupRepository.findByCodeGroup(codeGroup.trim())
                .orElseThrow(() -> new ApiException(ErrorCode.COMMON_CODE_GROUP_NOT_FOUND));
    }

    // 공통코드 그룹 코드 목록 파싱
    private List<String> parseCodeGroups(
            String codeGroups // 공통코드 그룹 코드 목록
    ) {
        if (!StringUtils.hasText(codeGroups)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        LinkedHashSet<String> parsedCodeGroups = new LinkedHashSet<>();
        for (String codeGroup : codeGroups.split(",")) {
            if (StringUtils.hasText(codeGroup)) {
                parsedCodeGroups.add(codeGroup.trim());
            }
        }

        if (parsedCodeGroups.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        return List.copyOf(parsedCodeGroups);
    }

    // 공통코드 그룹 코드 검증
    private void validateCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        if (!StringUtils.hasText(codeGroup)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
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

    // 공통코드 Entity DTO 변환
    private CommonCodeItem toItem(
            CommonCode commonCode // 공통코드 Entity
    ) {
        return new CommonCodeItem(
                commonCode.getCodeGroup().getCodeGroup(),
                commonCode.getCode(),
                commonCode.getCodeName(),
                commonCode.getDescription(),
                commonCode.getSortOrder()
        );
    }

    // 공통코드 그룹 응답 변환
    private CommonCodeGroupResponse toGroupResponse(
            CommonCodeGroup codeGroup // 공통코드 그룹 Entity
    ) {
        return new CommonCodeGroupResponse(
                codeGroup.getId(),
                codeGroup.getCodeGroup(),
                codeGroup.getGroupName(),
                codeGroup.getDescription(),
                codeGroup.getSortOrder(),
                toYn(codeGroup.isEnabled()),
                toYn(codeGroup.isSystem())
        );
    }

    // 공통코드 항목 응답 변환
    private CommonCodeItemResponse toItemResponse(
            CommonCode commonCode, // 공통코드 Entity
            boolean includeMetadata // 메타데이터 포함 여부
    ) {
        return new CommonCodeItemResponse(
                commonCode.getId(),
                commonCode.getCode(),
                commonCode.getCodeName(),
                commonCode.getDescription(),
                commonCode.getSortOrder(),
                toYn(commonCode.isEnabled()),
                toYn(commonCode.isSystem()),
                includeMetadata ? commonCode.getMetadataJson() : null
        );
    }

    // Y/N 문자열 변환
    private String toYn(
            boolean value // boolean 값
    ) {
        return value ? "Y" : "N";
    }
}
