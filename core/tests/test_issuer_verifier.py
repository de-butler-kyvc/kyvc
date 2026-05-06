from datetime import UTC, datetime, timedelta

import httpx
from fastapi.testclient import TestClient

from app.credentials.resolver import (
    CachingDidResolver,
    CompositeDidResolver,
    VerifiedCachedDidResolver,
    VerifiedStaticDidResolver,
    XrplDidResolver,
    did_document_hash,
)
from app.credentials.resolver import did_resolution_result
from app.credentials.crypto import decode_compact_jws, generate_private_key, private_key_to_jwk
from app.credentials.crypto import private_key_to_pem
from app.credentials.did import did_from_account, holder_diddoc
from app.credentials.hexutil import utf8_to_hex
from app.credentials.vc import (
    build_kyc_vc,
    credential_type_hex_for_vc,
    decode_vc_jwt,
    verify_vc_signature,
)
from app.credentials.vp import create_vp, enveloped_vp, vc_jwts_from_vp, verify_vp_signature
from app.api.did import get_did_document
from app.api.verifier import issue_presentation_challenge
from app.core.config import Settings
from app.main import create_app
from app.issuer.service import IssuerService
from app.storage.interfaces import VerificationChallengeEntry
from app.verifier.api_models import IssuePresentationChallengeRequest
from app.verifier.policy import VerificationPolicy
from app.verifier.service import VerifierService


class JsonResponse:
    def __init__(self, data, *, error=None):
        self.data = data
        self.error = error

    def raise_for_status(self):
        if self.error is not None:
            raise self.error

    def json(self):
        return self.data


def _did_ledger_entry(did_document, *, uri="https://example.com/diddoc.json"):
    return {
        "URI": utf8_to_hex(uri),
        "Data": did_document_hash(did_document),
    }


class InMemoryRepository:
    def __init__(self):
        self.did_documents = {}
        self.issued_credentials = {}
        self.credential_status = {}
        self.verification_logs = []
        self.verification_challenges = {}

    @staticmethod
    def _datetime(value: datetime) -> datetime:
        return value.astimezone(UTC).replace(microsecond=0)

    def save_did_document(self, did, did_document):
        self.did_documents[did] = did_document

    def get_did_document(self, did):
        return self.did_documents.get(did)

    def resolve(self, did):
        did_document = self.get_did_document(did)
        if did_document is None:
            raise ValueError(f"DID Document not found for {did}")
        return did_resolution_result(did, did_document, {"resolver": "memory"})

    def save_issued_credential(
        self,
        *,
        vc,
        issuer_did,
        issuer_account,
        holder_did,
        holder_account,
        credential_type,
        vc_core_hash,
    ):
        self.issued_credentials[str(vc["id"])] = {
            "vc": vc,
            "issuer_did": issuer_did,
            "issuer_account": issuer_account,
            "holder_did": holder_did,
            "holder_account": holder_account,
            "credential_type": credential_type,
            "vc_core_hash": vc_core_hash,
            "revoked_at": None,
        }

    def save_credential_status(
        self,
        *,
        issuer_account,
        holder_account,
        credential_type,
        flags,
        expiration=None,
        uri=None,
    ):
        self.credential_status[(issuer_account, holder_account, credential_type)] = {
            "Issuer": issuer_account,
            "Subject": holder_account,
            "CredentialType": credential_type,
            "Flags": flags,
            **({"Expiration": expiration} if expiration is not None else {}),
            **({"URI": uri} if uri is not None else {}),
        }

    def get_credential_entry(self, issuer_account, holder_account, credential_type):
        return self.credential_status.get((issuer_account, holder_account, credential_type))

    def revoke_credential_status(self, *, issuer_account, holder_account, credential_type):
        self.credential_status.pop((issuer_account, holder_account, credential_type), None)
        for issued in self.issued_credentials.values():
            if (
                issued["issuer_account"] == issuer_account
                and issued["holder_account"] == holder_account
                and issued["credential_type"] == credential_type
            ):
                issued["revoked_at"] = datetime.now(tz=UTC)

    def save_verification_result(self, *, subject_id, ok, errors, details, verified_at):
        self.verification_logs.append(
            {
                "subject_id": subject_id,
                "ok": ok,
                "errors": errors,
                "details": details,
                "verified_at": self._datetime(verified_at),
            }
        )

    def save_verification_challenge(self, *, challenge, domain, expires_at, created_at):
        self.verification_challenges[challenge] = VerificationChallengeEntry(
            challenge=challenge,
            domain=domain,
            expires_at=self._datetime(expires_at),
            created_at=self._datetime(created_at),
        )

    def get_verification_challenge(self, challenge):
        return self.verification_challenges.get(challenge)

    def mark_verification_challenge_used(self, challenge, used_at):
        entry = self.verification_challenges.get(challenge)
        if entry is None or entry.used_at is not None:
            return False
        self.verification_challenges[challenge] = VerificationChallengeEntry(
            challenge=entry.challenge,
            domain=entry.domain,
            expires_at=entry.expires_at,
            used_at=self._datetime(used_at),
            created_at=entry.created_at,
        )
        return True


