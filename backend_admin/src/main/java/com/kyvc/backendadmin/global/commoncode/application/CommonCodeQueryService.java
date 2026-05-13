package com.kyvc.backendadmin.global.commoncode.application;

import com.kyvc.backendadmin.global.commoncode.domain.CommonCode;
import com.kyvc.backendadmin.global.commoncode.domain.CommonCodeGroup;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupListResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupSearchRequest;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeListResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeResponse;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeSearchRequest;
import com.kyvc.backendadmin.global.commoncode.repository.CommonCodeRepository;
import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공통코드 조회 유스케이스를 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class CommonCodeQueryService {

    private final CommonCodeRepository commonCodeRepository;

    @Transactional(readOnly = true)
    public CommonCodeGroupListResponse searchGroups(CommonCodeGroupSearchRequest request) {
        // 코드 그룹은 조회만 가능하다는 정책에 따라 목록/상세 조회만 제공한다.
        List<CommonCodeGroupResponse> items = commonCodeRepository.searchGroups(request)
                .stream()
                .map(this::toGroupResponse)
                .toList();
        long totalElements = commonCodeRepository.countGroups(request);
        return new CommonCodeGroupListResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages(totalElements, request.size())
        );
    }

    @Transactional(readOnly = true)
    public CommonCodeGroupResponse getGroup(Long codeGroupId) {
        // 코드 그룹은 조회만 가능하다는 정책에 따라 상세 조회만 처리한다.
        return commonCodeRepository.findGroupById(codeGroupId)
                .map(this::toGroupResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMON_CODE_GROUP_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public CommonCodeListResponse searchCodes(CommonCodeSearchRequest request) {
        List<CommonCodeResponse> items = commonCodeRepository.searchCodes(request)
                .stream()
                .map(this::toCodeResponse)
                .toList();
        long totalElements = commonCodeRepository.countCodes(request);
        return new CommonCodeListResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages(totalElements, request.size())
        );
    }

    @Transactional(readOnly = true)
    public CommonCodeResponse getCode(Long codeId) {
        return commonCodeRepository.findCodeById(codeId)
                .map(this::toCodeResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMON_CODE_NOT_FOUND));
    }

    CommonCodeResponse toCodeResponse(CommonCode commonCode) {
        CommonCodeGroup codeGroup = commonCode.getCodeGroup();
        return new CommonCodeResponse(
                commonCode.getCodeId(),
                codeGroup.getCodeGroupId(),
                codeGroup.getCodeGroup(),
                codeGroup.getCodeGroupName(),
                commonCode.getCode(),
                commonCode.getCodeName(),
                commonCode.getDescription(),
                commonCode.getSortOrder(),
                commonCode.getEnabledYn(),
                commonCode.getSystemYn(),
                commonCode.getMetadataJson(),
                commonCode.getCreatedByAdminId(),
                commonCode.getUpdatedByAdminId(),
                commonCode.getCreatedAt(),
                commonCode.getUpdatedAt()
        );
    }

    private CommonCodeGroupResponse toGroupResponse(CommonCodeGroup codeGroup) {
        return new CommonCodeGroupResponse(
                codeGroup.getCodeGroupId(),
                codeGroup.getCodeGroup(),
                codeGroup.getCodeGroupName(),
                codeGroup.getDescription(),
                codeGroup.getSortOrder(),
                codeGroup.getEnabledYn(),
                codeGroup.getSystemYn(),
                codeGroup.getCreatedByAdminId(),
                codeGroup.getUpdatedByAdminId(),
                codeGroup.getCreatedAt(),
                codeGroup.getUpdatedAt()
        );
    }

    private int totalPages(long totalElements, int size) {
        return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
