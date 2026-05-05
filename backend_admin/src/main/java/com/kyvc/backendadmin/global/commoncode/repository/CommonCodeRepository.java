package com.kyvc.backendadmin.global.commoncode.repository;

import com.kyvc.backendadmin.domain.admin.domain.AuditLog;
import com.kyvc.backendadmin.global.commoncode.domain.CommonCode;
import com.kyvc.backendadmin.global.commoncode.domain.CommonCodeGroup;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeGroupSearchRequest;
import com.kyvc.backendadmin.global.commoncode.dto.CommonCodeSearchRequest;

import java.util.List;
import java.util.Optional;

public interface CommonCodeRepository {

    /**
     * codeGroup, enabledYn 기준 조회 조건과 keyword 조건으로 공통코드 그룹 목록을 조회합니다.
     *
     * @param request 공통코드 그룹 조회 조건
     * @return 공통코드 그룹 목록
     */
    List<CommonCodeGroup> searchGroups(CommonCodeGroupSearchRequest request);

    /**
     * codeGroup, enabledYn 기준 조회 조건과 keyword 조건으로 공통코드 그룹 건수를 조회합니다.
     *
     * @param request 공통코드 그룹 조회 조건
     * @return 공통코드 그룹 건수
     */
    long countGroups(CommonCodeGroupSearchRequest request);

    /**
     * codeGroupId 기준으로 공통코드 그룹을 조회합니다.
     *
     * @param codeGroupId 공통코드 그룹 ID
     * @return 공통코드 그룹 Optional
     */
    Optional<CommonCodeGroup> findGroupById(Long codeGroupId);

    /**
     * codeGroup 기준으로 공통코드 그룹을 조회합니다.
     *
     * @param codeGroup 공통코드 그룹
     * @return 공통코드 그룹 Optional
     */
    Optional<CommonCodeGroup> findGroupByCodeGroup(String codeGroup);

    /**
     * codeGroup, code, enabledYn 기준 조회 조건과 keyword 조건으로 공통코드 목록을 조회합니다.
     *
     * @param request 공통코드 조회 조건
     * @return 공통코드 목록
     */
    List<CommonCode> searchCodes(CommonCodeSearchRequest request);

    /**
     * codeGroup, code, enabledYn 기준 조회 조건과 keyword 조건으로 공통코드 건수를 조회합니다.
     *
     * @param request 공통코드 조회 조건
     * @return 공통코드 건수
     */
    long countCodes(CommonCodeSearchRequest request);

    /**
     * codeId 기준으로 공통코드를 조회합니다.
     *
     * @param codeId 공통코드 ID
     * @return 공통코드 Optional
     */
    Optional<CommonCode> findCodeById(Long codeId);

    /**
     * 동일 codeGroup 안에서 code 중복 여부를 조회합니다.
     *
     * @param codeGroupId 공통코드 그룹 ID
     * @param code 코드값
     * @return 동일 그룹 내 코드가 존재하면 true
     */
    boolean existsCodeByGroupIdAndCode(Long codeGroupId, String code);

    /**
     * codeGroup 존재, codeGroup enabledYn, code 존재, commonCode enabledYn 기준으로 활성 공통코드를 조회합니다.
     *
     * @param codeGroup 공통코드 그룹
     * @param code 코드값
     * @return 활성 공통코드 존재 여부
     */
    boolean existsEnabledCode(String codeGroup, String code);

    /**
     * systemYn 기준 보호 정책 적용 대상인 공통코드를 제외하고 공통코드를 저장합니다.
     *
     * @param commonCode 저장할 공통코드 엔티티
     * @return 저장된 공통코드 엔티티
     */
    CommonCode saveCode(CommonCode commonCode);

    /**
     * systemYn 기준 보호 정책 적용 대상이 아닌 공통코드를 삭제합니다.
     *
     * @param commonCode 삭제할 공통코드 엔티티
     */
    void deleteCode(CommonCode commonCode);

    /**
     * 공통코드 변경 audit log 기록을 저장합니다.
     *
     * @param auditLog 저장할 감사로그 엔티티
     * @return 저장된 감사로그 엔티티
     */
    AuditLog saveAuditLog(AuditLog auditLog);
}
