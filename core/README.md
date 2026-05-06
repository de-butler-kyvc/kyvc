# KYvC Core API

KYvC core now exposes reusable issuer and verifier packages, plus thin FastAPI adapters.
The implementation now supports both the legacy XRPL DID / W3C VC flow and the
new KYvC legal entity SD-JWT flow:

- `did:xrpl:1:{account}` DID format
- secp256k1 JWK keys
- compact JWS signatures using JOSE `ES256K`
- legacy `vc+jwt` credentials with `VC_STATUS_V1:{sha256(vc_core)}` credential type
- SD-JWT credentials with `SDJWT_STATUS_V1` credential type derived from
  `iss`, `sub`, `vct`, and `jti`
- verifier policy separated from cryptographic and status verification

Legal-entity-shaped KYC requests default to `dc+sd-jwt`. Flat legacy claim
requests remain `vc+jwt` when `format` is omitted for compatibility. The legacy
secured VP representation remains `application/vp+jwt`; SD-JWT presentations use
SD-JWT+KB and do not rely on holder-signed `vp+jwt`.
Legacy expanded JSON objects with `proof.jws` are only available through the
explicit `embedded_jws` compatibility mode.

## SD-JWT Legal Entity KYC

`POST /issuer/credentials/kyc` accepts `"format": "dc+sd-jwt"` or `"vc+jwt"`.
For SD-JWT, the issuer signs a compact SD-JWT credential:

```text
<issuer-signed-jwt>~<disclosure-1>~<disclosure-2>~...
```

The issuer-signed JWT uses `typ: "dc+sd-jwt"` and contains always-disclosed
claims: `iss`, `sub`, `vct`, `jti`, `iat`, `exp`, `cnf`, and
`credentialStatus`. KYvC also keeps `kyc.jurisdiction` and
`kyc.assuranceLevel` available for coarse policy routing. Legal entity,
representative, beneficial owner, establishment purpose, and document evidence
claims are selectively disclosed through salted disclosures. Undisclosed claims
are never reconstructed or used by verifier policy.

SD-JWT status is an active/revoked lookup key only:

```text
SDJWT_STATUS_V1 = hex(sha256(
  "SDJWT_STATUS_V1:" + iss + "\x1f" + sub + "\x1f" + vct + "\x1f" + jti
))
```

The implementation emits uppercase hex because the existing XRPL adapter and
legacy status code already use uppercase `CredentialType` values. Verifiers
recompute the value only from the always-disclosed signed payload fields and
fail if it differs from `credentialStatus.credentialType`.

Tamper-proofing is not delegated to credential status. It is provided by the
issuer JWS signature, disclosure digest validation, KB-JWT holder signature,
`sd_hash`, nonce, and audience.

SD-JWT+KB presentations use:

```text
<issuer-signed-jwt>~<selected-disclosure-1>~...~<kb-jwt>
```

The KB-JWT is signed by the holder authentication key referenced by `cnf.kid`
and carries `iat`, `aud`, `nonce`, and `sd_hash`. `aud` replaces the legacy VP
`domain`; `nonce` replaces the legacy VP `challenge`. Verifier challenges are
one-time-use and expire according to `VERIFIER_CHALLENGE_TTL_SECONDS`.

Original PDF/image files are not embedded in SD-JWT. A verifier may accept
multipart attachments alongside the presentation metadata. Each attachment must
match a disclosed issuer-signed `documentEvidence[]` item by `documentId`,
`documentType`, and `digestSRI`; hashes supplied only in presentation metadata
are not trusted.

## Signature Format

KYvC uses the JOSE profile from W3C VC-JOSE-COSE:

- JWS Compact Serialization from RFC 7515
- `alg: "ES256K"` from RFC 8812, using secp256k1 with SHA-256
- `typ: "vc+jwt"` for credentials and `typ: "vp+jwt"` for presentations
- `cty: "vc"` or `cty: "vp"` to identify the unsecured payload type
- `kid` set to the DID verification method URL
- `iss` set on VC JWTs and required to match the VC `issuer`