def _repo(tmp_path):
    return InMemoryRepository()


def _vp_fixture(tmp_path):
    repository = _repo(tmp_path)
    issuer_key = generate_private_key()
    holder_key = generate_private_key()
    issuer = IssuerService(
        "rIssuer",
        issuer_key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    issuer.register_did_document()
    holder_did = did_from_account("rHolder")
    repository.save_did_document(holder_did, holder_diddoc(holder_did, "holder-key-1", private_key_to_jwk(holder_key)))
    vc = issuer.issue_kyc_vc(
        holder_account="rHolder",
        holder_did=holder_did,
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        mark_status_accepted=True,
    )
    return repository, holder_did, holder_key, vc


def _save_challenge(
    repository,
    *,
    challenge: str = "challenge-1",
    domain: str = "example.com",
    expires_at: datetime | None = None,
) -> None:
    issued_at = datetime.now(tz=UTC)
    repository.save_verification_challenge(
        challenge=challenge,
        domain=domain,
        expires_at=expires_at or issued_at + timedelta(minutes=5),
        created_at=issued_at,
    )


def test_vc_local_signature_and_salt_tamper(tmp_path):
    repository = _repo(tmp_path)
    key = generate_private_key()
    service = IssuerService(
        "rIssuer",
        key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    service.register_did_document()
    vc = service.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "amlScreened": True, "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
    )

    assert verify_vc_signature(vc, private_key_to_jwk(key))
    _, vc_document = decode_vc_jwt(vc)
    original_credential_type = vc_document["credentialStatus"]["credentialType"]
    parts = vc.split(".")
    parts[2] = ("A" if parts[2][0] != "A" else "B") + parts[2][1:]
    tampered_vc = ".".join(parts)
    assert not verify_vc_signature(tampered_vc, private_key_to_jwk(key))
    vc_document["credentialSalt"] = "tampered-salt"
    assert credential_type_hex_for_vc(vc_document) != original_credential_type


def test_vc_uses_application_vc_jwt_secured_representation(tmp_path):
    repository = _repo(tmp_path)
    key = generate_private_key()
    service = IssuerService(
        "rIssuer",
        key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    service.register_did_document()
    vc = service.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
    )

    assert isinstance(vc, str)
    assert len(vc.split(".")) == 3
    protected, payload, _, signature = decode_compact_jws(vc)
    assert protected["alg"] == "ES256K"
    assert protected["typ"] == "vc+jwt"
    assert protected["cty"] == "vc"
    assert protected["kid"] == "did:xrpl:1:rIssuer#issuer-key-1"
    assert protected["iss"] == payload["issuer"]
    assert len(signature) == 64
    assert payload["type"] == ["VerifiableCredential", "KycCredential"]
    assert "proof" not in payload
    assert verify_vc_signature(vc, private_key_to_jwk(key))


def test_vp_uses_application_vp_jwt_with_enveloped_vc(tmp_path):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    assert isinstance(vp, str)
    protected, payload, _, signature = decode_compact_jws(vp)
    assert protected["alg"] == "ES256K"
    assert protected["typ"] == "vp+jwt"
    assert protected["cty"] == "vp"
    assert protected["kid"] == f"{holder_did}#holder-key-1"
    assert protected["challenge"] == "challenge-1"
    assert protected["domain"] == "example.com"
    assert len(signature) == 64
    enveloped = payload["verifiableCredential"][0]
    assert enveloped["@context"] == "https://www.w3.org/ns/credentials/v2"
    assert enveloped["type"] == "EnvelopedVerifiableCredential"
    assert enveloped["id"].startswith("data:application/vc+jwt,")
    assert vc_jwts_from_vp(payload) == [vc]
    assert verify_vp_signature(vp, private_key_to_jwk(holder_key))

    parts = vp.split(".")
    parts[2] = ("A" if parts[2][0] != "A" else "B") + parts[2][1:]
    tampered_vp = ".".join(parts)
    assert not verify_vp_signature(tampered_vp, private_key_to_jwk(holder_key))


def test_verifier_accepts_enveloped_vp_data_url(tmp_path):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    _save_challenge(repository)
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    result = verifier.verify_vp(enveloped_vp(vp))

    assert result.ok
    assert result.details["mediaType"] == "vp+jwt"


def test_same_claims_receive_distinct_credential_types():
    valid_from = datetime(2026, 1, 1, tzinfo=UTC)
    valid_until = datetime(2026, 1, 2, tzinfo=UTC)
    claims = {"kycLevel": "BASIC", "jurisdiction": "KR"}

    vc_a = build_kyc_vc(did_from_account("rIssuer"), did_from_account("rHolder"), claims, valid_from, valid_until)
    vc_b = build_kyc_vc(did_from_account("rIssuer"), did_from_account("rHolder"), claims, valid_from, valid_until)
    vc_b["id"] = vc_a["id"]

    assert vc_a["credentialSalt"] != vc_b["credentialSalt"]
    assert credential_type_hex_for_vc(vc_a) != credential_type_hex_for_vc(vc_b)


def test_verifier_vc_success_and_policy_failure_are_separate(tmp_path):
    repository = _repo(tmp_path)
    key = generate_private_key()
    service = IssuerService(
        "rIssuer",
        key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    service.register_did_document()
    vc = service.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        mark_status_accepted=True,
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    assert verifier.verify_vc(vc).ok

    policy = VerificationPolicy(accepted_kyc_levels={"ADVANCED"})
    result = verifier.verify_vc(vc, policy=policy)
    assert not result.ok
    assert "kycLevel is not accepted by verifier policy" in result.errors
    assert result.details["credentialAccepted"] is True


def test_verifier_uses_local_db_diddoc_for_own_issuer(tmp_path):
    repository = _repo(tmp_path)
    key = generate_private_key()
    service = IssuerService(
        "rIssuer",
        key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    service.register_did_document()
    vc = service.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        mark_status_accepted=True,
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    result = verifier.verify_vc(vc)

    assert result.ok
    assert result.details["issuerDidResolution"]["resolver"] == "memory"


def test_local_db_diddoc_is_verified_against_xrpl_hash(tmp_path, monkeypatch):
    repository = _repo(tmp_path)
    key = generate_private_key()
    service = IssuerService(
        "rIssuer",
        key,
        credential_repository=repository,
        status_repository=repository,
        did_document_repository=repository,
    )
    did_document = service.register_did_document()
    vc = service.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        mark_status_accepted=True,
    )
    monkeypatch.setattr(
        "app.xrpl.ledger.get_did_entry",
        lambda client, account: _did_ledger_entry(did_document),
    )

    resolver = CompositeDidResolver(
        VerifiedCachedDidResolver(repository, object()),
        CachingDidResolver(XrplDidResolver(object()), repository),
    )
    result = VerifierService(resolver, status_lookup=repository.get_credential_entry).verify_vc(vc)

    assert result.ok
    assert result.details["issuerDidResolution"]["resolver"] == "cache-xrpl-verified"
    assert result.details["issuerDidResolution"]["cacheResolver"] == "memory"
    assert result.details["issuerDidResolution"]["cacheVerified"] is True


def test_local_db_hash_mismatch_skips_cache_and_fetches_xrpl(tmp_path, monkeypatch):
    repository = _repo(tmp_path)
    stale_key = generate_private_key()
    current_key = generate_private_key()
    issuer_did = did_from_account("rIssuer")
    stale_issuer = IssuerService(
        "rIssuer",
        stale_key,
        issuer_did=issuer_did,
        did_document_repository=repository,
    )
    stale_issuer.register_did_document()
    current_issuer = IssuerService(
        "rIssuer",
        current_key,
        issuer_did=issuer_did,
        credential_repository=repository,
        status_repository=repository,
    )
    current_document = current_issuer.build_did_document()
    vc = current_issuer.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        mark_status_accepted=True,
    )
    monkeypatch.setattr(
        "app.xrpl.ledger.get_did_entry",
        lambda client, account: _did_ledger_entry(current_document),
    )
    monkeypatch.setattr("app.credentials.resolver.httpx.get", lambda uri, timeout: JsonResponse(current_document))
    resolver = CompositeDidResolver(
        VerifiedCachedDidResolver(repository, object()),
        CachingDidResolver(XrplDidResolver(object()), repository),
    )

    result = VerifierService(resolver, status_lookup=repository.get_credential_entry).verify_vc(vc)

    assert result.ok
    assert result.details["issuerDidResolution"]["resolver"] == "xrpl"
    assert result.details["issuerDidResolution"]["cached"] is True
    assert repository.get_did_document(issuer_did) == current_document


def test_external_issuer_resolves_from_xrpl_fetch_and_caches(tmp_path, monkeypatch):
    repository = _repo(tmp_path)
    key = generate_private_key()
    issuer = IssuerService(
        "rExternalIssuer",
        key,
        credential_repository=repository,
        status_repository=repository,
        issuer_did=did_from_account("rExternalIssuer"),
    )
    issuer_diddoc = issuer.build_did_document()
    vc = issuer.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        mark_status_accepted=True,
    )

    def get_did_entry(client, account):
        assert account == "rExternalIssuer"
        return _did_ledger_entry(issuer_diddoc)

    monkeypatch.setattr("app.xrpl.ledger.get_did_entry", get_did_entry)
    monkeypatch.setattr("app.credentials.resolver.httpx.get", lambda uri, timeout: JsonResponse(issuer_diddoc))
    resolver = CompositeDidResolver(
        repository,
        CachingDidResolver(XrplDidResolver(object()), repository),
    )

    result = VerifierService(resolver, status_lookup=repository.get_credential_entry).verify_vc(vc)

    assert result.ok
    assert result.details["issuerDidResolution"]["resolver"] == "xrpl"
    assert result.details["issuerDidResolution"]["cached"] is True
    assert repository.get_did_document(did_from_account("rExternalIssuer")) == issuer_diddoc

    cached_result = VerifierService(
        VerifiedCachedDidResolver(repository, object()),
        status_lookup=repository.get_credential_entry,
    ).verify_vc(vc)
    assert cached_result.ok
    assert cached_result.details["issuerDidResolution"]["resolver"] == "cache-xrpl-verified"


