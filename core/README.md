# KYvC Core

KYvC 코어는 법인 KYC 자격증명의 발급과 검증을 담당하는 파이썬 기반 백엔드 서비스다. 기본 발급 형식은 SD-JWT이며, 기존 호환을 위해 vc+jwt와 vp+jwt 경로도 함께 유지한다.

## 1. 서비스 개요

### 담당 사용자

- 백엔드 연동 개발자
- 지갑 앱 또는 중간 백엔드에서 증명서 발급과 검증을 호출하는 서비스 개발자
- 운영 상태와 공급자 선택을 관리하는 내부 운영자

### 호출 주체

- 프론트엔드
- 백엔드
- 외부 검증 클라이언트
- 내부 운영 도구 또는 배치성 관리 호출
- 테스트 코드 및 로컬 개발 스크립트

### 서비스 역할

- XRPL DID 기반 발급자 DID 생성 및 문서 노출
- KYC 자격증명 발급, 폐기, 상태 조회
- SD-JWT+KB 및 기존 vc+jwt, vp+jwt 검증
- OCR, LLM 공급자 선택 및 문서 인공지능 평가
- 내부 상태 점검과 검증용 챌린지 발급

### 서비스 도메인

- 자격증명: 법인 KYC 자격증명, SD-JWT, vc+jwt
- 검증: 자격증명 검증, 프레젠테이션 검증, 챌린지 관리
- 인공지능 평가: OCR, LLM 기반 문서 판독 및 위임인 비교
- 공급자 선택: OCR, LLM 공급자 활성 선택 이력 관리
- DID: did:xrpl:1:{account}

### 스웨거 주소

- 로컬 기본 주소: http://127.0.0.1:8090/docs
- 오픈API JSON: http://127.0.0.1:8090/openapi.json
- 연동 명세 초안: docs/backend-integration.openapi.yaml

## 2. 기술 스택

### 언어

- 파이썬 3.12

### 프레임워크

- FastAPI
- Pydantic
- Uvicorn

### 빌드 도구

- pip 기반 의존성 관리
- 도커 이미지 빌드
- pytest 기반 테스트 실행

### 데이터베이스

- MySQL 8.4
- 로컬 개발은 docker-compose.local.yml로 MySQL 컨테이너를 실행한다.
- XRPL 원장은 자격증명 상태의 외부 기준 원본으로 사용할 수 있다.

### 주요 라이브러리

- fastapi
- uvicorn[standard]
- pydantic
- httpx
- cryptography
- xrpl-py
- pymysql
- python-dotenv
- python-multipart

## 3. API 구조

### API 그룹

- /health: 헬스체크
- /internal/status: 내부 상태 조회
- /internal/provider-selections: 공급자 옵션, 현재 선택, 변경 이력
- /ai-assessment: 주 LLM 기반 문서 평가
- /dids: DID 문서 조회
- /issuer: 지갑 생성, DID 등록, 자격증명 발급 및 폐기
- /credential-status: 자격증명 상태 조회
- /verifier: 자격증명 검증, 프레젠테이션 챌린지 발급, 프레젠테이션 검증

### 주요 API

- POST /issuer/wallets: XRPL 발급자 지갑 생성 및 시드 반환
- POST /issuer/did/register: 발급자 DID 등록
- POST /issuer/credentials/kyc: KYC 자격증명 발급
- POST /issuer/credentials/revoke: 자격증명 폐기
- GET /credential-status/credentials/{issuer_account}/{holder_account}/{credential_type}: 상태 조회
- POST /verifier/credentials/verify: 자격증명 검증
- POST /verifier/presentations/challenges: 검증용 챌린지 발급
- POST /verifier/presentations/verify: 프레젠테이션 검증
- GET /internal/status: 데이터베이스, XRPL, 발급자 초기화 상태 조회

### 요청/응답 규칙

- 요청과 응답 모델은 Pydantic response_model을 사용한다.
- 검증 실패나 비즈니스 오류는 주로 400으로 반환된다.
- 요청 유효성 검사 오류는 422로 반환된다.
- 처리되지 않은 예외는 500과 detail 필드 형식으로 반환된다.
- 공통 오류 응답 형태는 다음과 같다.

```json
{
  "detail": "오류 메시지"
}
```

- SD-JWT 자격증명은 압축 문자열 형식으로 반환되며, 선택 공개 경로를 함께 응답한다.
- 기존 vc+jwt, vp+jwt 경로는 호환용으로 유지되며 embedded_jws 형식도 일부 허용한다.

## 4. 패키지 구조

### 글로벌

- app/core: 설정 로딩, 환경 변수 매핑
- app/main.py: FastAPI 앱 생성, 라우터 등록, 예외 처리기 등록
- app/logging_config.py: 로깅 설정
- app/resilience: 외부 호출 시간 제한, 재시도, 회로 차단 보조 로직

### 도메인

