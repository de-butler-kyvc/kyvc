package com.kyvc.backendadmin.domain.kyc.application;

import com.kyvc.backendadmin.global.exception.ApiException;
import com.kyvc.backendadmin.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 공통코드 존재 및 사용 가능 여부를 검증하는 Validator입니다.
 *
 * <p>common_code_groups와 common_codes를 조인하여 특정 코드 그룹의 코드가 존재하고
 * 활성 상태인지 확인합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class CommonCodeValidator {

    private final ObjectProvider<EntityManager> entityManagerProvider;

    /**
     * 공통코드 그룹과 코드값 기준으로 활성 공통코드인지 검증합니다.
     *
     * @param codeGroup 공통코드 그룹
     * @param code 검증할 코드
     */
    public void validateEnabledCode(String codeGroup, String code) {
        Long count = ((Number) entityManager()
                .createNativeQuery("""
                        select count(*)
                        from common_codes code
                        join common_code_groups code_group on code_group.code_group_id = code.code_group_id
                        where code_group.code_group = :codeGroup
                          and code.code = :code
                          and code_group.enabled_yn = 'Y'
                          and code.enabled_yn = 'Y'
                        """)
                .setParameter("codeGroup", codeGroup)
                .setParameter("code", code)
                .getSingleResult()).longValue();
        if (count == 0) {
            throw new ApiException(ErrorCode.COMMON_CODE_NOT_FOUND);
        }
    }

    private EntityManager entityManager() {
        return entityManagerProvider.getObject();
    }
}
