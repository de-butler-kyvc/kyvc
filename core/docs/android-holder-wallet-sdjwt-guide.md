# Android Holder Wallet SD-JWT 연동 가이드

이 문서는 Android holder wallet 개발자가 KYvC Core의 신규 `dc+sd-jwt` 및 SD-JWT+KB presentation 흐름을 구현할 때 필요한 계약을 정리한다.

기존 `vc+jwt`/`vp+jwt` 흐름은 호환용으로 남아 있지만, 신규 legal entity KYC wallet은 이 문서의 SD-JWT 흐름을 기준으로 구현한다.

## 1. 핵심 요약

포맷:

- Credential: `dc+sd-jwt`
- Presentation: SD-JWT+KB
- Credential serialization: `<issuer-jwt>~<disclosure-1>~<disclosure-2>~...`
- Presentation serialization: `<issuer-jwt>~<selected-disclosure-1>~...~<kb-jwt>`
- Issuer signature: JOSE compact JWS, `ES256K`
- Holder binding: KB-JWT, holder DID `authentication` key
- Status: XRPL Credential, `credentialStatus.credentialType`

중요한 차이:

- SD-JWT presentation에서는 holder-signed `vp+jwt`를 만들지 않는다.
- verifier challenge의 `nonce`가 기존 `challenge`를 대체한다.
- verifier audience의 `aud`가 기존 `domain`을 대체한다.
- `sd_hash`가 issuer JWT와 선택 disclosure set 전체를 KB-JWT에 묶는다.
- 원본 PDF/image는 SD-JWT에 넣지 않는다. 필요 시 multipart attachment로 별도 제출한다.

## 2. Wallet이 보관해야 하는 값

| 데이터 | 용도 | 권장 보관 |
| --- | --- | --- |
| XRPL holder seed/account | `CredentialAccept` 서명 | Keystore/StrongBox wrapping key로 암호화 |
| holder DID | `did:xrpl:1:{holderAccount}` | 앱 DB |
| holder auth key | KB-JWT 서명 | 가능한 경우 hardware-backed, 아니면 앱 암호화 저장소 |
| holder DID Document | verifier가 KB-JWT 검증 시 사용 | 앱 DB 및 verifier 제출용 JSON |
| SD-JWT credential 원문 | issuer JWT + 전체 disclosure 저장 | 앱 DB 암호화 저장 |
| disclosure 목록 | 선택 제출 UI 및 presentation 생성 | credential 원문에서 파싱 가능 |
| credentialType | XRPL CredentialAccept/status lookup | issuer JWT payload `credentialStatus.credentialType` |
| issuer/holder account | status lookup, accept, revoke 확인 | issuer JWT payload에서 파싱 |
| accepted tx hash | holder accept 추적 | 앱 DB |

민감정보 주의:

- SD-JWT credential 원문에는 전체 disclosure가 들어 있다. 앱 저장소 관점에서는 전체 KYC 원문과 같은 민감도로 취급한다.
- verifier에 제출하는 것은 선택된 disclosure만이다.

## 3. 전체 플로우

```text
1. 앱이 XRPL holder account를 생성하거나 복구한다.
2. 앱이 holder authentication secp256k1 key pair를 생성한다.
3. 앱이 holder DID와 DID Document를 만든다.
4. issuer backend가 Core /issuer/credentials/kyc에 holder_account, holder_did, holder_key_id를 전달한다.
5. Core가 dc+sd-jwt credential을 발급하고 XRPL CredentialCreate를 제출한다.
6. 앱이 SD-JWT credential을 수신해 issuer JWT와 disclosures를 저장한다.
7. 앱이 XRPL CredentialAccept를 holder 계정으로 제출한다.
8. verifier 제출 시 앱이 nonce/aud challenge를 받는다.
9. 앱이 제출할 disclosure만 고른다.
10. 앱이 selected SD-JWT string의 sd_hash를 계산하고 KB-JWT를 holder auth key로 서명한다.
11. 앱이 SD-JWT+KB presentation과 holder DID Document를 verifier에 제출한다.
```

XRPL status 규칙:

- `CredentialCreate` 직후에는 active가 아니다.
- holder가 `CredentialAccept`를 제출해야 active가 된다.
- issuer가 `CredentialDelete`를 제출하면 inactive/revoked가 된다.
- status는 active/revoked lookup key일 뿐이다. tamper-proofing은 issuer signature, disclosure digest, KB-JWT signature, `sd_hash`, `nonce`, `aud`가 담당한다.

