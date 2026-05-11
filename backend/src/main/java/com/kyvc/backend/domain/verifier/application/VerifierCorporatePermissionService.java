package com.kyvc.backend.domain.verifier.application;

import com.kyvc.backend.domain.verifier.dto.VerifierCorporatePermissionListResponse;
import com.kyvc.backend.domain.verifier.repository.VerifierCorporatePermissionQueryRepository;
import com.kyvc.backend.global.security.VerifierPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Verifier 기업 권한 확인 서비스
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerifierCorporatePermissionService {

    private static final int DEFAULT_PAGE = 0; // 기본 페이지 번호
    private static final int DEFAULT_SIZE = 20; // 기본 페이지 크기
    private static final int MAX_SIZE = 100; // 최대 페이지 크기

    private final VerifierCorporatePermissionQueryRepository verifierCorporatePermissionQueryRepository;

    // Verifier 기업 권한 확인 목록 조회
    public VerifierCorporatePermissionListResponse getPermissions(
            VerifierPrincipal principal, // 인증된 Verifier 주체
            Integer page, // 페이지 번호
            Integer size, // 페이지 크기
            Long corporateId, // 법인 ID
            String permissionCode // 권한 코드
    ) {
        PageRequest pageRequest = normalizePageRequest(page, size);
        List<VerifierCorporatePermissionListResponse.Item> items =
                verifierCorporatePermissionQueryRepository.findPermissions(
                        principal.verifierId(),
                        corporateId,
                        permissionCode,
                        pageRequest.page(),
                        pageRequest.size()
                );
        long totalElements = verifierCorporatePermissionQueryRepository.countPermissions(
                principal.verifierId(),
                corporateId,
                permissionCode
        );
        return new VerifierCorporatePermissionListResponse(
                items,
                new VerifierCorporatePermissionListResponse.PageInfo(
                        pageRequest.page(),
                        pageRequest.size(),
                        totalElements,
                        totalPages(totalElements, pageRequest.size())
                )
        );
    }

    private PageRequest normalizePageRequest(
            Integer page, // 페이지 번호
            Integer size // 페이지 크기
    ) {
        int normalizedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageRequest(normalizedPage, normalizedSize);
    }

    private int totalPages(
            long totalElements, // 전체 건수
            int size // 페이지 크기
    ) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    private record PageRequest(
            int page, // 페이지 번호
            int size // 페이지 크기
    ) {
    }
}