def test_verifier_api_uses_xrpl_did_resolver_for_external_issuer(tmp_path, monkeypatch):
    repository = _repo(tmp_path)
    app = create_app(settings=Settings(), repository=repository)
    client = TestClient(app)
    key = generate_private_key()
    issuer = IssuerService(
        "rExternalIssuer",
        key,
        credential_repository=repository,
        status_repository=repository,
        issuer_did=did_from_account("rExternalIssuer"),
    )
    issuer_diddoc = issuer.build_did_document()
    vc = issuer.issue_kyc_vc(
        holder_account="rHolder",
        claims={"kycLevel": "BASIC", "jurisdiction": "KR"},
        valid_from=datetime.now(tz=UTC) - timedelta(minutes=1),
        valid_until=datetime.now(tz=UTC) + timedelta(days=1),
        mark_status_accepted=True,
    )

    monkeypatch.setattr("app.api.verifier.make_client", lambda rpc_url: object())
    monkeypatch.setattr(
        "app.xrpl.ledger.get_did_entry",
        lambda client, account: _did_ledger_entry(issuer_diddoc),
    )
    monkeypatch.setattr("app.credentials.resolver.httpx.get", lambda uri, timeout: JsonResponse(issuer_diddoc))

    response = client.post(
        "/verifier/credentials/verify",
        json={
            "credential": vc,
            "status_mode": "local",
            "xrpl_json_rpc_url": "https://xrpl.example",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["ok"] is True
    assert body["details"]["issuerDidResolution"]["resolver"] == "xrpl"


def test_request_holder_diddoc_is_verified_against_xrpl_hash(tmp_path, monkeypatch):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    holder_document = repository.did_documents.pop(holder_did)
    _save_challenge(repository)
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )
    monkeypatch.setattr(
        "app.xrpl.ledger.get_did_entry",
        lambda client, account: _did_ledger_entry(holder_document),
    )
    resolver = CompositeDidResolver(
        VerifiedStaticDidResolver({holder_did: holder_document}, object(), cache=repository),
        repository,
    )

    result = VerifierService(
        resolver,
        status_lookup=repository.get_credential_entry,
        challenge_lookup=repository.get_verification_challenge,
        challenge_marker=repository.mark_verification_challenge_used,
    ).verify_vp(vp)

    assert result.ok
    assert result.details["holderDidResolution"]["resolver"] == "request-xrpl-verified"
    assert repository.get_did_document(holder_did) == holder_document


def test_request_holder_diddoc_hash_mismatch_fails(tmp_path, monkeypatch):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    holder_document = repository.did_documents[holder_did]
    _save_challenge(repository)
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )
    tampered_document = {**holder_document, "id": holder_did}
    tampered_document["verificationMethod"] = []
    monkeypatch.setattr(
        "app.xrpl.ledger.get_did_entry",
        lambda client, account: _did_ledger_entry(holder_document),
    )
    resolver = CompositeDidResolver(
        VerifiedStaticDidResolver({holder_did: tampered_document}, object()),
        repository,
    )

    result = VerifierService(
        resolver,
        status_lookup=repository.get_credential_entry,
        challenge_lookup=repository.get_verification_challenge,
        challenge_marker=repository.mark_verification_challenge_used,
    ).verify_vp(vp)

    assert not result.ok
    assert any("DID Document hash mismatch" in error for error in result.errors)