## 4. DID와 Holder Key

holder DID:

```text
did:xrpl:1:{holderAccount}
```

DID Document 예시:

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

체크리스트:

- `crv`는 `secp256k1`.
- `x`, `y`는 32-byte big-endian 좌표를 base64url no-padding으로 인코딩한다.
- issuer JWT payload의 `cnf.kid`는 holder authentication method URL이어야 한다.
- verifier는 KB-JWT header `kid`가 DID Document `authentication`에 있는지 확인한다.

## 5. SD-JWT Credential 발급 응답

issuer backend 또는 운영 API가 Core로 요청한다. Android 앱은 issuer seed/private key를 다루지 않는다.

```http
POST /issuer/credentials/kyc
Content-Type: application/json
```

요청 예시:

```json
{
  "format": "dc+sd-jwt",
  "holder_account": "rHolder...",
  "holder_did": "did:xrpl:1:rHolder...",
  "holder_key_id": "holder-key-1",
  "claims": {
    "kyc": {
      "jurisdiction": "KR",
      "assuranceLevel": "STANDARD"
    },
    "legalEntity": {
      "type": "STOCK_COMPANY",
      "name": "KYvC Labs",
      "registrationNumber": "110111-1234567"
    },
    "representative": {
      "name": "Kim Holder",
      "birthDate": "1980-01-01",
      "nationality": "KR"
    },
    "beneficialOwners": [
      {
        "name": "Owner One",
        "birthDate": "1975-02-03",
        "nationality": "KR",
        "ownershipPercentage": 35
      }
    ],
    "documentEvidence": [
      {
        "documentId": "urn:kyvc:doc:registry:001",
        "documentType": "KR_CORPORATE_REGISTER_FULL_CERTIFICATE",
        "documentClass": "ENTITY_REALNAME_EVIDENCE",
        "digestSRI": "sha384-...",
        "mediaType": "application/pdf",
        "byteSize": 482913,
        "hashInput": "original-file-bytes",
        "evidenceFor": [
          "legalEntity.name",
          "legalEntity.registrationNumber"
        ]
      }
    ]
  },
  "valid_from": "2026-05-05T00:00:00Z",
  "valid_until": "2027-05-05T00:00:00Z"
}
```

응답 예시:

```json
{
  "format": "dc+sd-jwt",
  "credential": "<issuer-jwt>~<disclosure-1>~<disclosure-2>",
  "credentialId": "urn:uuid:...",
  "credential_type": "75525E464C1A129413213106E09282DADED49CE9BAB3F35B2AFCC91928660A03",
  "status": {
    "type": "XRPLCredentialStatus",
    "statusId": "urn:kyvc:status:...",
    "credentialType": "75525E464C1A129413213106E09282DADED49CE9BAB3F35B2AFCC91928660A03"
  },
  "selectiveDisclosure": {
    "disclosablePaths": [
      "legalEntity.type",
      "representative.name",
      "beneficialOwners[]",
      "documentEvidence[]"
    ]
  }
}
```

Android 앱이 저장할 최소 값:

- `credential` 원문
- `credentialId`
- `credential_type`
- issuer JWT header/payload decode 결과
- disclosures 배열
- accepted tx hash와 accepted timestamp

## 6. SD-JWT 파싱

credential 문자열은 `~`로 split한다.

```text
parts[0] = issuer-signed JWT
parts[1..n] = disclosures
```

issuer JWT는 compact JWS다.

```text
BASE64URL(header).BASE64URL(payload).BASE64URL(signature)
```

issuer JWT header 예시:

```json
{
  "alg": "ES256K",
  "typ": "dc+sd-jwt",
  "kid": "did:xrpl:1:rIssuer...#issuer-key-1",
  "iss": "did:xrpl:1:rIssuer..."
}
```

issuer JWT payload 예시:

