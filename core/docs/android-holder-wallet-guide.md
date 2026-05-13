# Android Holder Wallet 연동 가이드

> 신규 legal entity KYC wallet은 SD-JWT+KB 흐름을 기준으로 구현한다. SD-JWT 전용 가이드는 [`android-holder-wallet-sdjwt-guide.md`](android-holder-wallet-sdjwt-guide.md)를 먼저 참고한다. 이 문서는 legacy `vc+jwt`/`vp+jwt` 호환 흐름을 설명한다.

이 문서는 Android holder wallet 개발자가 KYvC `core` issuer/verifier와 연동할 때 필요한 계약을 정리한다. 현재 `core`는 holder 전용 API를 제공하지 않는다. Android 앱은 holder XRPL 계정과 holder 인증 키를 보관하고, issuer가 발급한 `vc+jwt`를 저장한 뒤 XRPL `CredentialAccept`를 직접 제출한다.

현재 기본 보안 포맷은 W3C VC Data Model 2.0 및 W3C VC-JOSE-COSE에 맞춘 JWT 기반 secured representation이다.

- VC: compact `application/vc+jwt`
- VP: compact `application/vp+jwt`
- VP 내부 VC: `EnvelopedVerifiableCredential`
- 서명 알고리즘: JOSE `ES256K`, secp256k1 + SHA-256
- DID: `did:xrpl:1:{xrplAccount}`

`proof.jws`가 들어간 expanded JSON 포맷은 compatibility mode로만 남아 있다. 신규 Android wallet은 사용하지 않는다.

참고 코드:

- `app/credentials/vc.py`: VC JWT, Enveloped VC, compatibility helper
- `app/credentials/vp.py`: VP JWT, Enveloped VP, VP 내부 VC envelope
- `app/verifier/service.py`: JWT 중심 VC/VP 검증 흐름
- `app/xrpl/ledger.py`: XRPL `CredentialCreate`, `CredentialAccept`, `CredentialDelete`, status 조회
- `holder-test/test_holder_flow.py`: holder 역할 end-to-end 예제

## 1. Wallet 책임

Android 앱은 다음 데이터를 생성하거나 보관한다.

| 데이터 | 용도 | 권장 보관 |
| --- | --- | --- |
| XRPL holder seed/account | `CredentialAccept` 트랜잭션 서명 | Keystore/StrongBox 래핑 키로 암호화한 저장소 |
| holder DID | `did:xrpl:1:{holderAccount}` | 앱 DB |
| holder 인증 키 | `vp+jwt` 서명, DID `authentication` method | 가능한 경우 hardware-backed, 아니면 암호화 저장 |
| holder DID Document | verifier가 VP 서명을 검증할 때 사용 | 앱 DB 및 verifier 제출용 JSON |
| VC JWT | issuer가 발급한 `application/vc+jwt` | 앱 DB |
| credentialType | XRPL credential ledger entry 식별 | VC JWT payload의 `credentialStatus.credentialType` |
| issuer/holder account | status 조회, accept, revoke 확인 | VC JWT payload에서 파싱 후 앱 DB |
| tx hash | accept 제출 추적 | 앱 DB |

## 2. 전체 플로우

```text
1. 앱이 XRPL holder account를 생성하거나 복구한다.
2. 앱이 holder 인증용 secp256k1 key pair를 생성한다.
3. 앱이 holder DID와 holder DID Document를 만든다.
4. 앱 또는 issuer frontend가 issuer backend에 holder_account, holder_did를 전달한다.
5. issuer backend가 core /issuer/credentials/kyc를 호출한다.
6. core가 VC를 vc+jwt로 발급하고 XRPL CredentialCreate를 제출한다.
7. 앱이 vc+jwt를 수신, 서명과 payload를 검증한 뒤 저장한다.
8. 앱이 XRPL CredentialAccept를 holder 계정으로 서명/제출한다.
9. verifier 제출 시 앱이 challenge를 받아 vp+jwt를 생성한다.
10. 앱이 vp+jwt와 holder DID Document를 verifier API에 제출한다.
```

XRPL status 규칙:

- issuer가 `CredentialCreate`를 제출한 직후에는 VC가 아직 active가 아니다.
- holder가 `CredentialAccept`를 제출해야 active가 된다.
- issuer가 `CredentialDelete`를 제출하면 inactive가 된다.
- verifier는 기본적으로 XRPL ledger entry를 authoritative status source로 사용한다.

## 3. DID와 Key

holder DID는 고정 형식이다.

