package com.kyvc.backend.domain.commoncode.repository;

import com.kyvc.backend.domain.commoncode.dto.CommonCodeItem;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

// 공통코드 조회용 Repository 구현체
@Repository
@RequiredArgsConstructor
public class CommonCodeQueryRepositoryImpl implements CommonCodeQueryRepository {

    private final EntityManager entityManager;

    // 그룹 코드 기준 활성 공통코드 목록 조회
    @Override
    public List<CommonCodeItem> findEnabledCodeItemsByCodeGroup(
            String codeGroup // 공통코드 그룹 코드
    ) {
        return entityManager.createQuery("""
                        select new com.kyvc.backend.domain.commoncode.dto.CommonCodeItem(
                            commonCodeGroup.codeGroup,
                            commonCode.code,
                            commonCode.codeName,
                            commonCode.description,
                            commonCode.sortOrder
                        )
                        from CommonCode commonCode
                        join commonCode.codeGroup commonCodeGroup
                        where commonCodeGroup.codeGroup = :codeGroup
                          and commonCodeGroup.enabled = true
                          and commonCode.enabled = true
                        order by commonCode.sortOrder asc, commonCode.code asc
                        """, CommonCodeItem.class)
                .setParameter("codeGroup", codeGroup)
                .getResultList();
    }
}
