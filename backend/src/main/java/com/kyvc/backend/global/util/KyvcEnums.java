package com.kyvc.backend.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// KYvC 업무 고정상수 enum 모음
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KyvcEnums {

    // 사용자 유형
    public enum UserType {
        CORPORATE_USER // 법인 사용자
    }

    // 사용자 상태
    public enum UserStatus {
        PENDING, // 가입 및 인증 대기
        ACTIVE, // 정상
        LOCKED, // 잠금
        INACTIVE, // 비활성
        WITHDRAWN // 탈퇴
    }

    // 법인 상태
    public enum CorporateStatus {
        PENDING, // 법인정보 등록 및 검증 대기
        ACTIVE, // 활성
        INACTIVE, // 비활성
        SUSPENDED // 정지
    }

    // KYC 신청 상태
    public enum KycStatus {
        DRAFT, // 임시저장
        SUBMITTED, // 제출완료
        AI_REVIEWING, // AI 심사중
        NEED_SUPPLEMENT, // 보완필요
        MANUAL_REVIEW, // 수동심사
        APPROVED, // 승인
        REJECTED, // 반려
        VC_ISSUED // VC 발급완료
    }

    // 원본서류 저장 옵션
    public enum OriginalDocumentStoreOption {
        STORE, // 원본 저장
        DO_NOT_STORE // 원본 미저장
    }

    // AI 심사 상태
    public enum AiReviewStatus {
        QUEUED, // 대기
        RUNNING, // 처리중
        SUCCESS, // 성공
        LOW_CONFIDENCE, // 낮은 신뢰도
        FAILED // 실패
    }

    // AI 심사 결과
    public enum AiReviewResult {
        PASS, // 통과
        FAIL, // 실패
        NEED_MANUAL_REVIEW // 수동심사 필요
    }

    // 문서 업로드 상태
    public enum DocumentUploadStatus {
        UPLOADED, // 업로드 완료
        FAILED, // 업로드 실패
        DELETED // 삭제
    }

    // 보완요청 상태
    public enum SupplementStatus {
        REQUESTED, // 보완 요청됨
        SUBMITTED, // 보완 제출됨
        COMPLETED, // 보완 완료
        CANCELLED, // 취소
        EXPIRED // 만료
    }

    // KYC 심사 이력 액션 유형
    public enum ReviewActionType {
        SUBMIT, // KYC 신청 제출
        AI_START, // AI 심사 시작
        AI_COMPLETE, // AI 심사 완료
        AI_FAILED, // AI 심사 실패
        REQUEST_AI_REVIEW, // AI 심사요청
        REQUEST_SUPPLEMENT, // 보완요청 생성
        SUPPLEMENT_SUBMIT, // 보완서류 제출
        MANUAL_REVIEW, // 수동심사 전환
        APPROVE, // 승인
        REJECT, // 반려
        ISSUE_VC, // VC 발급 처리
        CHANGE_STATUS // 기타 상태 변경
    }

    // Credential 상태
    public enum CredentialStatus {
        ISSUING, // 발급중
        VALID, // 유효
        EXPIRED, // 만료
        REVOKED, // 폐기
        SUSPENDED // 일시중지
    }

    // VP 검증 상태
    public enum VpVerificationStatus {
        REQUESTED, // 요청됨
        PRESENTED, // 제출됨
        VALID, // 유효
        INVALID, // 무효
        REPLAY_SUSPECTED, // 재사용 의심
        EXPIRED // 만료
    }

    // Credential 유형
    public enum CredentialType {
        KYC_CREDENTIAL, // 법인 KYC VC
        BUSINESS_CREDENTIAL // 기업 인증 VC
    }

    // Credential 상태 목적
    public enum CredentialStatusPurpose {
        revocation // 폐기 상태 확인
    }

    // KYC 검증 수준
    public enum KycLevel {
        BASIC, // 기본 KYC
        STANDARD, // 표준 KYC
        ENHANCED // 강화 KYC
    }

    // 관할 국가 및 지역
    public enum Jurisdiction {
        KR // 대한민국
    }

    // Issuer 정책 유형
    public enum IssuerPolicyType {
        WHITELIST, // 허용 목록
        BLACKLIST, // 차단 목록
        CREDENTIAL_TYPE_POLICY // Credential 유형 정책
    }

    // Issuer 정책 상태
    public enum IssuerPolicyStatus {
        PENDING, // 승인 및 검토 대기
        ACTIVE, // 활성
        INACTIVE, // 비활성
        REJECTED // 반려
    }

    // Issuer 유형
    public enum IssuerType {
        PLATFORM, // 플랫폼 Issuer
        FINANCIAL_INSTITUTION // 금융기관 Issuer
    }

    // Issuer 설정 상태
    public enum IssuerConfigStatus {
        ACTIVE, // 활성
        INACTIVE // 비활성
    }

    // 관리자 사용자 상태
    public enum AdminUserStatus {
        ACTIVE, // 정상
        LOCKED, // 잠금
        INACTIVE // 비활성
    }

    // 관리자 권한 코드
    public enum RoleCode {
        BACKEND_ADMIN, // 백엔드 관리자
        CORE_ADMIN, // 코어 관리자
        POLICY_MANAGER, // 정책 관리자
        AUDITOR, // 감사담당자
        VIEWER, // 조회 전용
        SYSTEM_ADMIN // 시스템 관리자
    }

    // 행위자 유형
    public enum ActorType {
        USER, // 일반 사용자
        ADMIN, // 관리자
        SYSTEM, // 시스템
        CORE // Core
    }

    // 감사로그 대상 유형
    public enum AuditTargetType {
        KYC_APPLICATION, // KYC 신청
        KYC_DOCUMENT, // KYC 문서
        KYC_SUPPLEMENT, // KYC 보완요청
        CREDENTIAL, // Credential
        VP_VERIFICATION, // VP 검증
        ISSUER_POLICY, // Issuer 정책
        ADMIN_USER, // 관리자 계정
        ADMIN_ROLE, // 관리자 권한
        USER, // 사용자
        CORPORATE, // 법인
        NOTIFICATION // 알림
    }

    // Core 요청 대상 유형
    public enum CoreTargetType {
        KYC_APPLICATION, // KYC 신청
        CREDENTIAL, // Credential
        VP_VERIFICATION // VP 검증
    }

    // 알림 유형
    public enum NotificationType {
        KYC_SUBMITTED, // KYC 신청 접수
        AI_REVIEW_STARTED, // AI 심사 시작
        AI_REVIEW_COMPLETED, // AI 심사 완료
        MANUAL_REVIEW, // 수동심사 전환
        NEED_SUPPLEMENT, // 보완요청
        SUPPLEMENT_SUBMITTED, // 보완서류 제출 완료
        KYC_APPROVED, // KYC 승인
        KYC_REJECTED, // KYC 반려
        VC_ISSUED, // VC 발급 완료
        VC_EXPIRED, // VC 만료
        WALLET_SAVED, // Wallet 저장 완료
        VP_REQUESTED, // VP 제출 요청
        VP_PRESENTED, // VP 제출 완료
        VP_VERIFIED, // VP 검증 완료
        VP_VERIFICATION_COMPLETED // VP 검증 처리 완료
    }

    // 사용자 동의 유형
    public enum ConsentType {
        TERMS_OF_SERVICE, // 서비스 이용약관
        PRIVACY_POLICY, // 개인정보 처리방침
        KYC_PROCESSING, // KYC 처리 동의
        ORIGINAL_DOCUMENT_STORAGE, // 원본서류 저장 동의
        MARKETING // 마케팅 동의
    }

    // 기기 바인딩 상태
    public enum DeviceBindingStatus {
        ACTIVE, // 활성
        BLOCKED, // 차단
        REMOVED // 제거
    }

    // 인증 토큰 유형
    public enum TokenType {
        REFRESH, // Refresh Token
        ACCESS_JTI, // Access Token JTI
        PASSWORD_RESET, // 비밀번호 재설정 토큰
        MFA_SESSION // MFA 세션 토큰
    }

    // 인증 토큰 상태
    public enum TokenStatus {
        ACTIVE, // 활성
        EXPIRED, // 만료
        REVOKED, // 폐기
        USED // 사용완료
    }

    // MFA 목적
    public enum MfaPurpose {
        LOGIN, // 로그인
        IMPORTANT_ACTION, // 중요 작업
        PASSWORD_RESET, // 비밀번호 재설정
        KYC_APPROVE, // KYC 승인
        KYC_REJECT, // KYC 반려
        VC_ISSUE, // VC 발급
        POLICY_CHANGE // 정책 변경
    }

    // MFA 상태
    public enum MfaStatus {
        REQUESTED, // 요청됨
        VERIFIED, // 검증됨
        EXPIRED, // 만료
        FAILED, // 실패
        USED // 사용완료
    }

    // Core 요청 유형
    public enum CoreRequestType {
        AI_REVIEW, // AI 심사
        VC_ISSUE, // VC 발급
        VC_STATUS_CHECK, // VC 상태조회
        VP_VERIFY, // VP 검증
        XRPL_TX // XRPL 트랜잭션
    }

    // Core 요청 상태
    public enum CoreRequestStatus {
        QUEUED, // 대기
        REQUESTED, // 요청됨
        PROCESSING, // 처리중
        SUCCESS, // 성공
        FAILED, // 실패
        CALLBACK_RECEIVED, // Callback 수신
        RETRYING // 재시도중
    }

    // 감사로그 작업 유형

    // XRPL 트랜잭션 상태
    public enum XrplTransactionStatus {
        PENDING, // 대기
        CONFIRMED, // 확정
        FAILED // 실패
    }

    public enum AuditActionType {
        NOTIFICATION_READ, // 알림 읽음 처리
        NOTIFICATION_READ_ALL // 알림 전체 읽음 처리
    }
}