The JWS payload is the unsecured VC or VP document. In the default mode the API
returns that compact JWT directly:

```json
{
  "credential": "eyJhbGciOiJFUzI1Nksi..."
}
```

VP payloads no longer embed raw VC JSON. Each entry in `verifiableCredential` is
an Enveloped Verifiable Credential as defined by VC Data Model 2.0:

```json
{
  "@context": "https://www.w3.org/ns/credentials/v2",
  "id": "data:application/vc+jwt,eyJhbGciOiJFUzI1Nksi...",
  "type": "EnvelopedVerifiableCredential"
}
```

The verifier now treats the secured JWT as the verification input, verifies the
JOSE signature, decodes the VC/VP document from the JWS payload, and then reuses
the existing validation path for XRPL status, policy, validity dates, DID
authorization, and holder/subject matching.

`challenge` and `domain` are still required for VP replay protection. They are
carried as protected JOSE header parameters on the `vp+jwt`, so tampering with
either field breaks the holder signature. This is a KYvC application profile on
top of VC-JOSE-COSE; the core secured presentation remains `application/vp+jwt`.

### Compatibility Mode

For older local callers, `POST /issuer/credentials/kyc` accepts
`"credential_format": "embedded_jws"`. That returns the previous expanded JSON
shape with a `proof.jws` compatibility wrapper. Standard JWT mode is the default,
and the verifier treats the embedded wrapper path as `legacy-embedded-jws`.

### Why JWS Instead of Data Integrity

VC Data Integrity is the natural W3C-native proof family, but production-grade
Data Integrity requires the matching cryptosuite canonicalization rules, usually
JSON-LD processing and RDF Dataset Canonicalization. The previous implementation
used deterministic JSON only, so switching to Data Integrity correctly would be a
larger architecture and dependency change.

JOSE/JWS is a clearer minimal step toward interoperability because it has stable
compact serialization, standard protected headers, and direct support for JWK
key discovery. W3C VC-JOSE-COSE defines JOSE/COSE-based securing for VC/VP data,
and RFC 8812 registers secp256k1 for JOSE as `ES256K`.

### Remaining Limits

The default VC and VP security representations now match the VC-JOSE-COSE media
types much more closely, but there are still implementation limits:

- JWS payload bytes are produced from deterministic JSON serialization; the
  verifier validates the JWS and uses the decoded payload as the VC/VP document.
- `challenge` and `domain` are protected JOSE header parameters rather than a
  separate W3C-defined presentation exchange profile.
- The local credential repository still stores the unsecured VC document for
  indexing and XRPL status bookkeeping; API responses default to the secured JWT.
- Compatibility mode can still emit `proof.jws`, but it is no longer the default
  verifier or issuer path.

`ES256K` is standardized, but secp256k1 is less universally supported by
enterprise JWT libraries, FIPS-oriented stacks, and managed KMS products than
`ES256` on P-256. If broad off-the-shelf verifier compatibility becomes more
important than XRPL/ecosystem key alignment, the recommended migration path is
`ES256` with P-256 JWKs or Ed25519/`EdDSA`, with DID Documents publishing the
new verification methods during a key-rotation period.

References:

- W3C VC-JOSE-COSE: https://www.w3.org/TR/vc-jose-cose/
- W3C VC Data Model 2.0: https://www.w3.org/TR/vc-data-model-2.0/
- RFC 7515 JSON Web Signature: https://www.rfc-editor.org/rfc/rfc7515
- RFC 7519 JSON Web Token: https://www.rfc-editor.org/rfc/rfc7519
- RFC 8812 JOSE `ES256K`: https://www.rfc-editor.org/rfc/rfc8812

## Package Layout

