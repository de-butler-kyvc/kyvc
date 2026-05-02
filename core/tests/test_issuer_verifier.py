from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient

from app.credentials.resolver import did_resolution_result
from app.credentials.crypto import generate_private_key, private_key_to_jwk
from app.credentials.crypto import private_key_to_pem
from app.credentials.did import did_from_account, holder_diddoc
from app.credentials.vc import build_kyc_vc, credential_type_hex_for_vc, verify_vc_signature
from app.credentials.vp import create_vp
from app.api.did import get_did_document
from app.api.verifier import issue_presentation_challenge
from app.core.config import Settings
from app.main import create_app
from app.issuer.service import IssuerService
from app.storage.interfaces import VerificationChallengeEntry
from app.verifier.api_models import IssuePresentationChallengeRequest
from app.verifier.policy import VerificationPolicy
from app.verifier.service import VerifierService


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
    original_credential_type = vc["credentialStatus"]["credentialType"]
    vc["credentialSalt"] = "tampered-salt"
    assert not verify_vc_signature(vc, private_key_to_jwk(key))
    assert credential_type_hex_for_vc(vc) != original_credential_type


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