```text
did:xrpl:1:{holderAccount}
```

holder account가 `rHolder...`이면 DID는 다음과 같다.

```text
did:xrpl:1:rHolder...
```

holder 인증 키는 secp256k1 EC key다. DID Document는 다음 구조를 사용한다.

```json
{
  "@context": [
    "https://www.w3.org/ns/did/v1",
    "https://w3id.org/security/jwk/v1"
  ],
  "id": "did:xrpl:1:rHolder...",
  "verificationMethod": [
    {
      "id": "did:xrpl:1:rHolder...#holder-key-1",
      "type": "JsonWebKey",
      "controller": "did:xrpl:1:rHolder...",
      "publicKeyJwk": {
        "kty": "EC",
        "crv": "secp256k1",
        "x": "...base64url-no-padding...",
        "y": "...base64url-no-padding..."
      }
    }
  ],
  "authentication": [
    "did:xrpl:1:rHolder...#holder-key-1"
  ]
}
```

주의:

- `crv`는 정확히 `secp256k1`이다.
- `x`, `y`는 32-byte big-endian 좌표를 base64url no-padding으로 인코딩한다.
- VP JWT header의 `kid`는 `authentication` 배열에 들어 있는 verification method여야 한다.
- issuer VC JWT header의 `kid`는 issuer DID Document의 `assertionMethod`에 들어 있어야 한다.

## 4. DID Document 신뢰 모델

Verifier의 DID resolution 우선순위는 다음과 같다.

1. 요청에 포함된 `did_documents`
2. core MySQL에 저장된 DID Document cache
3. XRPL DID entry 조회, ledger `URI` fetch, ledger `Data` hash 검증

단, XRPL client를 사용할 수 있는 검증에서는 요청에 포함된 DID Document와 local DB cache를 그대로 신뢰하지 않는다. verifier는 같은 DID의 XRPL DID entry를 조회하고, 문서의 `multihash_sha2_256(canonical_json(didDocument))` 값이 ledger `Data`와 일치할 때만 사용한다.

Android 앱 관점의 의미:

- 우리 issuer DID Document는 보통 issuer API가 core DB에 저장하므로 verifier가 먼저 로컬에서 찾는다. 운영 XRPL 검증에서는 이 로컬 문서도 ledger `Data` hash와 일치해야 사용된다.
- 외부 issuer DID는 core DB에 없으면 XRPL DID entry의 `URI`에서 가져오고 `Data` hash로 검증된다.
- local cache가 ledger `Data`와 다르면 verifier는 캐시를 건너뛰고 XRPL `URI` fetch로 최신 문서를 다시 가져오려고 시도한다.
- holder DID Document를 `did_documents`에 같이 보내도 된다. 다만 운영 XRPL 모드에서는 ledger `Data` hash와 일치해야 검증에 사용된다.
- holder DID를 XRPL에 `DIDSet`으로 등록하지 않은 상태라면 운영 verifier에서 실패할 수 있다. 로컬 개발 모드에서만 static DID Document를 그대로 쓸 수 있다.

## 5. VC 발급 수신

Android 앱은 issuer seed나 issuer private key를 다루지 않는다. 발급 요청은 issuer backend 또는 issuer 운영 API가 `core`로 보낸다.

```http
POST /issuer/credentials/kyc
Content-Type: application/json
```

요청 예시:

```json
{
  "holder_account": "rHolder...",
  "holder_did": "did:xrpl:1:rHolder...",
  "claims": {
    "kycLevel": "BASIC",
    "jurisdiction": "KR"
  },
  "valid_from": "2026-05-02T00:00:00Z",
  "valid_until": "2026-06-02T00:00:00Z"
}
```

기본 응답의 `credential`은 JSON 객체가 아니라 compact JWT 문자열이다.

```json
{
  "credential": "eyJhbGciOiJFUzI1Nksi...",
  "credential_type": "56435F5354415455535F56313A...",
  "vc_core_hash": "...",
  "credential_create_transaction": {},
  "ledger_entry": {},
  "status_mode": "xrpl"
}
```

Android 앱이 저장해야 하는 최소 값:

- `credential`: VC JWT 원문
- `credential_type`: XRPL `CredentialType`
- `vc_core_hash`
- issuer account
- holder account
- VC `id`
- `validFrom`, `validUntil`

issuer account, holder account, VC `id` 등은 VC JWT payload를 decode해서 얻는다.

## 6. VC JWT 구조

VC JWT는 compact JWS다.

```text
BASE64URL(protected-header).BASE64URL(payload).BASE64URL(signature)
```