```text
app/credential_schema/ KYC VC/VP schema constants and subject shaping
app/credentials/  canonical JSON, DID docs, VC, VP, proof helpers
app/issuer/       issuer service and API request/response models
app/credential_status/ credential status query service and API models
app/sdjwt/        SD-JWT disclosure, issuer, verifier, KB-JWT helpers, models
app/status/       SD-JWT status derivation helpers
app/policy/       SD-JWT disclosure and legal-entity document policy
app/verifier/     verifier service, policy, and API request/response models
app/storage/      MySQL repository and storage protocols
app/xrpl/         XRPL DID/Credential transaction helpers
app/api/          FastAPI adapters
```

MySQL is used for local DID documents, issued credentials, optional credential
status mirrors, verifier challenges, and verification logs. XRPL ledger entries
are the authoritative source for credential active/inactive status in the default
API flow.

## DID Resolution

Verifier DID resolution uses a chain:

1. Request `did_documents`, verified against the XRPL DID entry `Data` hash when
   an XRPL client is configured
2. Local MySQL DID Document cache, verified against the XRPL DID entry `Data`
   hash before use when an XRPL client is configured
3. XRPL DID entry lookup, DID Document fetch from the ledger `URI`, and
   multihash comparison against ledger `Data`

Issuer DID Documents created by this core service are stored locally, but in
XRPL-backed verification the cached document is still checked against ledger
`Data` before it is used. External issuer and holder DIDs fall back to XRPL
resolution: the verifier reads the DID entry, fetches the document from `URI`,
computes `multihash_sha2_256(canonical_json(didDocument))`, and requires it to
match ledger `Data`. Successfully verified external documents are cached locally
for later verification; later cache hits are rechecked against the current ledger
hash.

Holder DID Documents may still be supplied in verifier request `did_documents`,
but in XRPL mode they are not trusted as-is. The verifier verifies the supplied
document against the XRPL DID entry hash before using it. Offline local mode
without `xrpl_json_rpc_url` keeps the old static/local behavior for development
only. If a local cache entry does not match ledger `Data`, the verifier skips
that cache entry and attempts a fresh XRPL `URI` fetch.

## Run

Start a local MySQL container:

```bash
cd core
docker compose -f docker-compose.local.yml up -d mysql
```

The local Compose file reads `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and
`LOCAL_DB_PORT` from `core/.env` when present. By default it exposes MySQL on
`127.0.0.1:3307` and stores data in the `kyvc-core-mysql-data` Docker volume.

```bash
cd core
uvicorn app.main:app --reload --port 8090
```

Configuration:

```env
DB_HOST=127.0.0.1
DB_PORT=3307
LOCAL_DB_PORT=3307
DB_NAME=kyvc_core
DB_USER=kyvc_core_dev
DB_PASSWORD=kyvc_core_dev_password
MYSQL_ROOT_PASSWORD=kyvc_core_root_password
XRPL_NETWORK_NAME=devnet
XRPL_JSON_RPC_URL=https://s.devnet.rippletest.net:51234
XRPL_ISSUER_SEED=
ISSUER_PRIVATE_KEY_PEM_PATH=./.local-secrets/issuer-key.pem
XRPL_FAUCET_HOST=
DID_DOC_BASE_URL=http://127.0.0.1:8090
VERIFIER_CHALLENGE_TTL_SECONDS=300
ALLOW_MAINNET=0
```

The application creates the required MySQL tables on startup when the configured
database already exists and the configured user has DDL permissions.

Holder behavior is not exposed as a core API. The production holder will be a
mobile app. This repository keeps a configured-network holder test runner under
`holder-test/` so the core issuer/verifier flow can still be exercised end to
end.

For Android holder wallet implementation guidance, see
[`docs/android-holder-wallet-sdjwt-guide.md`](docs/android-holder-wallet-sdjwt-guide.md).
The legacy `vc+jwt`/`vp+jwt` wallet guide remains at
[`docs/android-holder-wallet-guide.md`](docs/android-holder-wallet-guide.md).
For backend integration, see the OpenAPI contract at
[`docs/backend-integration.openapi.yaml`](docs/backend-integration.openapi.yaml).

## Issuer API

Generate a funded issuer wallet on the configured XRPL network:

```bash
curl -sS -X POST http://127.0.0.1:8090/issuer/wallets \
  -H "Content-Type: application/json" \
  -d '{}' | jq