```json
{
  "iss": "did:xrpl:1:rIssuer...",
  "sub": "did:xrpl:1:rHolder...",
  "vct": "https://kyvc.example/vct/legal-entity-kyc-v1",
  "jti": "urn:uuid:...",
  "iat": 1777939200,
  "exp": 1809475200,
  "cnf": {
    "kid": "did:xrpl:1:rHolder...#holder-key-1"
  },
  "credentialStatus": {
    "type": "XRPLCredentialStatus",
    "statusId": "urn:kyvc:status:...",
    "credentialType": "75525E..."
  },
  "kyc": {
    "jurisdiction": "KR",
    "assuranceLevel": "STANDARD",
    "_sd": ["..."]
  },
  "legalEntity": {
    "_sd": ["...", "..."]
  },
  "beneficialOwners": [
    {"...": "..."}
  ],
  "documentEvidence": [
    {"...": "..."}
  ]
}
```

Disclosure는 base64url no-padding 인코딩된 JSON array다.

Object property disclosure:

```json
["salt", "name", "Kim Holder"]
```

Array element disclosure:

```json
["salt", {"name": "Owner One", "birthDate": "1975-02-03"}]
```

Disclosure digest:

```text
digest = BASE64URL_NO_PADDING(SHA-256(ASCII(disclosure)))
```

검증 규칙:

- disclosure가 malformed면 사용하지 않는다.
- 같은 disclosure digest가 중복되면 실패 처리한다.
- issuer JWT payload가 reference하지 않는 disclosure는 제출하지 않는다.
- 앱 UI는 disclosure decode 결과를 사용자에게 보여주고 선택하게 할 수 있다.
- verifier 제출 전 앱도 digest reference 검증을 해두면 UX가 좋아진다.

## 7. Credential 수신 직후 검증

앱에서 최소한 확인할 것:

- issuer JWT header `alg == "ES256K"`.
- issuer JWT header `typ == "dc+sd-jwt"`.
- issuer JWT header `iss == payload.iss`.
- `payload.sub == holderDid`.
- `payload.cnf.kid == "$holderDid#holder-key-1"` 또는 앱이 생성한 holder key id.
- `payload.credentialStatus.type == "XRPLCredentialStatus"`.
- `issueResponse.credential_type == payload.credentialStatus.credentialType`.
- `iat <= now <= exp`.
- issuer DID Document를 얻을 수 있으면 `kid`의 `publicKeyJwk`로 issuer JWS signature를 검증한다.

status credentialType 재계산이 필요한 경우:

```text
credentialType = UPPERCASE_HEX(SHA-256(
  UTF8("SDJWT_STATUS_V1:" + iss + "\u001f" + sub + "\u001f" + vct + "\u001f" + jti)
))
```

Core는 XRPL adapter 호환을 위해 uppercase hex를 사용한다. Android 앱은 문자열 case를 바꾸지 말고 issuer JWT payload 값을 그대로 보관한다.

## 8. CredentialAccept 제출

Holder가 credential을 수락하려면 Android 앱이 holder XRPL wallet으로 `CredentialAccept`를 제출한다.

트랜잭션 필드:

```json
{
  "TransactionType": "CredentialAccept",
  "Account": "rHolder...",
  "Issuer": "rIssuer...",
  "CredentialType": "75525E..."
}
```

입력값 매핑:

| XRPL 필드 | 값 |
| --- | --- |
| `Account` | holder XRPL account |
| `Issuer` | issuer JWT payload `iss`에서 account 추출 |
| `CredentialType` | issuer JWT payload `credentialStatus.credentialType` |

제출 전 확인:

- holder seed에서 복원한 account가 `account_from_did(payload.sub)`와 같아야 한다.
- `CredentialType`은 issuer JWT payload 값을 그대로 사용한다.
- issuer account는 `did:xrpl:1:{account}`에서 `{account}`만 추출한다.

status 조회:

```http
GET /credential-status/credentials/{issuerAccount}/{holderAccount}/{credentialType}
```

`active == true` 조건:

- XRPL ledger entry가 존재한다.
- accepted flag `0x00010000`이 설정되어 있다.
- expiration이 없거나 현재보다 미래다.

## 9. Challenge 발급

SD-JWT+KB presentation 전에 verifier challenge를 받는다.

```http
POST /verifier/presentations/challenges
Content-Type: application/json
```

요청:

```json
{
  "aud": "https://verifier.example",
  "presentationDefinition": {
    "id": "backend-defined-kyc-policy",
    "acceptedFormat": "dc+sd-jwt",
    "acceptedVct": [
      "https://kyvc.example/vct/legal-entity-kyc-v1"
    ],
    "acceptedJurisdictions": ["KR"],
    "minimumAssuranceLevel": "STANDARD",
    "requiredDisclosures": [
      "legalEntity.type",
      "representative.name"
    ],
    "documentRules": []
  }
}
```