protected header 예시:

```json
{
  "alg": "ES256K",
  "typ": "vc+jwt",
  "cty": "vc",
  "kid": "did:xrpl:1:rIssuer...#issuer-key-1",
  "iss": "did:xrpl:1:rIssuer..."
}
```

payload 예시:

```json
{
  "@context": [
    "https://www.w3.org/ns/credentials/v2",
    "https://example.com/contexts/kyc-v1"
  ],
  "id": "urn:uuid:...",
  "type": ["VerifiableCredential", "KycCredential"],
  "issuer": "did:xrpl:1:rIssuer...",
  "validFrom": "2026-05-02T00:00:00Z",
  "validUntil": "2026-06-02T00:00:00Z",
  "credentialSalt": "...",
  "credentialSubject": {
    "id": "did:xrpl:1:rHolder...",
    "kycLevel": "BASIC",
    "jurisdiction": "KR"
  },
  "credentialStatus": {
    "id": "xrpl:credential:rIssuer...:rHolder...:56435F...",
    "type": "XRPLCredentialStatus",
    "statusPurpose": "revocation",
    "issuer": "rIssuer...",
    "subject": "rHolder...",
    "credentialType": "56435F5354415455535F56313A...",
    "vcCoreHash": "..."
  }
}
```

수신 직후 앱 검증 체크리스트:

- JWS header `alg == "ES256K"`
- JWS header `typ == "vc+jwt"`
- JWS header `cty == "vc"`
- JWS header `iss == payload.issuer`
- `payload.credentialSubject.id == holderDid`
- `payload.credentialStatus.subject == holderAccount`
- `payload.credentialStatus.issuer == account_from_did(payload.issuer)`
- `payload.credentialStatus.credentialType == issueResponse.credential_type`
- `validFrom <= now <= validUntil`
- issuer DID Document를 구할 수 있으면 `kid`의 `publicKeyJwk`로 JWS signature 검증

`credentialType`은 XRPL status와 연결되는 핵심 값이다. Android에서 직접 재계산할 필요는 없다. 재계산해야 한다면 `core`와 동일한 deterministic JSON 규칙과 `credentialStatus`, `proof` 제외 규칙을 정확히 맞춰야 한다.

## 7. ES256K JWS 구현 주의사항

JOSE `ES256K` signature는 64-byte raw ECDSA signature다.

```text
signature = R(32 bytes) || S(32 bytes)
```

많은 Java/Android ECDSA API는 DER signature를 반환한다. JWS에 넣기 전 DER를 raw `R || S`로 변환해야 한다. 검증할 때도 JWS raw signature를 crypto provider가 요구하는 DER로 변환해야 할 수 있다.

signing input:

```text
ascii(BASE64URL(protected-header) + "." + BASE64URL(payload))
```

base64url은 padding `=`을 제거한다.

Kotlin 의사 코드:

```kotlin
fun base64UrlNoPadding(bytes: ByteArray): String =
    Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

fun jwsSigningInput(protectedJson: ByteArray, payloadJson: ByteArray): ByteArray {
    val protected = base64UrlNoPadding(protectedJson)
    val payload = base64UrlNoPadding(payloadJson)
    return "$protected.$payload".toByteArray(Charsets.US_ASCII)
}

fun compactJws(protectedJson: ByteArray, payloadJson: ByteArray, rawSignature64: ByteArray): String {
    val protected = base64UrlNoPadding(protectedJson)
    val payload = base64UrlNoPadding(payloadJson)
    val signature = base64UrlNoPadding(rawSignature64)
    return "$protected.$payload.$signature"
}
```

운영 전 확인할 것:

- 사용 crypto provider가 secp256k1 key generation/sign/verify를 지원하는지 확인한다.
- Android Keystore가 secp256k1을 직접 지원하지 않는 기기가 많을 수 있다.
- hardware-backed가 어렵다면 앱 내부 암호화 저장소와 백업/복구 정책을 별도로 설계한다.

## 8. CredentialAccept 제출

Holder가 VC를 수락하려면 Android 앱이 holder XRPL wallet으로 `CredentialAccept`를 서명하고 제출한다.

트랜잭션 필드:

```json
{
  "TransactionType": "CredentialAccept",
  "Account": "rHolder...",
  "Issuer": "rIssuer...",
  "CredentialType": "56435F5354415455535F56313A..."
}
```

입력값 매핑:

