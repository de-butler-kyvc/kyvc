![alt text](KYvC_Logo.png)

# KYvC


KYvC는 법인 KYC 심사, Verifiable Credential 발급, Verifiable Presentation 검증, DID/자격증명 상태 관리를 하나의 흐름으로 연결하는 프로젝트입니다.

## 1. 프로젝트 개요

### 프로젝트 소개

KYvC는 법인 사용자의 KYC 신청부터 제출서류 검토, AI/수동 심사, VC 발급, VP 검증까지 연결하는 서비스이다. 사용자 서비스, 관리자 서비스, Core 기술 서비스, 배포 인프라를 하나의 저장소에서 관리한다.

### 프로젝트 목적

- 법인 정보와 제출서류 기반 KYC 심사 흐름 디지털화
- 심사 완료 법인에 대한 검증 가능한 자격증명 발급
- 금융사와 외부 Verifier를 위한 VP 요청/제출/검증 구조 제공
- DID, VC/VP, SD-JWT, XRPL, AI 평가 기능을 업무 API와 분리
- 사용자 업무, 관리자 업무, Core 운영, 인프라 책임 경계 명확화

### 서비스 도메인

| 도메인 | 환경 | 대상 서비스 |
| --- | --- | --- |
| `dev-kyvc.khuoo.synology.me` | dev | Synology DSM Reverse Proxy / Frontend 통합 Nginx / 사용자 프론트 |
| `dev-admin-kyvc.khuoo.synology.me` | dev admin | Synology DSM Reverse Proxy / Frontend 통합 Nginx / 백엔드 어드민 프론트 |
| `dev-core-admin-kyvc.khuoo.synology.me` | dev core admin | Synology DSM Reverse Proxy / Frontend 통합 Nginx / 코어 어드민 프론트 |
| `dev-api-kyvc.khuoo.synology.me` | dev | Synology DSM Reverse Proxy / Backend API |
| `dev-core-kyvc.khuoo.synology.me` | dev | Synology DSM Reverse Proxy / Core API |
| `dev-admin-api-kyvc.khuoo.synology.me` | dev admin | Synology DSM Reverse Proxy / Backend Admin API |
| `dev-admin-core-kyvc.khuoo.synology.me` | dev admin | Synology DSM Reverse Proxy / Core Admin API |

### 핵심 기능 요약

- 사용자 KYC: 법인 정보 등록, KYC 신청, 제출서류 업로드, 보완 제출, 진행 상태 조회
- 관리자 심사: KYC 신청 목록/상세, 제출서류 검토, AI 심사 결과 조회, 승인/반려, 보완요청
- Credential: VC 발급 요청, 발급 상태 조회, 재발급/폐기 요청, Credential 이력 관리
- VP Verification: VP 요청, QR/링크 기반 제출, 검증 결과 조회, 외부 Verifier 연동
- Core 기술 기능: DID, VC/VP, SD-JWT, XRPL 상태 관리, OCR/LLM 기반 AI 평가

## 2. 전체 서비스 구성

| 구분 | 구성 서비스 | 주요 책임 | 대표 디렉터리 |
| --- | --- | --- | --- |
| 사용자 서비스 | 사용자 프론트, 사용자 업무 API | 법인 사용자 KYC 신청, 문서 제출, VC/VP 사용자 흐름 | `frontend`, `backend` |
| 관리자 서비스 | 백엔드 어드민 프론트, 백엔드 어드민 API | KYC 심사, 문서 검토, 사용자/법인 조회, 업무 운영 | `frontend_admin`, `backend_admin` |
| Core 서비스 | Core API, Core Admin API, Core Admin 화면 | DID/VC/VP/AI 평가 처리, Core 운영 API와 화면 | `core`, `core_admin`, `frontend_core_admin` |
| 인프라 | Docker, Compose, Frontend 통합 Nginx, Synology DSM Reverse Proxy 기준 | 컨테이너 실행, reverse proxy, 네트워크, 로그/볼륨 기준 | `infra` |
| CI/CD | GitHub Actions, GHCR, self-hosted runner | 브랜치 기준 이미지 빌드와 배포 | `.github/workflows` |

## 3. Monorepo 디렉터리 구조

```text
kyvc/
├── frontend/                 사용자 웹 화면
├── frontend_admin/           백엔드 어드민 웹 화면
├── frontend_core_admin/      Core 운영 관리자 화면
├── frontend_core_admin_api/  Core Admin API 운영 콘솔
├── backend/                  사용자 업무 API
├── backend_admin/            백엔드 어드민 업무 API
├── core/                     DID, VC/VP, SD-JWT, XRPL, AI 평가 Core API
├── core_admin/               Core 운영용 Admin API 어댑터
├── infra/                    Docker, Compose, Nginx, Reverse Proxy 기준
└── .github/                  GitHub Actions workflow와 actionlint 설정
```

## 4. 서비스별 책임 분리