응답:

```json
{
  "nonce": "...",
  "aud": "https://verifier.example",
  "expiresAt": "2026-05-06T00:05:00Z",
  "presentationDefinition": {
    "id": "kr-stock-company-kyc-v1",
    "acceptedFormat": "dc+sd-jwt",
    "acceptedVct": [
      "https://kyvc.example/vct/legal-entity-kyc-v1"
    ],
    "trustedIssuers": [],
    "requiredDisclosures": [],
    "documentRules": []
  }
}
```

주의:

- `nonce`는 1회용이다.
- 성공한 verification 이후 같은 `nonce`는 재사용할 수 없다.
- `aud`는 KB-JWT payload에 그대로 넣어야 한다.
- `presentationDefinition`은 backend가 정한다. Core는 이 값을 nonce와 함께 저장하고 응답에 그대로 돌려준다.
- verify 단계에서 backend가 `policy`를 다시 보내면 challenge에 저장된 `presentationDefinition`과 같아야 한다. 생략하면 Core는 nonce에 저장된 policy를 사용한다.

## 10. 선택 Disclosure 결정

앱은 verifier의 `presentationDefinition` 또는 verifier가 별도로 전달한 policy에 맞춰 disclosure를 선택한다. `documentRules`는 verifier 요청자가 정하는 policy다. 비어 있거나 생략되면 Core는 문서 증빙을 요구하지 않는다.

예:

```json
{
  "requiredDisclosures": [
    "legalEntity.type",
    "representative.name",
    "representative.birthDate",
    "representative.nationality",
    "beneficialOwners[].name",
    "beneficialOwners[].birthDate",
    "beneficialOwners[].nationality"
  ],
  "documentRules": [
    {
      "id": "registry-evidence",
      "required": true,
      "oneOf": [
        "KR_CORPORATE_REGISTER_FULL_CERTIFICATE"
      ],
      "originalPolicy": "OPTIONAL"
    }
  ]
}
```

선택 제출 원칙:

- required disclosure만 제출한다.
- `documentRules`가 없거나 해당 verifier가 문서 증빙을 요구하지 않으면 `documentEvidence[]` disclosure는 제출하지 않는다.
- verifier는 공개되지 않은 claim을 사용할 수 없다.
- 앱은 제출 전 사용자에게 공개될 항목을 보여주는 UX를 제공해야 한다.

실제 holder test runner에서는 총 11개 disclosure 중 6개만 선택 제출하는 케이스를 검증했다.

## 11. KB-JWT 생성

선택한 disclosure로 selected SD-JWT string을 만든다.

```text
selectedSdJwt = issuerJwt + "~" + selectedDisclosure1 + "~" + selectedDisclosure2
```

`sd_hash` 계산:

```text
sd_hash = BASE64URL_NO_PADDING(SHA-256(ASCII(selectedSdJwt)))
```

KB-JWT payload:

```json
{
  "iat": 1777939200,
  "aud": "https://verifier.example",
  "nonce": "...",
  "sd_hash": "..."
}
```

KB-JWT protected header:

```json
{
  "alg": "ES256K",
  "typ": "kb+jwt",
  "kid": "did:xrpl:1:rHolder...#holder-key-1"
}
```

KB-JWT도 compact JWS다.

```text
BASE64URL(header).BASE64URL(payload).BASE64URL(signature)
```

최종 presentation string:

```text
sdJwtKb = selectedSdJwt + "~" + kbJwt
```

Kotlin 의사 코드:

```kotlin
val selectedSdJwt = listOf(issuerJwt)
    .plus(selectedDisclosures)
    .joinToString("~")

val sdHash = base64UrlNoPadding(sha256(selectedSdJwt.toByteArray(Charsets.US_ASCII)))

val kbHeader = canonicalJsonBytes(
    mapOf(
        "alg" to "ES256K",
        "typ" to "kb+jwt",
        "kid" to "$holderDid#holder-key-1"
    )
)

val kbPayload = canonicalJsonBytes(
    mapOf(
        "iat" to nowEpochSeconds(),
        "aud" to aud,
        "nonce" to nonce,
        "sd_hash" to sdHash
    )
)

val signingInput = jwsSigningInput(kbHeader, kbPayload)
val derSignature = secp256k1SignSha256(holderAuthPrivateKey, signingInput)
val rawSignature = derEcdsaToJoseRaw64(derSignature)
val kbJwt = compactJws(kbHeader, kbPayload, rawSignature)

val sdJwtKb = "$selectedSdJwt~$kbJwt"
```