```

The API uses `XRPL_JSON_RPC_URL`, `XRPL_FAUCET_HOST`, and `ALLOW_MAINNET` from
env unless the request overrides them. The response includes a seed. Put it in
`core/.env` as `XRPL_ISSUER_SEED`; never commit that file.

Generate an issuer DID signing key PEM for local development:

```bash
PYTHONPATH=. .venv/bin/python - <<'PY'
from app.credentials.crypto import generate_private_key, private_key_to_pem
print(private_key_to_pem(generate_private_key()))
PY
```

Store it in a private file such as `core/.local-secrets/issuer-key.pem`, then
set the path in `core/.env`:

```env
ISSUER_PRIVATE_KEY_PEM_PATH=./.local-secrets/issuer-key.pem
```

Register the issuer DID on XRPL with `DIDSet`:

```bash
curl -sS -X POST http://127.0.0.1:8090/issuer/did/register \
  -H "Content-Type: application/json" \
  -d '{}' | jq
```

The `diddoc_url` in the response is the URI written to the XRPL DID entry. With
the default local server it looks like:

```text
http://127.0.0.1:8090/dids/{issuerAccount}/diddoc.json
```

Fetch a stored DID Document directly:

```bash
curl -sS http://127.0.0.1:8090/dids/{issuerAccount}/diddoc.json | jq
```

`POST /issuer/credentials/kyc`

```json
{
  "issuer_seed": "s...",
  "holder_account": "rHolder",
  "claims": {
    "kycLevel": "BASIC",
    "jurisdiction": "KR"
  },
  "valid_from": "2026-05-02T00:00:00Z",
  "valid_until": "2026-06-02T00:00:00Z",
  "status_uri": "https://example.com/status/optional"
}
```

The default `status_mode` is `xrpl`. The issuer account is derived from
`issuer_seed` or `XRPL_ISSUER_SEED`; if `issuer_account` is also supplied it must
match that wallet. The DID/VC signing key is read from `issuer_private_key_pem`
or the file at `ISSUER_PRIVATE_KEY_PEM_PATH`.

For legal-entity-shaped KYC claims, omit `format` or set `"format":
"dc+sd-jwt"`:

```json
{
  "issuer_account": "rIssuer",
  "holder_account": "rHolder",
  "holder_did": "did:xrpl:1:rHolder",
  "format": "dc+sd-jwt",
  "claims": {
    "kyc": {"jurisdiction": "KR", "assuranceLevel": "STANDARD"},
    "legalEntity": {"type": "STOCK_COMPANY", "name": "KYvC Labs"},
    "representative": {"name": "Kim Holder", "birthDate": "1980-01-01", "nationality": "KR"},
    "beneficialOwners": [{"name": "Owner One", "birthDate": "1979-02-03", "nationality": "KR"}],
    "documentEvidence": [
      {
        "documentId": "urn:kyvc:doc:registry:001",
        "documentType": "KR_CORPORATE_REGISTER_FULL_CERTIFICATE",
        "digestSRI": "sha384-...",
        "mediaType": "application/pdf",
        "byteSize": 482913,
        "evidenceFor": ["legalEntity.registrationNumber"]
      }
    ]
  },
  "valid_from": "2026-05-05T00:00:00Z",
  "valid_until": "2027-05-05T00:00:00Z",
  "status_mode": "local"
}
```

The SD-JWT response includes:

- `format: "dc+sd-jwt"`
- `credential` as `<issuer-jwt>~<all-disclosures>`
- `credentialId` from `jti`
- `status.credentialType` for XRPL `CredentialCreate`
- `selectiveDisclosure.disclosablePaths`

Legacy flat claim requests or explicit `"format": "vc+jwt"` still secure the VC
as `application/vc+jwt`, compute XRPL `credentialType` from the VC core hash,
submit `CredentialCreate`, store the issued VC locally, and record a pending
local mirror for development convenience. The legacy response includes:

- `credential` as a compact `vc+jwt` string by default
- `credential_type`
- `vc_core_hash`
- `credential_create_transaction`
- `ledger_entry`

Local/offline mode is still available but must be explicit:

```json
{
  "issuer_account": "rIssuer",
  "issuer_private_key_pem": "-----BEGIN PRIVATE KEY-----\\n...\\n-----END PRIVATE KEY-----",
  "holder_account": "rHolder",
  "claims": {"kycLevel": "BASIC", "jurisdiction": "KR"},
  "valid_from": "2026-05-02T00:00:00Z",
  "valid_until": "2026-06-02T00:00:00Z",
  "status_mode": "local",
  "mark_status_accepted": true
}
```

In local mode the local mirror is the status source. In XRPL mode it is only a
cache/log; verifier decisions use XRPL `ledger_entry`.

## Holder Test Runner

Holder accept is deliberately outside the core service API boundary. For
development, `holder-test/test_holder_flow.py` plays the mobile holder role and
submits the holder-side `CredentialAccept` directly to the configured XRPL
network.

Run the configured-network flow:

```bash
cd core
PYTHONPATH=. .venv/bin/python holder-test/test_holder_flow.py
```

The runner:

- creates funded issuer and holder wallets on the configured XRPL network
- builds and signs a KYC VC through `IssuerService`
- submits `CredentialCreate`
- verifies that the VC is inactive before holder acceptance
- stores the VC in a small holder-test SQLite file
- submits `CredentialAccept` as the holder
- verifies that the VC is active
- submits `CredentialDelete` as the issuer
- verifies that the VC is inactive again

It prints the issuer/holder accounts, credential type, transaction hashes, and
the before/after verifier results as JSON.

## Revoke

`POST /issuer/credentials/revoke` now submits XRPL `CredentialDelete` in default
`xrpl` mode:

```json
{
  "issuer_seed": "s...",
  "holder_account": "rHolder",
  "credential_type": "56435F5354415455535F56313A..."
}
```

On success the service removes the local mirror as well. `status_mode: "local"`
keeps the old local-only deletion behavior for offline tests.

SD-JWT credentials can also be revoked by `jti` or `status_id` when the issuer,
holder, and `vct` inputs needed for `SDJWT_STATUS_V1` are supplied:

```json
{
  "issuer_account": "rIssuer",
  "holder_account": "rHolder",
  "jti": "urn:uuid:...",
  "vct": "https://kyvc.example/vct/legal-entity-kyc-v1",
  "status_mode": "local"
}
```

## Status Query API

`GET /credential-status/credentials/{issuerAccount}/{holderAccount}/{credentialType}`
fetches the XRPL credential ledger entry by default and returns an `active`
boolean. Active means the entry exists, the XRPL accepted flag is set, and the
entry is not expired.

```bash
curl -sS \
  "http://127.0.0.1:8090/credential-status/credentials/rIssuer/rHolder/56435...?xrpl_json_rpc_url=https://s.devnet.rippletest.net:51234" | jq