| XRPL 필드 | 값 |
| --- | --- |
| `Account` | holder XRPL account |
| `Issuer` | VC JWT payload `issuer` DID에서 account 추출 |
| `CredentialType` | VC JWT payload `credentialStatus.credentialType` |

제출 전 검증:

- holder seed에서 복원한 account가 `credentialStatus.subject`와 같아야 한다.
- `CredentialType`은 issuer 응답 및 VC payload의 값을 그대로 사용한다.
- issuer account는 `did:xrpl:1:{account}`에서 `{account}`만 추출한다.

제출 후 tx hash를 저장하고 status를 다시 조회한다.

```http
GET /credential-status/credentials/{issuerAccount}/{holderAccount}/{credentialType}
```

응답 예시:

```json
{
  "issuer_account": "rIssuer...",
  "holder_account": "rHolder...",
  "credential_type": "56435F5354415455535F56313A...",
  "found": true,
  "active": true,
  "entry": {},
  "checked_at": "2026-05-02T00:00:00Z"
}
```

`active == true` 조건:

- ledger entry가 존재한다.
- `Flags`에 accepted bit `0x00010000`이 설정되어 있다.
- `Expiration`이 있으면 현재 시간보다 미래다.

## 9. VP 생성

Verifier 제출 시 앱은 challenge를 먼저 발급받는다.

```http
POST /verifier/presentations/challenges
Content-Type: application/json
```

요청:

```json
{
  "domain": "example.com"
}
```

응답:

```json
{
  "challenge": "...",
  "domain": "example.com",
  "expires_at": "2026-05-02T00:05:00Z"
}
```

앱은 VP payload를 만든다. VP 내부 `verifiableCredential`에는 raw VC payload가 아니라 Enveloped VC 객체를 넣는다.

```json
{
  "@context": ["https://www.w3.org/ns/credentials/v2"],
  "type": ["VerifiablePresentation"],
  "holder": "did:xrpl:1:rHolder...",
  "verifiableCredential": [
    {
      "@context": "https://www.w3.org/ns/credentials/v2",
      "id": "data:application/vc+jwt,eyJhbGciOiJFUzI1Nksi...",
      "type": "EnvelopedVerifiableCredential"
    }
  ]
}
```

VP JWT protected header:

```json
{
  "alg": "ES256K",
  "typ": "vp+jwt",
  "cty": "vp",
  "kid": "did:xrpl:1:rHolder...#holder-key-1",
  "challenge": "...",
  "domain": "example.com"
}
```

`challenge`와 `domain`은 protected header에 들어가야 한다. verifier는 이 값을 challenge 저장소와 비교하고, 성공하면 challenge를 used 처리한다. 재사용하면 실패한다.

VP JWT 생성 의사 코드:

```kotlin
val envelopedVc = mapOf(
    "@context" to "https://www.w3.org/ns/credentials/v2",
    "id" to "data:application/vc+jwt,$vcJwt",
    "type" to "EnvelopedVerifiableCredential"
)

val vpPayload = mapOf(
    "@context" to listOf("https://www.w3.org/ns/credentials/v2"),
    "type" to listOf("VerifiablePresentation"),
    "holder" to holderDid,
    "verifiableCredential" to listOf(envelopedVc)
)

val protectedHeader = mapOf(
    "alg" to "ES256K",
    "typ" to "vp+jwt",
    "cty" to "vp",
    "kid" to "$holderDid#holder-key-1",
    "challenge" to challenge,
    "domain" to domain
)

val signingInput = jwsSigningInput(
    canonicalJson(protectedHeader),
    canonicalJson(vpPayload)
)
val derSignature = secp256k1SignSha256(holderAuthPrivateKey, signingInput)
val rawSignature = derEcdsaToJoseRaw64(derSignature)
val vpJwt = compactJws(canonicalJson(protectedHeader), canonicalJson(vpPayload), rawSignature)
```

JWS는 실제 전송하는 protected header segment와 payload segment를 그대로 서명한다. Android에서 자체 JOSE 라이브러리를 쓰는 경우, 라이브러리가 만든 signing input과 signature가 최종 compact JWT와 일치하면 된다. `core`와 같은 테스트 벡터를 만들거나 직접 구현한다면 key 정렬, 공백 제거, UTF-8 기반 deterministic JSON을 사용하는 편이 디버깅하기 쉽다.

## 10. VP 제출

Verifier API에는 `vp+jwt` 문자열과 holder DID Document를 제출한다. Issuer DID Document는 issuer API에서 서버에 저장되어 있으면 따로 넘기지 않아도 된다. 모바일 앱이 holder DID Document를 아직 서버에 등록하지 않는 구조라면 요청의 `did_documents`에 holder DID Document를 넣는다.