JWS 구현 주의:

- ES256K JWS signature는 `R(32 bytes) || S(32 bytes)` raw 64-byte 형식이다.
- Android crypto provider가 DER ECDSA signature를 반환하면 JOSE raw signature로 변환해야 한다.
- base64url은 `=` padding을 제거한다.
- signing input은 실제 compact JWT의 header segment와 payload segment를 그대로 사용한다.

## 12. Presentation 제출

JSON 제출:

```http
POST /verifier/presentations/verify
Content-Type: application/json
```

```json
{
  "format": "kyvc-sd-jwt-presentation-v1",
  "presentation": {
    "format": "kyvc-sd-jwt-presentation-v1",
    "definitionId": "kr-stock-company-kyc-v1",
    "aud": "https://verifier.example",
    "nonce": "...",
    "sdJwtKb": "<issuer-jwt>~<selected-disclosures>~<kb-jwt>",
    "attachmentManifest": []
  },
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
  "status_mode": "xrpl",
  "require_status": true
}
```

성공 응답 예시:

```json
{
  "ok": true,
  "errors": [],
  "details": {
    "mediaType": "kyvc-sd-jwt-presentation-v1",
    "credentialVerified": true,
    "holderBindingVerified": true,
    "statusVerified": true,
    "policyVerified": true,
    "verified": true
  }
}
```

## 13. 원본 문서 Attachment 제출

원본 PDF/image는 SD-JWT에 넣지 않는다. verifier가 원본 제출을 요구하거나 허용할 때만 multipart로 제출한다.

```http
POST /verifier/presentations/verify
Content-Type: multipart/form-data
```

Form fields:

- `presentation`: JSON string
- `doc-1`: original file bytes

`presentation.attachmentManifest` 예시:

```json
[
  {
    "requirementId": "registry-evidence",
    "documentId": "urn:kyvc:doc:registry:001",
    "attachmentRef": "doc-1",
    "documentType": "KR_CORPORATE_REGISTER_FULL_CERTIFICATE",
    "digestSRI": "sha384-...",
    "mediaType": "application/pdf",
    "byteSize": 482913,
    "submissionMode": "ATTACHED_ORIGINAL"
  }
]
```

검증 규칙:

- `attachmentManifest.documentId`는 공개된 `documentEvidence[].documentId`와 매칭되어야 한다.
- `digestSRI`는 공개된 issuer-signed `documentEvidence[].digestSRI`와 같아야 한다.
- 원본 파일 bytes의 `sha384-...` digest도 같아야 한다.
- `byteSize`, `mediaType`이 있으면 일치해야 한다.
- metadata에만 있고 SD-JWT disclosure로 공개되지 않은 document hash는 신뢰하지 않는다.

지원 모드:

- `EVIDENCE_ONLY`
- `HASH_ONLY`
- `ATTACHED_ORIGINAL`

`EXTERNAL_ORIGINAL`은 현재 명시적으로 unsupported다.

## 14. 자주 보는 실패

| 오류 | 원인 |
| --- | --- |
| `SD-JWT issuer signature verification failed` | issuer JWT 변조, issuer DID Document/key 불일치 |
| `disclosure digest is not referenced by issuer payload` | 선택 disclosure가 해당 credential에 속하지 않거나 변조됨 |
| `duplicate disclosure` | 같은 disclosure 중복 제출 |
| `SD-JWT credentialStatus credentialType mismatch` | status algorithm 입력값 또는 payload 변조 |
| `XRPL Credential status is not active` | CredentialAccept 미제출, revoke, expire, ledger 조회 실패 |
| `KB-JWT signature verification failed` | holder auth key 또는 ES256K JWS serialization 문제 |
| `KB-JWT nonce was not issued by verifier` | verifier challenge 없이 제출 |
| `KB-JWT nonce was already used` | nonce 재사용 |
| `KB-JWT nonce is expired` | challenge TTL 초과 |
| `KB-JWT aud mismatch` | challenge aud와 KB-JWT aud 불일치 |
| `KB-JWT sd_hash mismatch` | KB-JWT 생성 후 disclosure set이 바뀜 |
| `required disclosure missing: ...` | policy가 요구하는 disclosure 미제출 |
| `document rule missing: ...` | 필요한 documentEvidence disclosure 미제출 |
| `attached original digest does not match disclosed documentEvidence` | 원본 파일 bytes가 issuer-signed digest와 불일치 |

