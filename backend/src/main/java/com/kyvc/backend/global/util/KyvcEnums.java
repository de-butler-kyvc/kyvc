package com.kyvc.backend.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// KYvC 공통 enum 모음
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KyvcEnums {

    // 사용자 유형
    public enum UserType {
        CORPORATE_USER // 법인 사용자
    }

    // 사용자 상태
    public enum UserStatus {
        PENDING, // 가입 대기
        ACTIVE, // 활성
        LOCKED, // 잠금
        INACTIVE, // 비활성
        WITHDRAWN // 탈퇴
    }

    // 법인 상태
    public enum CorporateStatus {
        PENDING, // 심사 대기
        ACTIVE, // 활성
        INACTIVE, // 비활성
        SUSPENDED // 정지
    }

    // KYC 상태
    public enum KycStatus {
        DRAFT, // 임시 저장
        SUBMITTED, // 제출 완료
        AI_REVIEWING, // AI 심사 중
        NEED_SUPPLEMENT, // 보완 필요
        MANUAL_REVIEW, // 수동 심사 중
        APPROVED, // 승인
        REJECTED, // 반려
        VC_ISSUED // VC 발급 완료
    }

    // 원본 문서 저장 옵션
    public enum OriginalDocumentStoreOption {
        STORE, // 저장
        DO_NOT_STORE // 미저장
    }

    // AI 심사 상태
    public enum AiReviewStatus {
        QUEUED, // 대기
        RUNNING, // 진행
        SUCCESS, // 성공
        LOW_CONFIDENCE, // 신뢰도 낮음
        FAILED // 실패
    }

    // AI 심사 판정
    public enum AiReviewResult {
        PASS, // 통과
        FAIL, // 실패
        NEED_MANUAL_REVIEW // 수동 심사 필요
    }

    // 문서 업로드 상태
    public enum DocumentUploadStatus {
        UPLOADED, // 업로드 완료
        FAILED, // 업로드 실패
        DELETED // 삭제
    }

    // 보완 요청 상태
    public enum SupplementStatus {
        REQUESTED, // 요청
        SUBMITTED, // 제출
        COMPLETED, // 완료
        CANCELLED, // 취소
        EXPIRED // 만료
    }

    // 심사 이력 액션
    public enum ReviewActionType {
        SUBMIT, // KYC 제출
        AI_START, // AI 시작
        AI_COMPLETE, // AI 완료
        AI_FAILED, // AI 실패
        REQUEST_AI_REVIEW, // AI 심사 요청
        REQUEST_SUPPLEMENT, // 보완 요청
        SUPPLEMENT_SUBMIT, // 보완 제출
        MANUAL_REVIEW, // 수동 심사
        APPROVE, // 승인
        REJECT, // 반려
        ISSUE_VC, // VC 발급
        CHANGE_STATUS // 상태 변경
    }

    // Credential 상태
    public enum CredentialStatus {
        ISSUING, // 발급 중
        VALID, // 유효
        EXPIRED, // 만료
        REVOKED, // 폐기
        SUSPENDED, // 중지
        FAILED // 발급 실패
    }

    // KYC 완료 후 가이드 액션
    public enum KycCompletionAction {
        SUBMIT_KYC, // KYC 제출
        WAIT_REVIEW, // 심사 대기
        WAIT_AI_REVIEW, // AI 대기
        CHECK_SUPPLEMENT, // 보완 확인
        WAIT_MANUAL_REVIEW, // 수동 심사 대기
        CONTACT_SUPPORT, // 고객센터 문의
        OPEN_WALLET, // 지갑 열기
        ISSUE_CREDENTIAL // Credential 발급
    }

    // KYC 심사 결과 유형
    public enum KycReviewFindingType {
        SUMMARY, // 요약
        MANUAL_REVIEW_REASON, // 수동 심사 사유
        REJECT_REASON // 반려 사유
    }

    // VP 검증 상태
    public enum VpVerificationStatus {
        REQUESTED, // 요청
        PRESENTED, // 제출
        VALID, // 유효
        INVALID, // 무효
        REPLAY_SUSPECTED, // 재사용 의심
        EXPIRED // 만료
    }

    // Credential 유형
    public enum CredentialType {
        KYC_CREDENTIAL, // KYC VC
        BUSINESS_CREDENTIAL // 사업자 VC
    }

    // Credential Status 목적 코드
    public enum CredentialStatusPurpose {
        revocation // 폐기 관리 목적
    }

    // KYC 레벨
    public enum KycLevel {
        BASIC, // 기본
        STANDARD, // 표준
        ENHANCED // 강화
    }

    // 관할 코드
    public enum Jurisdiction {
        KR // 대한민국
    }

    // Issuer 정책 유형
    public enum IssuerPolicyType {
        WHITELIST, // 화이트리스트
        BLACKLIST, // 블랙리스트
        CREDENTIAL_TYPE_POLICY // Credential 유형 정책
    }

    // Issuer 정책 상태
    public enum IssuerPolicyStatus {
        PENDING, // 승인 대기
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

    // 관리자 상태
    public enum AdminUserStatus {
        ACTIVE, // 활성
        LOCKED, // 잠금
        INACTIVE // 비활성
    }

    // 관리자 역할 코드
    public enum RoleCode {
        BACKEND_ADMIN, // 백엔드 관리자
        CORE_ADMIN, // 코어 관리자
        POLICY_MANAGER, // 정책 관리자
        AUDITOR, // 감사자
        VIEWER, // 조회자
        SYSTEM_ADMIN // 시스템 관리자
    }

    // 행위자 유형
    public enum ActorType {
        USER, // 사용자
        ADMIN, // 관리자
        SYSTEM, // 시스템
        CORE // Core
    }

    // 감사 대상 유형
    public enum AuditTargetType {
        KYC_APPLICATION, // KYC 신청
        KYC_DOCUMENT, // KYC 문서
        KYC_SUPPLEMENT, // KYC 보완
        CREDENTIAL, // Credential
        VP_VERIFICATION, // VP 검증
        ISSUER_POLICY, // Issuer 정책
        ADMIN_USER, // 관리자 사용자
        ADMIN_ROLE, // 관리자 권한
        USER, // 사용자
        CORPORATE, // 법인
        NOTIFICATION // 알림
    }

    // Core 대상 유형
    public enum CoreTargetType {
        KYC_APPLICATION, // KYC 신청
        CREDENTIAL, // Credential
        VP_VERIFICATION // VP 검증
    }

    // 알림 유형
    public enum NotificationType {
        KYC_SUBMITTED, // KYC 제출
        AI_REVIEW_STARTED, // AI 심사 시작
        AI_REVIEW_COMPLETED, // AI 심사 완료
        MANUAL_REVIEW, // 수동 심사
        NEED_SUPPLEMENT, // 보완 필요
        SUPPLEMENT_SUBMITTED, // 보완 제출
        KYC_APPROVED, // KYC 승인
        KYC_REJECTED, // KYC 반려
        VC_ISSUED, // VC 발급
        VC_EXPIRED, // VC 만료
        WALLET_SAVED, // 지갑 저장
        VP_REQUESTED, // VP 요청
        VP_PRESENTED, // VP 제출
        VP_VERIFIED, // VP 검증
        VP_VERIFICATION_COMPLETED // VP 검증 완료
    }

    // 동의 유형
    public enum ConsentType {
        TERMS_OF_SERVICE, // 서비스 이용약관
        PRIVACY_POLICY, // 개인정보 처리방침
        KYC_PROCESSING, // KYC 처리 동의
        ORIGINAL_DOCUMENT_STORAGE, // 원본 문서 저장 동의
        MARKETING // 마케팅 동의
    }

    // 기기 바인딩 상태
    public enum DeviceBindingStatus {
        ACTIVE, // 활성
        BLOCKED, // 차단
        REMOVED // 해제
    }

    // Y/N 값
    public enum Yn {
        Y, // 예
        N // 아니오
    }

    // QR 유형
    public enum QrType {
        CREDENTIAL_OFFER, // Credential Offer
        VP_REQUEST // VP 요청
    }

    // 토큰 유형
    public enum TokenType {
        REFRESH, // Refresh 토큰
        ACCESS_JTI, // Access JTI
        PASSWORD_RESET, // 비밀번호 재설정 토큰
        MFA_SESSION // MFA 세션 토큰
    }

    // 토큰 상태
    public enum TokenStatus {
        ACTIVE, // 활성
        EXPIRED, // 만료
        REVOKED, // 폐기
        USED // 사용 완료
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
        REQUESTED, // 요청
        VERIFIED, // 인증 완료
        EXPIRED, // 만료
        FAILED, // 실패
        USED // 사용 완료
    }

    // Core 요청 유형
    public enum CoreRequestType {
        AI_REVIEW, // AI 심사
        VC_ISSUE, // VC 발급
        VC_STATUS_CHECK, // VC 상태 조회
        VP_VERIFY, // VP 검증
        XRPL_TX // XRPL 트랜잭션
    }

    // Core 요청 상태
    public enum CoreRequestStatus {
        QUEUED, // 대기
        REQUESTED, // 요청
        PROCESSING, // 처리 중
        SUCCESS, // 성공
        FAILED, // 실패
        TIMEOUT, // 타임아웃
        CALLBACK_RECEIVED, // 콜백 수신
        RETRYING // 재시도 중
    }

    // XRPL 트랜잭션 상태
    public enum XrplTransactionStatus {
        PENDING, // 대기
        CONFIRMED, // 확정
        FAILED // 실패
    }

    // 감사 액션 유형
    public enum AuditActionType {
        NOTIFICATION_READ, // 알림 단건 읽음
        NOTIFICATION_READ_ALL // 알림 전체 읽음
    }

    // QR 해석 다음 액션
    public enum QrNextAction {
        OPEN_CREDENTIAL_OFFER, // Credential Offer 열기
        OPEN_VP_REQUEST, // VP 요청 열기
        INVALID_QR // 잘못된 QR
    }
}
