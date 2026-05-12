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

    // 대리인 권한 상태
    public enum AgentAuthorityStatus {
        ACTIVE, // 활성
        INACTIVE, // 비활성
        EXPIRED, // 만료
        SUSPENDED, // 정지
        REVOKED // 폐기
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
        REVOKE_REQUESTED, // 폐기 요청
        REISSUE_REQUESTED, // 재발급 요청
        FAILED // 발급 실패
    }

    // Credential Offer 상태
    public enum CredentialOfferStatus {
        ACTIVE, // 활성
        USED, // 사용 완료
        EXPIRED, // 만료
        CANCELLED, // 취소
        FAILED // 실패
    }

    // Credential 요청 유형
    public enum CredentialRequestType {
        ISSUE, // 발급
        REISSUE, // 재발급
        REVOKE, // 폐기
        STATUS_CHECK // 상태 확인
    }

    // Credential 요청 상태
    public enum CredentialRequestStatus {
        REQUESTED, // 요청
        PROCESSING, // 처리 중
        COMPLETED, // 완료
        FAILED, // 실패
        CANCELLED // 취소
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
        EXPIRED, // 만료
        FAILED // 실패
    }

    // VP 요청 유형
    public enum VpRequestType {
        VP_VERIFY, // VP 검증
        RE_AUTH, // 재인증
        TEST_VERIFY, // 테스트 검증
        FINANCE_VERIFY, // 금융 검증
        VP_LOGIN, // 모바일 VP 로그인
        CORPORATE_PERMISSION_CHECK // 법인 권한 확인
    }

    // Verifier 상태
    public enum VerifierStatus {
        PENDING, // 대기
        APPROVED, // 승인
        ACTIVE, // 활성
        SUSPENDED, // 정지
        REJECTED // 반려
    }

    // Verifier API Key 상태
    public enum VerifierApiKeyStatus {
        ACTIVE, // 활성
        REVOKED, // 폐기
        EXPIRED, // 만료
        ROTATED // 교체
    }

    // Verifier Callback 상태
    public enum VerifierCallbackStatus {
        ACTIVE, // 활성
        INACTIVE, // 비활성
        DISABLED // 비활성화
    }

    // Callback 전송 상태
    public enum CallbackDeliveryStatus {
        PENDING, // 대기
        SUCCESS, // 성공
        SENT, // 발송
        FAILED // 실패
    }

    // Verifier 행위 유형
    public enum VerifierActionType {
        VP_REQUEST, // VP 요청
        VP_VERIFY, // VP 검증
        API_KEY_ISSUE, // API Key 발급
        API_CALL, // API 호출
        POLICY_SYNC, // 정책 동기화
        RE_AUTH, // 재인증
        TEST_VERIFY, // 테스트 검증
        USAGE_EXPORT // 사용량 내보내기
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

    // DID 기관 매핑 상태
    public enum DidInstitutionStatus {
        ACTIVE, // 사용 가능
        DELETED, // 삭제
        SUSPENDED // 정지
    }

    // 관리자 상태
    public enum AdminUserStatus {
        ACTIVE, // 활성
        LOCKED, // 잠금
        INACTIVE // 비활성
    }

    // 관리자 역할 코드
    public enum RoleCode {
        OPERATOR, // 일반 운영자
        SYSTEM_ADMIN // 시스템 관리자
    }

    // 행위자 유형
    public enum ActorType {
        USER, // 사용자
        ADMIN, // 관리자
        SYSTEM, // 시스템
        CORE, // Core
        VERIFIER, // Verifier
        FINANCE // 금융사
    }

    // 업로드 행위자 유형
    public enum UploadActorType {
        USER, // 사용자
        FINANCE, // 금융사
        ADMIN, // 관리자
        SYSTEM // 시스템
    }

    // 감사 대상 유형
    public enum AuditTargetType {
        KYC_APPLICATION, // KYC 신청
        KYC_DOCUMENT, // KYC 문서
        KYC_SUPPLEMENT, // KYC 보완
        CREDENTIAL, // Credential
        CREDENTIAL_OFFER, // Credential Offer
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

    // 알림 채널
    public enum NotificationChannel {
        IN_APP, // 앱 내 알림
        WEB, // 웹
        APP_PUSH, // 앱 푸시
        EMAIL, // 이메일
        SMS // SMS
    }

    // 알림 발송 상태
    public enum NotificationSendStatus {
        READY, // 준비
        PENDING, // 대기
        SENT, // 발송
        FAILED, // 실패
        CANCELLED // 취소
    }

    // 신청 채널
    public enum ApplicationChannel {
        ONLINE, // 온라인
        FINANCE_VISIT, // 금융사 방문
        WEB, // 웹
        MOBILE, // 모바일
        FINANCE_BRANCH // 금융사 영업점
    }

    // 문서 삭제 요청 상태
    public enum DocumentDeleteRequestStatus {
        REQUESTED, // 요청
        APPROVED, // 승인
        REJECTED, // 반려
        COMPLETED, // 완료
        CANCELLED // 취소
    }

    // 금융사 고객 연결 상태
    public enum FinanceCustomerLinkStatus {
        ACTIVE, // 활성
        INACTIVE, // 비활성
        UNLINKED // 연결 해제
    }

    // 금융사 KYC QR 상태
    public enum FinanceKycQrStatus {
        NOT_ISSUED, // 미발급
        ACTIVE, // 활성
        EXPIRED, // 만료
        USED, // 사용 완료
        WALLET_SAVED // Wallet 저장 완료
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
        SIGNUP, // 회원가입 이메일 인증
        SIGNUP_EMAIL_VERIFICATION, // 회원가입 이메일 인증
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
        VC_REISSUE, // VC 재발급
        VC_REVOKE, // VC 폐기
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