- app/issuer: 자격증명 발급, 폐기, 발급자 초기화
- app/verifier: 자격증명 및 프레젠테이션 검증
- app/credential_status: 상태 조회 모델 및 서비스
- app/credentials: VC, VP, DID, JWS 유틸리티
- app/sdjwt: SD-JWT 발급, 선택 공개, KB-JWT, 파싱과 검증
- app/status: SD-JWT 상태값 계산
- app/policy: 선택 공개 및 문서 검증 정책
- app/ai_assessment: OCR, LLM 기반 문서 평가
- app/provider_selection: 공급자 선택 정책 및 이력
- app/internal_status: 내부 상태 집계
- app/xrpl: XRPL 트랜잭션, DID, 자격증명 엔트리 연동

### 리소스

- app/api: 외부 노출 FastAPI 라우터 계층
- app/storage: MySQL 저장소와 프로토콜 인터페이스
- docs: 연동 문서와 가이드
- tests: 단위 테스트와 통합 테스트
- storage: 로컬 파일 저장 경로

## 5. DB 구조

### 주요 사용 테이블

- did_documents: DID 문서 캐시 및 로컬 발급자 DID 문서 저장
- issued_credentials: 개발용 임시 발급 이력 테이블
- credential_status: 로컬 상태 미러 저장
- verification_logs: 검증 결과 로그 저장
- verification_challenges: 검증 nonce, domain, 만료 시간 저장
- provider_selections: 현재 활성 OCR, LLM 공급자 설정 저장
- provider_selection_history: 공급자 변경 이력 저장

### 저장소 구조

- 단일 MySQLRepository가 저장소 역할을 수행한다.
- 프로토콜 기반으로 CredentialRepository, StatusRepository, DidDocumentRepository, VerificationLogRepository, VerificationChallengeRepository를 구현한다.
- 앱 시작 시 스키마가 없으면 테이블을 자동 생성한다.
- 저장소 객체는 FastAPI app.state.repository에 주입되어 각 라우터와 서비스에서 공용 사용한다.

### issued_credentials 주의사항

- issued_credentials는 개발과 로컬 검증 편의를 위한 임시 테이블이다.
- 현재 구현은 발급된 자격증명 JSON을 저장할 수 있으므로 개인정보와 KYC 원문에 준하는 민감정보로 취급해야 한다.
- 운영 환경에서는 자격증명 원문을 평문으로 저장하지 않는 것을 원칙으로 한다.
- 운영에 필요한 값은 credential_id, issuer_did, holder_did, credential_type, 상태 식별자, 만료 시각, 폐기 시각 같은 최소 메타데이터로 제한한다.
- 부득이하게 원문 저장이 필요하면 암호화, 짧은 보관 기간, 접근 통제, 감사 로그를 별도로 적용해야 한다.

## 6. 인증/인가 구조

### JWT

- 사용자 세션 인증용 JWT는 현재 구현되어 있지 않다.
- 다만 자격증명 자체는 JOSE JWS 기반 vc+jwt, vp+jwt, dc+sd-jwt, kb+jwt를 사용한다.

### Cookie

- 쿠키 세션 인증은 사용하지 않는다.

### Role

- 애플리케이션 수준 역할 기반 인가 처리는 현재 없다.
- 내부와 외부 API 구분은 라우터 접두사와 호출 주체 관례에 의존한다.

### 내부 API 인증

- /internal 계열 API에 별도 API 키, 베어러 토큰, mTLS 검증은 현재 구현되어 있지 않다.
- 운영 환경에서는 상위 게이트웨이, 비공개 네트워크, 인그레스 정책으로 보호하는 전제가 필요하다.

## 7. 외부 연동 구조

### 코어 연동

- 본 서비스가 KYvC 자격증명 수명주기의 중심이다.
- 프론트엔드 또는 상위 백엔드가 발급, 검증, 문서 평가 API를 호출한다.

### DB 연동

- MySQLRepository가 PyMySQL로 직접 연결한다.
- DID 문서, 발급 자격증명, 검증 챌린지, 공급자 선택 정보를 영속화한다.

### 파일 저장소

- APP_STORAGE_PATH 기본값은 ./storage다.
- 컨테이너에서는 /app/storage를 사용한다.
- 첨부 문서 원본은 API 본문 또는 multipart 첨부로 처리되며, 영구 파일 저장소 연동은 현재 제한적이다.

### 외부 API

- XRPL JSON-RPC: DID 및 자격증명 상태 조회, 생성, 폐기
- XRPL Faucet: 개발망 발급자 지갑 자금 충전
- Azure Document Intelligence: OCR 추출
- OpenAI 또는 Azure OpenAI: 문서 평가 및 LLM 분석
- Naver Clova OCR: OCR 공급자 대안

## 8. 환경 변수 구조

### 로컬 환경

