# KYvC Frontend Core Admin API

![KYvC Core Admin Banner](./public/kyvcwordmarkdark.png)

KYvC Core Admin API 프론트엔드는 Core 운영자가 Core 상태와 AI Provider 설정을 확인/관리하기 위한 관리자 콘솔입니다.

## 1. 서비스 개요

### 사용자 유형

- KYvC Core 운영 관리자
- AI/OCR Provider 설정 담당자
- Core API 운영 및 모니터링 담당자
- Core 모듈 배포/설정 승인 담당자

### 담당 화면

- 관리자 로그인, MFA, 비밀번호 재설정
- Core Admin 대시보드
- Core API 상태 및 health check 확인
- AI Provider 설정
- OCR / LLM Provider 선택 및 변경 이력 확인
- Schema, VC, VP, XRPL, Issuer, SDK, 버전/배포, 설정 승인, 감사로그 화면 뼈대

현재 라우트 파일은 대부분 구성되어 있으나, 실제 접근이 허용된 화면은 `dashboard`, `ai/settings` 중심입니다. 나머지 메뉴는 개발중 상태로 안내 후 대시보드로 이동합니다.

### 서비스 역할

- `core_admin`에서 제공하는 `/admin/*`, `/health` API를 호출해 Core 운영 상태를 조회합니다.
- Core AI Provider 설정을 조회/수정합니다.
- Core 운영 관리자용 화면과 API 연동 클라이언트를 제공합니다.
- 사용자 서비스 또는 일반 관리자 서비스 API를 직접 호출하지 않습니다.

### 서비스 도메인

- 로컬: `http://localhost:3000`
- dev Core Admin API 기본 대상: `https://dev-admin-core-kyvc.khuoo.synology.me`
- Core Admin API 경로는 Next.js rewrites 또는 `NEXT_PUBLIC_CORE_ADMIN_API_BASE_URL`을 통해 연결합니다.

## 2. 기술 스택

### 언어

- TypeScript
- TSX

### 프레임워크

- Next.js 16
- React 19
- App Router

### 패키지 매니저

- npm

### 주요 라이브러리

- Tailwind CSS
- lucide-react
- shadcn / Base UI 기반 UI 컴포넌트
- class-variance-authority
- clsx
- tailwind-merge

## 3. 화면 구성

### 주요 라우트

| Route | 설명 | 현재 상태 |
| --- | --- | --- |
| `/` | 로그인 페이지로 redirect | 사용 |
| `/login` | 관리자 로그인 | 사용 |
| `/login/mfa` | MFA 인증 | 준비 |
| `/login/reset` | 비밀번호 재설정 | 준비 |
| `/dashboard` | Core Admin 대시보드, health/status 조회 | 사용 |
| `/ai/settings` | AI Provider 설정 | 사용 |
| `/ai/settings/azure` | Azure OpenAI 연동 설정 | 준비 |
| `/ai/settings/prompts` | 프롬프트 목록 | 준비 |
| `/ai/settings/prompts/[id]/[version]` | 프롬프트 버전 상세 | 준비 |
| `/ai/settings/threshold` | AI 임계치 설정 | 준비 |
| `/ai/status` | AI 처리 현황 | 개발중 |
| `/ai/status/[id]` | AI 처리 상세 | 개발중 |
| `/schema` | Schema 관리 | 개발중 |
| `/schema/new` | Schema 등록 | 개발중 |
| `/schema/[id]` | Schema 상세 | 개발중 |
| `/vc` | VC 발급 관리 | 개발중 |
| `/vc/detail` | VC 상세 | 개발중 |
| `/vp` | VP 검증 관리 | 개발중 |
| `/vp/detail` | VP 상세 | 개발중 |
| `/xrpl` | XRPL 관리 | 개발중 |
| `/xrpl/transactions` | XRPL 트랜잭션 목록 | 개발중 |
| `/xrpl/transactions/detail` | XRPL 트랜잭션 상세 | 개발중 |
| `/xrpl/reprocess` | XRPL 재처리 | 개발중 |
| `/issuer` | Issuer 관리 | 개발중 |
| `/issuer/keys` | Issuer 키 관리 | 개발중 |
| `/sdk` | SDK 관리 | 개발중 |
| `/sdk/metadata` | SDK 메타데이터 | 개발중 |
| `/sdk/compatibility` | SDK 호환성 | 개발중 |
| `/sdk/testvectors` | SDK 테스트 벡터 | 개발중 |
| `/version` | 버전/배포 관리 | 개발중 |
| `/version/[module]` | 모듈별 버전 상세 | 개발중 |
| `/version/deploy-history` | 배포 이력 | 개발중 |
| `/approval` | 설정 승인 | 개발중 |
| `/audit-log` | 감사로그 | 개발중 |
| `/api-docs` | API 문서 화면 | 준비 |

### 사이트맵

