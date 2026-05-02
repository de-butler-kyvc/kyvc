# KYvC Core API

KYvC core now exposes reusable issuer and verifier packages, plus thin FastAPI adapters.
The implementation follows the XRPL DID / W3C VC PoC rules:

- `did:xrpl:1:{account}` DID format
- deterministic JSON canonicalization for the PoC proof input
- secp256k1 JWK keys
- Data Integrity-like `ecdsa-secp256k1-jcs-poc-2026` proofs
- top-level `credentialSalt` included in the signed VC core
- XRPL-style `credentialStatus` with `VC_STATUS_V1:{sha256(vc_core)}` credential type
- verifier policy separated from cryptographic and status verification

This is still a PoC proof format, not production JSON-LD/JWS/COSE canonicalization.

## Package Layout

```text
app/credential_schema/ KYC VC/VP schema constants and subject shaping
app/credentials/  canonical JSON, DID docs, VC, VP, proof helpers
app/issuer/       issuer service and API request/response models
app/credential_status/ credential status query service and API models
app/verifier/     verifier service, policy, and API request/response models
app/storage/      MySQL repository and storage protocols
app/xrpl/         XRPL DID/Credential transaction helpers
app/api/          FastAPI adapters
```

MySQL is used for local DID documents, issued credentials, optional credential
status mirrors, verifier challenges, and verification logs. XRPL ledger entries
are the authoritative source for credential active/inactive status in the default
API flow.

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
[`docs/android-holder-wallet-guide.md`](docs/android-holder-wallet-guide.md).

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

Generate an issuer DID signing key PEM for local PoC work:

```bash
PYTHONPATH=. .venv/bin/python - <<'PY'
from app.credentials.crypto import generate_private_key, private_key_to_pem
print(private_key_to_pem(generate_private_key()))
PY
```

Register the issuer DID on XRPL with `DIDSet`:

```bash
ISSUER_KEY_PEM="$(cat issuer-key.pem)"

curl -sS -X POST http://127.0.0.1:8090/issuer/did/register \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg pem "$ISSUER_KEY_PEM" '{issuer_private_key_pem: $pem}')" | jq
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
  "issuer_private_key_pem": "-----BEGIN PRIVATE KEY-----\\n...\\n-----END PRIVATE KEY-----",
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
match that wallet. The service signs the VC, computes the XRPL `credentialType`
from the VC core hash, submits `CredentialCreate`, stores the issued VC locally,
and records a pending local mirror for development convenience. The response
includes:

- `credential`
- `credential_type`
- `vc_core_hash`
- `credential_create_transaction`
- `ledger_entry`

Local/offline PoC mode is still available but must be explicit:

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
keeps the old local-only deletion behavior for offline PoC work.

## Status Query API

`GET /credential-status/credentials/{issuerAccount}/{holderAccount}/{credentialType}`
fetches the XRPL credential ledger entry by default and returns an `active`
boolean. Active means the entry exists, the XRPL accepted flag is set, and the
entry is not expired.

```bash
curl -sS \
  "http://127.0.0.1:8090/credential-status/credentials/rIssuer/rHolder/56435...?xrpl_json_rpc_url=https://s.devnet.rippletest.net:51234" | jq
```

Use `?status_mode=local` only for offline tests or local PoC flows.

## Verifier API

`POST /verifier/credentials/verify`

```json
{
  "credential": {},
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

```json
{
  "domain": "example.com"
}
```

Returns a verifier-issued `challenge`, its `domain`, and `expires_at`. Build the
VP proof with that challenge and domain.

`POST /verifier/presentations/verify`

```json
{
  "presentation": {},
  "did_documents": {
    "did:xrpl:1:rHolder": {}
  }
}
```

For VP verification, pass holder DID Documents in `did_documents` or pre-store them
through the MySQL repository from application code. Issuer DID Documents created by
the issuer API are stored automatically. VP verification applies the same XRPL
status lookup to each embedded VC unless `require_status` is false or
`status_mode` is explicitly set to `local`.

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