```

Use `?status_mode=local` only for offline tests or local development flows.

## Verifier API

`POST /verifier/credentials/verify`

```json
{
  "credential": "eyJhbGciOiJFUzI1Nksi...",
  "policy": {
    "trustedIssuers": ["did:xrpl:1:rIssuer"],
    "acceptedKycLevels": ["BASIC", "ADVANCED"],
    "acceptedJurisdictions": ["KR"]
  },
  "require_status": true,
  "status_mode": "xrpl",
  "xrpl_json_rpc_url": "https://s.devnet.rippletest.net:51234"
}
```

When `require_status` is true, verifier status checks use XRPL `ledger_entry`
by default. A VC issued with `CredentialCreate` but not yet accepted by the
holder is inactive. After `CredentialAccept` it is active. After
`CredentialDelete` it is inactive again.

`POST /verifier/presentations/challenges`

For SD-JWT+KB:

```json
{
  "aud": "https://verifier.example",
  "presentationDefinition": {
    "id": "backend-defined-kyc-policy",
    "acceptedFormat": "dc+sd-jwt",
    "acceptedVct": ["https://kyvc.example/vct/legal-entity-kyc-v1"],
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

Returns `nonce`, `aud`, `expiresAt`, and a `presentationDefinition` with
`acceptedFormat`, `acceptedVct`, `requiredDisclosures`, and `documentRules`.
Document rules are verifier policy, not automatic Core defaults. If
`documentRules` is empty or omitted, Core does not require document evidence for
that verification request. When the backend supplies `presentationDefinition` in
the challenge request, Core stores it with the nonce and echoes the same object
in the response. Later SD-JWT+KB verification uses the stored challenge policy;
if the verify request also supplies `policy`, it must match the challenge policy.

For legacy `vp+jwt`, send `"format": "vp+jwt"` with `domain`; the response keeps
`challenge`, `domain`, and `expires_at`.

`POST /verifier/presentations/verify`

```json
{
  "presentation": "eyJhbGciOiJFUzI1Nksi...",
  "did_documents": {
    "did:xrpl:1:rHolder": {}
  }
}
```

For VP verification, pass holder DID Documents in `did_documents` or pre-store them
through the MySQL repository from application code. Issuer DID Documents created by
the issuer API are stored automatically. VP verification applies the same XRPL
status lookup to each enveloped VC unless `require_status` is false or
`status_mode` is explicitly set to `local`.

Verifier responses include DID resolution metadata when available, for example
`details.issuerDidResolution` and `details.holderDidResolution` with `resolver`,
`ledger`, `uri`, `dataHash`, and `cached`. DID ledger entry absence, URI fetch
failure, and hash mismatch are reported as distinct errors.

For SD-JWT+KB JSON verification:

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
    "did:xrpl:1:rHolder": {}
  },
  "policy": {
    "acceptedFormat": "dc+sd-jwt",
    "acceptedVct": ["https://kyvc.example/vct/legal-entity-kyc-v1"],
    "acceptedJurisdictions": ["KR"],
    "minimumAssuranceLevel": "STANDARD",
    "requiredDisclosures": [
      "legalEntity.type",
      "representative.name",
      "beneficialOwners[].name"
    ],
    "documentRules": [
      {
        "id": "registry-evidence",
        "required": true,
        "oneOf": ["KR_CORPORATE_REGISTER_FULL_CERTIFICATE"],
        "originalPolicy": "OPTIONAL"
      }
    ]
  },
  "status_mode": "local"
}
```

When the challenge was issued with `presentationDefinition`, this `policy` may be
omitted; Core will use the policy stored with the nonce. If it is supplied, it
must match the challenge `presentationDefinition`.

`multipart/form-data` is also accepted. Put the same JSON object in the
`presentation` form field and upload originals using field names that match
`attachmentManifest[].attachmentRef`. Supported original-document modes are
evidence/hash-only presentation and attached originals. External original
retrieval is intentionally not implemented yet.

## Migration Note

Move new holders and verifiers from `vc+jwt` plus holder-signed `vp+jwt` to
`dc+sd-jwt` plus KB-JWT. Keep accepting legacy `vc+jwt`/`vp+jwt` during rollout,
but do not mix status algorithms: legacy credentials use `VC_STATUS_V1`; SD-JWT
credentials use `SDJWT_STATUS_V1`. Status remains only the XRPL active/revoked
lookup. Presentation replay protection moves from `challenge`/`domain` in the
VP JWS header to `nonce`/`aud` in KB-JWT, with `sd_hash` binding the exact
issuer JWT and selected disclosure set.

## Developer Verification

Compile check:

```bash
.venv/bin/python -m compileall app tests
```

Tests are in `tests/`. Install dev dependencies in the core virtualenv to run them:

```bash
pip install -r requirements-dev.txt
.venv/bin/python -m pytest tests -q
```
