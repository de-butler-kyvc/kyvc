# KYvC Frontend Admin

![KYvC Admin Banner](./public/kyvcwordmarkdark.png)

KYvC 관리자 서비스는 운영자가 KYC 신청, VC/VP, 사용자, Issuer/Verifier, 정책, 감사로그, 리포트를 관리하는 백오피스 프론트엔드입니다.

## 1. 서비스 개요

### 사용자 유형

- KYvC 내부 운영 관리자
- KYC 심사 담당자
- Issuer / Verifier 운영 담당자
- 관리자 계정 및 권한 관리 담당자

### 담당 화면

- 관리자 로그인, MFA, 비밀번호 재설정
- 대시보드
- KYC 신청 목록/상세/수동 심사/재심사/AI 심사 결과/보완 요청
- VC 발급 내역, 재발급, 폐기
- VP 검증 이력
- 사용자 및 법인 사용자 관리
- Issuer 정책, 승인, whitelist, blacklist
- Verifier 등록/상세/신뢰 정책
- SDK 관리 및 사용량
- 감사로그, 보안 이벤트, 데이터 접근 로그
- 운영 리포트
- 관리자 계정, 역할, 그룹
- 공통코드, 알림 템플릿, 설정

### 서비스 역할

- `backend_admin`에서 제공하는 관리자 API를 호출해 운영 데이터를 조회/수정합니다.
- 관리자 인증 상태를 확인하고 보호된 관리자 화면 접근을 제어합니다.
- KYC 심사, VC/VP 운영, Issuer/Verifier 정책 관리 등 운영 업무 화면을 제공합니다.

### 서비스 도메인

- 로컬: `http://localhost:3000`
- dev API 기본 대상: `https://dev-admin-api-kyvc.khuoo.synology.me`
- API 경로는 Next.js rewrites를 통해 `/api/admin/*` 형태로 호출합니다.

## 2. 기술 스택

### 언어

- TypeScript
- TSX

### 프레임워크

- Next.js 16
- React 19
- App Router

### 패키지 매니저

- npm 기준 실행 스크립트 제공
- `package-lock.json`, `pnpm-lock.yaml`이 함께 존재하므로 팀 기준에 맞춰 하나의 패키지 매니저로 통일 필요

### 주요 라이브러리

- Tailwind CSS
- lucide-react
- shadcn / Radix UI 기반 UI 컴포넌트
- class-variance-authority
- clsx
- tailwind-merge

## 3. 화면 구성

### 주요 라우트

| Route | 설명 |
| --- | --- |
| `/` | 진입 페이지 |
| `/login` | 관리자 로그인 |
| `/login/mfa` | MFA 인증 |
| `/login/reset` | 비밀번호 재설정 |
| `/dashboard` | 관리자 대시보드 |
| `/kyc` | KYC 신청 관리 |
| `/kyc/[id]` | KYC 상세 |
| `/kyc/[id]/manual-review` | KYC 수동 심사 |
| `/kyc/[id]/re-review` | KYC 재심사 |
| `/kyc/[id]/ai-result` | AI 심사 결과 |
| `/kyc/[id]/supplement-request` | 보완 요청 |
| `/kyc/[id]/supplement-history` | 보완 이력 |
| `/kyc/[id]/review-history` | 심사 이력 |
| `/vc` | VC 관리 |
| `/vc/[id]` | VC 상세 |
| `/vc/[id]/reissue` | VC 재발급 |
| `/vc/[id]/revoke` | VC 폐기 |
| `/vp` | VP 검증 이력 |
| `/users` | 사용자 관리 |
| `/users/[id]` | 사용자 상세 |
| `/corporates` | 법인 사용자 관리 |
| `/issuer` | Issuer 관리 |
| `/issuer/[id]` | Issuer 상세 |
| `/issuer/new` | Issuer 등록 |
| `/issuer/approval` | Issuer 승인 |
| `/issuer/whitelist` | Issuer whitelist |
| `/issuer/blacklist` | Issuer blacklist |
| `/verifier` | Verifier 관리 |
| `/verifier/[id]` | Verifier 상세 |
| `/verifier/trust-policy` | Verifier 신뢰 정책 |
| `/policy` | 정책/규칙 |
| `/policy/required-docs` | 필수 서류 정책 |
| `/issuer-policy` | Issuer 정책 |
| `/review` | 심사 관리 |
| `/review/detail` | 심사 상세 |
| `/sdk` | SDK 관리 |
| `/sdk/usage` | SDK 사용량 |
| `/audit-log` | 감사로그 |
| `/audit` | 감사/이벤트 조회 |
| `/report` | 리포트 |
| `/managers` | 관리자 계정 관리 |
| `/managers/[id]` | 관리자 상세 |
| `/managers/groups` | 관리자 그룹 |
| `/common-codes` | 공통코드 |
| `/settings` | 설정 |
| `/settings/notifications` | 알림 템플릿 |

### 사이트맵

```text
frontend_admin
├─ app
│  ├─ login
│  ├─ (admin)
│  │  ├─ dashboard
│  │  ├─ kyc
│  │  ├─ vc
│  │  ├─ vp
│  │  ├─ users
│  │  ├─ corporates
│  │  ├─ issuer
│  │  ├─ verifier
│  │  ├─ policy
│  │  ├─ sdk
│  │  ├─ audit
│  │  ├─ audit-log
│  │  ├─ report
│  │  ├─ managers
│  │  ├─ common-codes
│  │  └─ settings
│  ├─ layout.tsx
│  ├─ page.tsx
│  └─ globals.css
├─ components
│  ├─ layout
│  ├─ nav
│  └─ ui
├─ lib
│  ├─ api
│  ├─ auth-session.ts
│  └─ utils.ts
├─ public
└─ types
```