```text
frontend_core_admin_api
├─ app
│  ├─ login
│  ├─ (admin)
│  │  ├─ dashboard
│  │  ├─ ai
│  │  │  ├─ settings
│  │  │  └─ status
│  │  ├─ schema
│  │  ├─ vc
│  │  ├─ vp
│  │  ├─ xrpl
│  │  ├─ issuer
│  │  ├─ sdk
│  │  ├─ version
│  │  ├─ approval
│  │  ├─ audit-log
│  │  └─ api-docs
│  ├─ layout.tsx
│  ├─ page.tsx
│  └─ globals.css
├─ components
│  ├─ layout
│  └─ ui
├─ lib
│  ├─ api
│  │  ├─ api-base.ts
│  │  ├─ auth.ts
│  │  └─ core-admin.ts
│  ├─ auth-session.ts
│  └─ utils.ts
├─ public
└─ package.json
```

## 4. API 연동 구조

### 호출 대상 서버

- 호출 대상 서비스: `core_admin`
- 기본 dev API 서버: `https://dev-admin-core-kyvc.khuoo.synology.me`
- Next.js rewrite 대상 환경 변수:
  - `CORE_ADMIN_API_URL`
  - `NEXT_PUBLIC_CORE_ADMIN_API_BASE_URL`

### API Prefix

- Core Admin API: `/admin`
- Health check: `/health`
- 관리자 인증 API: `/api/admin/auth`

`lib/api/core-admin.ts`의 주요 호출은 다음과 같습니다.

```text
GET /health
GET /admin/core/status
GET /admin/provider-selections/options
GET /admin/provider-selections
PUT /admin/provider-selections/{category}
GET /admin/provider-selections/history
```

### 인증/세션 처리

- 로그인 성공 시 access token은 `auth_token` cookie와 `localStorage`에 저장합니다.
- refresh token은 `localStorage`에 저장합니다.
- API 호출 시 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.
- `(admin)/layout.tsx`에서 `auth_token` cookie 존재 여부를 확인하고, 토큰이 없으면 `/login`으로 이동합니다.
- 개발 편의를 위해 로그인 화면에 `dummy_token`을 저장하는 로그인 건너뛰기 기능이 있습니다.

## 5. 환경 변수 구조

### 로컬 환경

```env
CORE_ADMIN_API_URL=https://dev-admin-core-kyvc.khuoo.synology.me
NEXT_PUBLIC_CORE_ADMIN_API_BASE_URL=
NEXT_PUBLIC_API_BASE_URL=
NEXT_PUBLIC_ADMIN_ACCESS_TOKEN=
```

- `CORE_ADMIN_API_URL`: Next.js rewrite에서 사용하는 Core Admin API 서버 주소입니다.
- `NEXT_PUBLIC_CORE_ADMIN_API_BASE_URL`: 브라우저에서 직접 사용하는 Core Admin API base URL입니다.
- `NEXT_PUBLIC_API_BASE_URL`: 공통 API base fallback입니다.
- `NEXT_PUBLIC_ADMIN_ACCESS_TOKEN`: 개발용 access token fallback입니다.

### dev 환경

- dev 환경에서는 `CORE_ADMIN_API_URL` 또는 `NEXT_PUBLIC_CORE_ADMIN_API_BASE_URL`을 dev Core Admin API 도메인으로 설정합니다.
- `/admin/*`, `/health` 요청은 Core Admin API로 전달됩니다.

### prod 환경

- prod 환경에서는 운영 Core Admin API 도메인을 사용합니다.
- 운영 토큰은 로그인/세션 발급 흐름을 통해 관리하고, public 환경 변수에 고정하지 않습니다.

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
- 개발 중 같은 포트를 다른 서비스가 사용 중이면 `npm run dev -- -p 3001`처럼 포트를 변경합니다.

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

- `frontend_core_admin_api`는 Core Admin API 운영 화면과 API 호출 클라이언트만 담당합니다.
- Core 비즈니스 로직과 API 구현은 `core_admin`에서 처리합니다.
- 일반 사용자 화면은 `frontend`, 일반 관리자 화면은 `frontend_admin`에서 작업합니다.

### API 호출 규칙

- Core Admin API 호출은 `lib/api/core-admin.ts`에 작성합니다.
- API base URL 처리는 `lib/api/api-base.ts`를 사용합니다.
- 화면 컴포넌트에서 API URL을 직접 반복 조립하지 않고 API 모듈 함수를 사용합니다.
- Core Admin 프론트는 `core_admin` API만 호출하며 `backend_admin` 또는 `backend`를 우회 호출하지 않습니다.

### 인증 처리 규칙

- access token 조회는 `lib/auth-session.ts`의 `getAccessTokenForApi()`를 사용합니다.
- 로그인/로그아웃/토큰 갱신은 `lib/api/auth.ts`를 통해 처리합니다.
- 인증이 필요한 화면은 `(admin)` 라우트 그룹 아래에 배치합니다.
- 운영 배포에서는 로그인 건너뛰기 기능을 제거하거나 비활성화해야 합니다.

### 화면/컴포넌트 작성 규칙

- 공통 레이아웃은 `components/layout`을 사용합니다.
- 재사용 가능한 UI 컴포넌트는 `components/ui`에 둡니다.
- App Router 규칙에 맞춰 `app/(admin)/{domain}/page.tsx`를 사용합니다.
- 탭/상세 등 하위 화면은 해당 도메인 디렉터리 아래에 배치합니다.
- 경로 alias는 `@/*`를 사용합니다.
