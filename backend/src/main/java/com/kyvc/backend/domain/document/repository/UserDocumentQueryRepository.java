package com.kyvc.backend.domain.document.repository;

import com.kyvc.backend.domain.document.dto.UserDocumentDeleteRequestListResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentDetailResponse;
import com.kyvc.backend.domain.document.dto.UserDocumentListResponse;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 문서함 조회 Repository
 */
public interface UserDocumentQueryRepository {

    /**
     * 사용자 소유 문서 목록 조회
     *
     * @param userId 사용자 ID
     * @param documentTypeCode 문서 유형 코드
     * @param status 문서 상태 코드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자 문서 목록
     */
    List<UserDocumentListResponse.Item> searchDocuments(
            Long userId, // 사용자 ID
            String documentTypeCode, // 문서 유형 코드
            String status, // 문서 상태 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * 사용자 소유 문서 건수 조회
     *
     * @param userId 사용자 ID
     * @param documentTypeCode 문서 유형 코드
     * @param status 문서 상태 코드
     * @return 사용자 문서 건수
     */
    long countDocuments(
            Long userId, // 사용자 ID
            String documentTypeCode, // 문서 유형 코드
            String status // 문서 상태 코드
    );

    /**
     * 사용자 소유 문서 상세 조회
     *
     * @param userId 사용자 ID
     * @param documentId 문서 ID
     * @return 사용자 문서 상세 조회 결과
     */
    Optional<UserDocumentDetailResponse> findDocumentDetail(
            Long userId, // 사용자 ID
            Long documentId // 문서 ID
    );

    /**
     * 사용자 소유 문서 삭제 요청 이력 조회
     *
     * @param userId 사용자 ID
     * @param status 삭제 요청 상태 코드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 문서 삭제 요청 이력 목록
     */
    List<UserDocumentDeleteRequestListResponse.Item> searchDeleteRequests(
            Long userId, // 사용자 ID
            String status, // 삭제 요청 상태 코드
            int page, // 페이지 번호
            int size // 페이지 크기
    );

    /**
     * 사용자 소유 문서 삭제 요청 건수 조회
     *
     * @param userId 사용자 ID
     * @param status 삭제 요청 상태 코드
     * @return 문서 삭제 요청 건수
     */
    long countDeleteRequests(
            Long userId, // 사용자 ID
            String status // 삭제 요청 상태 코드
    );
}