## 15. Android 로컬 데이터 모델 예시

```kotlin
data class StoredSdJwtCredential(
    val credential: String,
    val issuerJwt: String,
    val disclosures: List<String>,
    val credentialId: String,
    val issuerDid: String,
    val issuerAccount: String,
    val holderDid: String,
    val holderAccount: String,
    val vct: String,
    val credentialType: String,
    val expiresAtEpochSeconds: Long,
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

data class DisclosurePreview(
    val disclosure: String,
    val pathHint: String?,
    val label: String,
    val valuePreview: String,
    val selected: Boolean
)
```

## 16. 개발 검증 순서

Core holder runner로 SD-JWT 네트워크 flow를 먼저 확인한다.

```bash
cd core
PYTHONPATH=. .venv/bin/python holder-test/test_holder_flow.py --format sd-jwt
```

현재 runner는 다음을 검증한다.

- SD-JWT credential 발급
- XRPL `CredentialCreate`
- accept 전 verifier 실패
- holder `CredentialAccept`
- accept 후 SD-JWT credential 검증 성공
- accept 후 SD-JWT+KB 전체 disclosure presentation 성공
- accept 후 선택 disclosure presentation 성공
- XRPL `CredentialDelete`
- delete 후 credential/presentation 검증 실패

마지막 live run 확인값:

```json
{
  "sd_presentation_after_accept_ok": true,
  "sd_selective_presentation_after_accept_ok": true,
  "selected_disclosure_count": 6,
  "total_disclosure_count": 11,
  "sd_presentation_after_delete_ok": false
}
```

Android 구현 검증 순서:

1. holder XRPL account 생성 또는 복구
2. holder secp256k1 authentication key 생성
3. holder DID Document 생성
4. SD-JWT credential 수신
5. issuer JWT decode 및 signature 검증
6. disclosure digest reference 검증
7. `CredentialAccept` 제출
8. status active 확인
9. challenge 발급
10. required disclosure만 선택
11. `sd_hash` 계산
12. KB-JWT 서명
13. `/verifier/presentations/verify` 성공 확인
14. 같은 nonce 재제출 시 실패 확인
15. disclosure set 변조 시 `sd_hash` mismatch 실패 확인

## 17. 운영 주의사항

- issuer JWT payload와 disclosures는 issuer signature/disclosure digest 검증 전 신뢰하지 않는다.
- 앱 로그에 raw disclosure, 원본 문서 bytes, holder seed를 남기지 않는다.
- SD-JWT credential 원문은 전체 disclosure를 포함하므로 암호화 저장한다.
- 사용자가 선택한 disclosure 목록과 verifier 요청 policy를 audit-friendly하게 남기되 민감값 원문은 피한다.
- Android Keystore의 secp256k1 지원은 기기별 차이가 크다. provider와 백업/복구 정책을 먼저 확정한다.
- `credentialType`은 XRPL lookup key이므로 case나 문자열을 변경하지 않는다.
- nonce는 1회용이다. presentation 재시도 시 새 challenge를 받아야 한다.
- 원본 파일 제출은 verifier policy가 요구하거나 사용자가 명시 동의한 경우에만 수행한다.

## 18. 참고 코드

- `app/sdjwt/disclosure.py`: disclosure digest 및 reconstruction
- `app/sdjwt/kb.py`: KB-JWT와 `sd_hash`
- `app/sdjwt/issuer.py`: legal entity SD-JWT issuance
- `app/sdjwt/verifier.py`: issuer SD-JWT verification
- `app/status/sdjwt_status.py`: `SDJWT_STATUS_V1`
- `app/policy/sdjwt_policy.py`: required disclosure 및 document rule 검증
- `holder-test/test_holder_flow.py`: Android holder 역할 SD-JWT E2E 예제