def test_xrpl_did_resolver_reports_uri_fetch_failure(monkeypatch):
    did = did_from_account("rExternal")
    did_document = holder_diddoc(did, "holder-key-1", private_key_to_jwk(generate_private_key()))
    monkeypatch.setattr(
        "app.xrpl.ledger.get_did_entry",
        lambda client, account: _did_ledger_entry(did_document, uri="https://dead.example/did.json"),
    )

    def fail_fetch(uri, timeout):
        raise httpx.ConnectError("connection refused")

    monkeypatch.setattr("app.credentials.resolver.httpx.get", fail_fetch)

    try:
        XrplDidResolver(object()).resolve(did)
    except ValueError as exc:
        assert "DID Document fetch failed" in str(exc)
    else:
        raise AssertionError("expected DID fetch failure")


def test_xrpl_did_resolver_reports_missing_ledger_entry(monkeypatch):
    did = did_from_account("rMissing")
    monkeypatch.setattr("app.xrpl.ledger.get_did_entry", lambda client, account: None)

    try:
        XrplDidResolver(object()).resolve(did)
    except ValueError as exc:
        assert "DID ledger entry not found" in str(exc)
    else:
        raise AssertionError("expected DID ledger entry failure")


def test_xrpl_did_resolver_reports_fetched_hash_mismatch(monkeypatch):
    did = did_from_account("rExternal")
    ledger_document = holder_diddoc(did, "holder-key-1", private_key_to_jwk(generate_private_key()))
    fetched_document = {**ledger_document, "verificationMethod": []}
    monkeypatch.setattr(
        "app.xrpl.ledger.get_did_entry",
        lambda client, account: _did_ledger_entry(ledger_document),
    )
    monkeypatch.setattr("app.credentials.resolver.httpx.get", lambda uri, timeout: JsonResponse(fetched_document))

    try:
        XrplDidResolver(object()).resolve(did)
    except ValueError as exc:
        assert "DID Document hash mismatch" in str(exc)
    else:
        raise AssertionError("expected DID hash mismatch")