- 기본 앱: APP_NAME, APP_ENV, APP_PORT, LOG_LEVEL, APP_STORAGE_PATH
- DB: DB_HOST, DB_PORT, LOCAL_DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_CHARSET, DB_CONNECT_TIMEOUT
- XRPL: XRPL_JSON_RPC_URL, XRPL_NETWORK_NAME, XRPL_ISSUER_SEED, XRPL_FAUCET_HOST, DID_DOC_BASE_URL, ALLOW_MAINNET
- 초기화: AUTO_CREATE_ISSUER_WALLET_ON_BOOT, AUTO_REGISTER_ISSUER_DID, AUTO_FUND_ISSUER_ON_BOOT
- 검증기: VERIFIER_CHALLENGE_TTL_SECONDS
- 공급자/인공지능: OCR_PROVIDER, LLM_PROVIDER, LLM_MODEL, LLM_MULTIMODAL_ENABLED, LLM_MULTIMODAL_MAX_PAGES
- 평가 정책: OWNERSHIP_THRESHOLD_PERCENT, OWNERSHIP_TOTAL_TOLERANCE_PERCENT, ASSESSMENT_SCHEMA_VERSION, PROMPT_VERSION
- Azure 문서 인식: AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT, AZURE_DOCUMENT_INTELLIGENCE_KEY, AZURE_DOCUMENT_INTELLIGENCE_MODEL_ID, AZURE_DOCUMENT_INTELLIGENCE_API_VERSION
- OpenAI: OPENAI_API_KEY, OPENAI_MODEL, OPENAI_BASE_URL
- Azure OpenAI: AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, AZURE_OPENAI_DEPLOYMENT, AZURE_OPENAI_API_VERSION
- Naver Clova OCR: NAVER_CLOVA_OCR_ENDPOINT, NAVER_CLOVA_OCR_SECRET, NAVER_CLOVA_OCR_TEMPLATE_ID
- 외부 연동 복원력: OUTBOUND_DEFAULT_TIMEOUT_SECONDS, OUTBOUND_XRPL_TIMEOUT_SECONDS, OUTBOUND_DID_RESOLVER_TIMEOUT_SECONDS, OUTBOUND_OCR_TIMEOUT_SECONDS, OUTBOUND_LLM_TIMEOUT_SECONDS, OUTBOUND_RETRY_MAX_ATTEMPTS, OUTBOUND_RETRY_BASE_DELAY_SECONDS, OUTBOUND_RETRY_MAX_DELAY_SECONDS, OUTBOUND_CIRCUIT_FAILURE_THRESHOLD, OUTBOUND_CIRCUIT_RECOVERY_TIMEOUT_SECONDS

### dev 환경

- APP_ENV=dev 기준으로 로컬 MySQL과 XRPL devnet 사용을 권장한다.
- XRPL JSON-RPC 기본값은 devnet 엔드포인트다.
- 외부 OCR, LLM 공급자 사용 시 API 키를 개별 개발자 환경에만 주입한다.

### prod 환경

- APP_ENV=prod와 함께 실운영 DB, 비공개 네트워크, 별도 비밀정보 관리 도구를 사용해야 한다.
- ALLOW_MAINNET는 명시적으로 필요할 때만 활성화한다.
- /internal API는 게이트웨이 또는 네트워크 수준에서 반드시 차단 또는 제한해야 한다.

## 9. 실행 구조

### 로컬 실행

1. 가상환경을 생성하고 의존성을 설치한다.
2. MySQL 로컬 컨테이너를 실행한다.
3. uvicorn으로 API 서버를 구동한다.

```bash
cd core
python -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
docker compose -f docker-compose.local.yml up -d mysql
uvicorn app.main:app --reload --host 0.0.0.0 --port 8090
```

### 도커 실행

```bash
cd core
docker build -t kyvc-core .
docker run --rm -p 8090:8090 --env-file .env kyvc-core
```

- 컨테이너 기본 포트는 8090이다.
- Dockerfile은 python:3.12-slim 기반이며 requirements.txt를 먼저 설치한다.

### 빌드

- 파이썬 서비스라 별도 컴파일 단계는 없다.
- 배포 단위는 도커 이미지 빌드 또는 requirements 기반 런타임 환경이다.
- 테스트 실행 예시는 다음과 같다.

```bash
cd core
source .venv/bin/activate
pytest
```

## 10. 개발 규칙

### 디렉터리 작업 범위

- API 엔드포인트 변경은 app/api와 해당 도메인 패키지를 함께 수정한다.
- 영속성 변경은 app/storage와 관련 서비스, 테스트를 함께 수정한다.
- 문서와 연동 명세 변경 시 docs/backend-integration.openapi.yaml도 같이 점검한다.

### 응답 포맷

- 성공 응답은 각 API별 Pydantic 모델을 따른다.
- 오류 응답은 detail 필드 단일 객체 또는 유효성 검사 오류 배열을 사용한다.
- 신규 API를 추가할 때도 기존 FastAPI response_model과 동일한 형식을 유지한다.

### 예외 처리

- HTTPException은 상태 코드와 detail을 그대로 반환한다.
- ValueError와 요청 처리형 RuntimeError는 400으로 매핑한다.
- RequestValidationError는 422로 반환한다.
- 처리되지 않은 예외는 500 내부 서버 오류로 통일한다.

## 참고

- SD-JWT 중심 홀더 흐름 가이드는 docs/android-holder-wallet-sdjwt-guide.md를 참고한다.
- 안드로이드 홀더 지갑 통합 가이드는 docs/android-holder-wallet-guide.md를 참고한다.
- 테스트 범위는 발급, 검증, 내부 상태, 공급자 선택, 외부 연동 복원력, 인공지능 평가를 포함한다.
