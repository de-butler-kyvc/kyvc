package com.kyvc.backendadmin.domain.admin.repository;

import com.kyvc.backendadmin.domain.admin.domain.AdminUser;
import com.kyvc.backendadmin.domain.admin.dto.AdminUserSearchRequest;

import java.util.List;

// 관리자 계정 목록/검색 전용 QueryRepository 계약
/**
 * 관리자 계정 목록과 검색 조회를 담당하는 QueryRepository입니다.
 *
 * <p>admin_users 기준으로 admin_user_roles, admin_roles 조인을 포함한 검색 조건과
 * 페이징 조회 책임을 가집니다.</p>
 */
public interface AdminUserQueryRepository {

    // 검색 조건에 맞는 관리자 페이지 조회
    /**
     * 검색 조건에 맞는 관리자 계정 페이지를 조회합니다.
     *
     * @param request 페이징, 키워드, 상태, 권한 코드 검색 조건
     * @return 검색 조건에 맞는 관리자 계정 목록
     */
    List<AdminUser> search(AdminUserSearchRequest request);

    // 검색 조건에 맞는 전체 관리자 수 조회
    /**
     * 검색 조건에 맞는 전체 관리자 계정 수를 조회합니다.
     *
     * @param request 키워드, 상태, 권한 코드 검색 조건
     * @return 검색 조건에 맞는 전체 관리자 계정 수
     */
    long count(AdminUserSearchRequest request);
}