## 4. API 연동 구조

### 호출 대상 서버

- 호출 대상 서비스: `backend_admin`
- 개발 환경 기본 API 서버: `BACK_ADMIN_API_URL`
- `BACK_ADMIN_API_URL` 기본값: `https://dev-admin-api-kyvc.khuoo.synology.me`

### API Prefix

- 인증: `/api/admin/auth`
- 관리자 본인 정보: `/api/admin/me`
- 백엔드 관리자 도메인 API: `/api/admin/backend`

주요 API 모듈은 `lib/api` 아래에 도메인별로 분리되어 있습니다.

```text
lib/api
├─ auth.ts
├─ me.ts
├─ kyc.ts
├─ credentials.ts
├─ vp.ts
├─ users.ts
├─ issuer.ts
├─ verifier.ts
├─ managers.ts
├─ audit.ts
├─ reports.ts
├─ common-codes.ts
├─ notification-templates.ts
└─ health.ts
```

### 인증/세션 처리

- 로그인 성공 시 access token은 `localStorage`와 `auth_token` cookie에 저장합니다.
- refresh token은 `localStorage`에 저장합니다.
- API 호출 시 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.
- 쿠키 기반 세션 연동을 위해 fetch 옵션에 `credentials: "include"`를 사용합니다.
- 관리자 보호 라우트는 `(admin)/layout.tsx`에서 `/api/admin/auth/session`을 호출해 세션을 확인합니다.
- 세션 확인 실패 또는 미인증 응답 시 `/login`으로 이동합니다.

## 5. 환경 변수 구조

### 로컬 환경

```env
BACK_ADMIN_API_URL=https://dev-admin-api-kyvc.khuoo.synology.me
NEXT_PUBLIC_ADMIN_ACCESS_TOKEN=
NEXT_PUBLIC_API_BASE_URL=
```

- `BACK_ADMIN_API_URL`: 개발 서버에서 `/api/admin/*` rewrite 대상이 되는 관리자 API 서버입니다.
- `NEXT_PUBLIC_ADMIN_ACCESS_TOKEN`: 임시 개발용 access token fallback입니다.
- `NEXT_PUBLIC_API_BASE_URL`: health check 등 일부 API에서 사용할 수 있는 public API base URL입니다.

### dev 환경

- dev 배포에서는 관리자 API 도메인을 `BACK_ADMIN_API_URL`로 명시합니다.
- 프론트 정적 산출물과 API reverse proxy 규칙은 인프라 설정과 함께 관리합니다.

### prod 환경

- prod 배포에서는 운영 관리자 API 도메인을 사용합니다.
- 민감한 토큰은 환경 변수에 직접 고정하지 않고 로그인/세션 발급 흐름을 통해 처리합니다.

## 6. 실행 구조

### 패키지 설치

```bash
npm install
```

### 로컬 실행

```bash
npm run dev
```

- Next.js 개발 서버는 `0.0.0.0` host로 실행됩니다.
- 기본 포트는 Next.js 기본값인 `3000`입니다.
- 개발 서버에서는 `next.config.ts`의 rewrites가 적용됩니다.

### 빌드

```bash
npm run build
```

### 프로덕션 실행

```bash
npm run start
```

## 7. 개발 규칙

### 디렉터리 작업 범위

- `frontend_admin`은 관리자 프론트엔드 화면과 API 호출 클라이언트만 담당합니다.
- 관리자 서버 비즈니스 로직은 `backend_admin`에서 처리합니다.
- 사용자 서비스 화면은 `frontend`, Core 관리자 화면은 `frontend_core_admin`에서 작업합니다.

### API 호출 규칙

- API 호출 함수는 `lib/api/{domain}.ts`에 도메인별로 작성합니다.
- 페이지 컴포넌트에서 fetch URL을 직접 조립하지 않고 API 모듈 함수를 사용합니다.
- 관리자 API는 `/api/admin/auth`, `/api/admin/me`, `/api/admin/backend` prefix를 사용합니다.
- `backend_admin`에서 `core`를 우회 호출해야 하는 요구가 있더라도 프론트에서는 `backend_admin` API만 호출합니다.

### 인증 처리 규칙

- access token 조회는 `lib/auth-session.ts`의 `getAccessTokenForApi()`를 사용합니다.
- 로그인/로그아웃/토큰 갱신/세션 확인은 `lib/api/auth.ts`를 통해 처리합니다.
- 인증이 필요한 화면은 `(admin)` 라우트 그룹 아래에 배치합니다.
- 인증 실패 시 사용자를 `/login`으로 이동시킵니다.

### 화면/컴포넌트 작성 규칙

- 공통 레이아웃은 `components/layout`을 사용합니다.
- 재사용 가능한 UI 컴포넌트는 `components/ui`에 둡니다.
- 화면별 페이지는 App Router 규칙에 맞춰 `app/(admin)/{domain}/page.tsx`에 작성합니다.
- 상세 화면에서 클라이언트 상태가 많은 경우 `ClientPage.tsx`로 분리합니다.
- 경로 alias는 `@/*`를 사용합니다.