| 서비스 | 책임 | 금지 또는 주의 |
| --- | --- | --- |
| `frontend` | 사용자 KYC, Credential, VP 제출 화면 제공 | 관리자 업무 API 직접 호출 금지 |
| `frontend_admin` | KYC 심사, 문서 검토, 법인/사용자 조회 화면 제공 | Core API 직접 호출 금지 |
| `frontend_core_admin` | Core 운영 화면 제공 | 사용자 업무 화면과 혼합 금지 |
| `backend` | 사용자 인증, KYC 신청, 문서 저장, Core 요청 생성, 업무 DB 동기화 | `/api/admin/**` 관리자 API 구현 금지 |
| `backend_admin` | 관리자 인증, KYC 심사 조회/처리, 업무 DB 기준 운영 API 제공 | CoreAdapter 직접 호출과 Core raw payload 노출 금지 |
| `core` | DID, VC/VP, SD-JWT, XRPL, AI 평가 처리 | 사용자 업무 DB 직접 처리 금지 |
| `core_admin` | Core 운영 API 어댑터, provider/상태/운영 기능 제공 | 백엔드 어드민 업무 API 대체 금지 |
| `infra` | 컨테이너, 네트워크, reverse proxy, 배포 구성 관리 | 서비스 비즈니스 로직 작성 금지 |

## 5. 전체 통신 구조

```text
frontend              -> backend
frontend_admin        -> backend_admin
frontend_core_admin   -> core_admin
backend               -> core
backend_admin         -> core 직접 호출 금지
```

사용자 업무 API는 `backend`, 관리자 업무 API는 `backend_admin`, Core 운영 API는 `core_admin` 기준으로 분리한다. `backend_admin`은 Core를 직접 호출하지 않고, backend 업무 DB에 동기화된 Core 결과를 조회한다.

데이터 책임도 같은 기준을 따른다. 사용자/법인/KYC/문서/심사는 `backend` 업무 DB 기준이고, DID/VC/VP/SD-JWT/XRPL/AI 평가는 `core` 책임이다. 정적 프론트 배포와 reverse proxy 기준은 `infra`에서 관리한다.

## 6. 기술 스택 요약

| 영역 | 주요 기술 | 사용 위치 |
| --- | --- | --- |
| Frontend | Next.js 16, React 19, TypeScript | `frontend`, `frontend_admin`, `frontend_core_admin`, `frontend_core_admin_api` |
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA | `backend`, `backend_admin` |
| Core | Python 3.12, FastAPI, SQLAlchemy, AI/OCR provider 연동 | `core`, `core_admin` |
| Database | PostgreSQL, MySQL, Flyway | 업무 DB, Core DB, migration |
| Infra | Docker, Docker Compose, Nginx, Synology DSM Reverse Proxy | `infra` |
| CI/CD | GitHub Actions, GHCR, self-hosted runner | `.github/workflows` |

## 7. README 문서 구조

| 문서 | 설명 |
| --- | --- |
| [통합 README](./README.md) | 프로젝트 전체 구조, 책임 분리, 통신/배포/Git 기준 |
| [Frontend README](./frontend/README.md) | 사용자 프론트 구조, API 호출, 실행 기준 |
| [Frontend Admin README](./frontend_admin/README.md) | 백엔드 어드민 프론트 구조와 개발 규칙 |
| [Frontend Core Admin API README](./frontend_core_admin_api/README.md) | Core Admin API 운영 콘솔 설명 |
| [Backend README](./backend/README.md) | 사용자 업무 API 구조, DB, 인증, 개발 규칙 |
| [Backend Admin README](./backend_admin/README.md) | 관리자 API 구조, 권한, 심사 업무, 개발 규칙 |
| [Core README](./core/README.md) | Core API와 DID/VC/VP/AI 평가 구조 |
| [Core Admin README](./core_admin/README.md) | Core 운영 API 어댑터 구조 |
| [Infra README](./infra/README.md) | Docker Compose, Nginx, Reverse Proxy, 배포/로그 기준 |

## 8. 배포 구조

배포는 브랜치 기준으로 분리한다. `feature/*`는 기능 개발과 PR 생성을 위한 브랜치이며 자동 배포 대상이 아니다. `develop`은 dev 배포 기준이고, `main`은 prod 배포 기준이다.

dev 배포는 `develop` 병합 이후 dev 이미지 태그와 `infra/compose/dev/docker-compose.yml` 기준으로 진행한다. prod 배포는 `main` 병합 이후 prod 이미지 태그와 `infra/compose/prod/docker-compose.yml` 기준으로 진행한다.

Frontend는 통합 Nginx 이미지로 빌드되며 `frontend`, `frontend_admin`, `frontend_core_admin` 정적 산출물을 포함한다. Backend, Backend Admin, Core, Core Admin은 각각 독립 이미지로 빌드하고 GHCR에 push한 뒤 compose 기준으로 배포한다.

## 9. Git 운영 전략

브랜치는 `feature/*`, `develop`, `main` 기준으로 운영한다. 작업은 `feature/*`에서 진행하고, dev 검증은 `feature/*` -> `develop` PR 병합 후 진행한다. 운영 반영은 `develop` -> `main` PR로 분리한다.

커밋 메시지는 `type(scope): 작업 내용` 형식을 사용한다.

- type: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
- scope: `back`, `back-admin`, `front`, `front-admin`, `core`, `core-admin`, `infra`, `-`
- 예시: `docs(-): 통합 README 구조 정리`

`main`, `develop`에는 직접 push하지 않고 PR 병합만 허용한다. 요청 없는 branch 생성, commit, push, merge, PR 생성은 하지 않는다.