```http
POST /verifier/presentations/verify
Content-Type: application/json
```

```json
{
  "presentation": "eyJhbGciOiJFUzI1Nksi...",
  "did_documents": {
    "did:xrpl:1:rHolder...": {
      "@context": [
        "https://www.w3.org/ns/did/v1",
        "https://w3id.org/security/jwk/v1"
      ],
      "id": "did:xrpl:1:rHolder...",
      "verificationMethod": [],
      "authentication": []
    }
  },
  "policy": {
    "trustedIssuers": ["did:xrpl:1:rIssuer..."],
    "acceptedKycLevels": ["BASIC"],
    "acceptedJurisdictions": ["KR"]
  },
  "status_mode": "xrpl",
  "require_status": true
}
```

성공 응답:

```json
{
  "ok": true,
  "errors": [],
  "details": {
    "mediaType": "vp+jwt",
    "challengeFound": true,
    "challengeUsed": false,
    "vc_0": {
      "ok": true,
      "errors": [],
      "details": {
        "mediaType": "vc+jwt",
        "credentialAccepted": true
      }
    }
  }
}
```

자주 보는 실패:

| 오류 | 원인 |
| --- | --- |
| `VP challenge was not issued by verifier` | challenge가 verifier에서 발급되지 않았거나 저장되지 않음 |
| `VP challenge was already used` | 같은 challenge 재사용 |
| `VP challenge is expired` | challenge 만료 |
| `VP domain mismatch` | VP JWT header `domain`과 challenge 발급 domain 불일치 |
| `VP signature verification failed` | holder 인증 키, JWS serialization, raw signature 변환 문제 |
| `embedded VC 0 failed verification` | VP 내부 VC JWT 검증, status, policy 중 하나 실패 |
| `XRPL Credential status is not active` | `CredentialAccept` 미제출, revoke, expire, ledger 조회 실패 |

## 11. 로컬 데이터 모델 예시

```kotlin
data class StoredCredential(
    val vcJwt: String,
    val vcId: String,
    val issuerDid: String,
    val issuerAccount: String,
    val holderDid: String,
    val holderAccount: String,
    val credentialType: String,
    val vcCoreHash: String,
    val validFrom: Instant,
    val validUntil: Instant,
    val acceptedTxHash: String?,
    val acceptedAt: Instant?
)

data class HolderIdentity(
    val holderAccount: String,
    val holderDid: String,
    val authKeyId: String,
    val publicJwkJson: String,
    val didDocumentJson: String
)
```

## 12. 개발 검증 순서

먼저 core의 Python holder runner로 네트워크와 issuer/verifier 설정이 맞는지 확인한다.

```bash
cd core
PYTHONPATH=. .venv/bin/python holder-test/test_holder_flow.py
```

Android 구현 검증 순서:

1. holder XRPL account 생성 또는 복구
2. holder secp256k1 인증 키 생성
3. holder DID Document 생성
4. issuer 발급 응답에서 `vc+jwt` 수신
5. VC JWT decode 및 signature 검증
6. `CredentialAccept` 제출
7. `/credential-status/credentials/...` active 확인
8. challenge 발급
9. `vp+jwt` 생성
10. `/verifier/presentations/verify` 성공 확인
11. 같은 VP 재제출 시 challenge reuse로 실패하는지 확인

## 13. 운영 주의사항

- holder XRPL seed와 holder 인증 키의 백업/복구 정책을 먼저 확정한다.
- Android Keystore secp256k1 지원이 제한적일 수 있으므로 대상 OS/기기별 crypto provider 검증이 필요하다.
- VC JWT payload는 signature 검증 전에는 신뢰하지 않는다.
- `CredentialAccept` tx 제출 성공과 ledger active 상태는 별도로 관리한다.
- challenge는 1회용이다. verifier 성공 후 같은 challenge는 재사용할 수 없다.
- `credentialType`은 status lookup의 primary key이므로 문자열을 변형하지 않는다.
- 신규 구현은 `embedded_jws` compatibility mode를 사용하지 않는다.

## 14. 참고 표준

- W3C VC Data Model 2.0: `https://www.w3.org/TR/vc-data-model-2.0/`
- W3C VC-JOSE-COSE: `https://www.w3.org/TR/vc-jose-cose/`
- RFC 7515 JSON Web Signature
- RFC 7519 JSON Web Token
- RFC 8812 ES256K