def test_verifier_challenge_issuance_persists_expiring_challenge(tmp_path):
    repository = _repo(tmp_path)

    class App:
        state = type("State", (), {"repository": repository, "settings": Settings(verifier_challenge_ttl_seconds=300)})()

    class Request:
        app = App()

    response = issue_presentation_challenge(IssuePresentationChallengeRequest(domain="example.com"), Request())
    stored = repository.get_verification_challenge(response.challenge)

    assert stored is not None
    assert response.domain == "example.com"
    assert stored.domain == "example.com"
    assert stored.used_at is None
    assert stored.expires_at > datetime.now(tz=UTC)


def test_vp_verification_uses_issued_challenge_and_marks_it_used(tmp_path):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    _save_challenge(repository)
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    assert verifier.verify_vp(vp).ok
    assert repository.get_verification_challenge("challenge-1").used_at is not None


def test_vp_verification_rejects_wrong_challenge(tmp_path):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    _save_challenge(repository, challenge="challenge-1")
    vp = create_vp(
        holder_did,
        vc,
        "wrong-challenge",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    result = verifier.verify_vp(vp)

    assert not result.ok
    assert "VP challenge was not issued by verifier" in result.errors
    assert repository.get_verification_challenge("challenge-1").used_at is None


def test_vp_verification_rejects_expired_challenge(tmp_path):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    _save_challenge(repository, expires_at=datetime.now(tz=UTC) - timedelta(seconds=1))
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    result = verifier.verify_vp(vp)

    assert not result.ok
    assert "VP challenge is expired" in result.errors
    assert repository.get_verification_challenge("challenge-1").used_at is None


def test_vp_verification_rejects_reused_challenge(tmp_path):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    _save_challenge(repository)
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "example.com",
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    assert verifier.verify_vp(vp).ok
    result = verifier.verify_vp(vp)

    assert not result.ok
    assert "VP challenge was already used" in result.errors


def test_vp_verification_rejects_wrong_domain(tmp_path):
    repository, holder_did, holder_key, vc = _vp_fixture(tmp_path)
    _save_challenge(repository, domain="example.com")
    vp = create_vp(
        holder_did,
        vc,
        "challenge-1",
        "wrong.example",
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    verifier = VerifierService(repository, status_lookup=repository.get_credential_entry)
    result = verifier.verify_vp(vp)

    assert not result.ok
    assert "VP domain mismatch" in result.errors
    assert repository.get_verification_challenge("challenge-1").used_at is None


def test_did_document_route_reads_repository(tmp_path):
    repository = _repo(tmp_path)
    key = generate_private_key()
    service = IssuerService("rIssuer", key, did_document_repository=repository)
    did_document = service.register_did_document()

    class App:
        state = type("State", (), {"repository": repository})()

    class Request:
        app = App()

    assert get_did_document("rIssuer", Request()) == did_document


def test_api_issue_submit_verify_flow(tmp_path):
    repository = _repo(tmp_path)
    app = create_app(
        settings=Settings(),
        repository=repository,
    )
    client = TestClient(app)
    issuer_key = generate_private_key()
    holder_key = generate_private_key()
    holder_account = "rHolder"
    holder_did = did_from_account(holder_account)
    holder_document = holder_diddoc(holder_did, "holder-key-1", private_key_to_jwk(holder_key))

    issue_response = client.post(
        "/issuer/credentials/kyc",
        json={
            "issuer_account": "rIssuer",
            "issuer_private_key_pem": private_key_to_pem(issuer_key),
            "holder_account": holder_account,
            "holder_did": holder_did,
            "claims": {
                "kycLevel": "BASIC",
                "jurisdiction": "KR",
                "assuranceLevel": "BASIC",
            },
            "valid_from": (datetime.now(tz=UTC) - timedelta(minutes=1)).isoformat().replace("+00:00", "Z"),
            "valid_until": (datetime.now(tz=UTC) + timedelta(days=1)).isoformat().replace("+00:00", "Z"),
            "status_mode": "local",
            "mark_status_accepted": True,
        },
    )
    assert issue_response.status_code == 200
    issued = issue_response.json()
    credential = issued["credential"]
    assert isinstance(credential, str)

    status_response = client.get(
        f"/credential-status/credentials/rIssuer/{holder_account}/{issued['credential_type']}?status_mode=local",
    )
    assert status_response.status_code == 200
    assert status_response.json()["active"] is True

    challenge_response = client.post("/verifier/presentations/challenges", json={"domain": "example.com"})
    assert challenge_response.status_code == 200
    challenge = challenge_response.json()
    presentation = create_vp(
        holder_did,
        credential,
        challenge["challenge"],
        challenge["domain"],
        holder_key,
        f"{holder_did}#holder-key-1",
    )

    verify_response = client.post(
        "/verifier/presentations/verify",
        json={
            "presentation": presentation,
            "did_documents": {holder_did: holder_document},
            "policy": {
                "trustedIssuers": ["did:xrpl:1:rIssuer"],
                "acceptedKycLevels": ["BASIC"],
                "acceptedJurisdictions": ["KR"],
            },
            "status_mode": "local",
        },
    )
    assert verify_response.status_code == 200
    assert verify_response.json()["ok"] is True

    replay_response = client.post(
        "/verifier/presentations/verify",
        json={
            "presentation": presentation,
            "did_documents": {holder_did: holder_document},
            "status_mode": "local",
        },
    )
    assert replay_response.status_code == 200
    assert replay_response.json()["ok"] is False
    assert "VP challenge was already used" in replay_response.json()["errors"]


def test_api_issue_uses_env_private_key_path_when_request_omits_it(tmp_path):
    repository = _repo(tmp_path)
    issuer_key = generate_private_key()
    issuer_key_path = tmp_path / "issuer-key.pem"
    issuer_key_path.write_text(private_key_to_pem(issuer_key), encoding="ascii")
    app = create_app(
        settings=Settings(issuer_private_key_pem_path=str(issuer_key_path)),
        repository=repository,
    )
    client = TestClient(app)

    issue_response = client.post(
        "/issuer/credentials/kyc",
        json={
            "issuer_account": "rIssuer",
            "holder_account": "rHolder",
            "claims": {"kycLevel": "BASIC", "jurisdiction": "KR"},
            "valid_from": (datetime.now(tz=UTC) - timedelta(minutes=1)).isoformat().replace("+00:00", "Z"),
            "valid_until": (datetime.now(tz=UTC) + timedelta(days=1)).isoformat().replace("+00:00", "Z"),
            "status_mode": "local",
        },
    )

    assert issue_response.status_code == 200
    assert verify_vc_signature(issue_response.json()["credential"], private_key_to_jwk(issuer_key))
