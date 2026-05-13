import json
import secrets
from dataclasses import dataclass, field
from typing import Any

from app.credentials.canonical import canonical_json, sha256_bytes
from app.credentials.crypto import b64url_decode, b64url_encode

SD_DIGEST_ALG = "sha-256"
SD_CLAIM_NAME = "_sd"
ARRAY_DIGEST_CLAIM_NAME = "..."
SALT_BYTES = 32


class DisclosureError(ValueError):
    pass


@dataclass
class DisclosureBuildResult:
    payload: dict[str, Any]
    disclosures: list[str]
    disclosable_paths: list[str]


@dataclass
class DisclosureVerificationResult:
    disclosed_payload: dict[str, Any]
    digests: set[str] = field(default_factory=set)
    paths: set[str] = field(default_factory=set)


def _salt() -> str:
    return secrets.token_urlsafe(SALT_BYTES)


def digest_for_disclosure(disclosure: str) -> str:
    return b64url_encode(sha256_bytes(disclosure.encode("ascii")))


def encode_property_disclosure(name: str, value: Any, *, salt: str | None = None) -> str:
    return b64url_encode(canonical_json([salt or _salt(), name, value]))


def encode_array_disclosure(value: Any, *, salt: str | None = None) -> str:
    return b64url_encode(canonical_json([salt or _salt(), value]))


def decode_disclosure(disclosure: str) -> list[Any]:
    try:
        decoded = json.loads(b64url_decode(disclosure))
    except Exception as exc:
        raise DisclosureError("malformed disclosure") from exc
    if not isinstance(decoded, list) or len(decoded) not in {2, 3}:
        raise DisclosureError("malformed disclosure")
    if not isinstance(decoded[0], str) or not decoded[0]:
        raise DisclosureError("malformed disclosure salt")
    if len(decoded) == 3 and not isinstance(decoded[1], str):
        raise DisclosureError("malformed disclosure claim name")
    return decoded


def _add_digest(target: dict[str, Any], digest: str) -> None:
    existing = target.setdefault(SD_CLAIM_NAME, [])
    if not isinstance(existing, list):
        raise DisclosureError("_sd must be an array")
    existing.append(digest)


def _disclose_property(target: dict[str, Any], name: str, path: str, disclosures: list[str], paths: list[str]) -> None:
    if name not in target:
        return
    disclosure = encode_property_disclosure(name, target.pop(name))
    _add_digest(target, digest_for_disclosure(disclosure))
    disclosures.append(disclosure)
    paths.append(path)


def _disclose_array_elements(target: dict[str, Any], name: str, path: str, disclosures: list[str], paths: list[str]) -> None:
    value = target.get(name)
    if not isinstance(value, list):
        return
    transformed: list[Any] = []
    for item in value:
        disclosure = encode_array_disclosure(item)
        transformed.append({ARRAY_DIGEST_CLAIM_NAME: digest_for_disclosure(disclosure)})
        disclosures.append(disclosure)
        paths.append(path)
    target[name] = transformed


def build_sd_payload(payload: dict[str, Any]) -> DisclosureBuildResult:
    """Transform KYvC legal entity KYC claims into an issuer-signed SD payload."""

    transformed = json.loads(json.dumps(payload))
    disclosures: list[str] = []
    paths: list[str] = []

    for object_name in ("legalEntity", "representative", "delegate", "delegation", "establishmentPurpose"):
        obj = transformed.get(object_name)
        if isinstance(obj, dict):
            for claim_name in list(obj.keys()):
                _disclose_property(obj, claim_name, f"{object_name}.{claim_name}", disclosures, paths)

    kyc = transformed.get("kyc")
    if isinstance(kyc, dict):
        for claim_name in list(kyc.keys()):
            if claim_name not in {"jurisdiction", "assuranceLevel"}:
                _disclose_property(kyc, claim_name, f"kyc.{claim_name}", disclosures, paths)

    _disclose_array_elements(transformed, "beneficialOwners", "beneficialOwners[]", disclosures, paths)
    _disclose_array_elements(transformed, "documentEvidence", "documentEvidence[]", disclosures, paths)

    return DisclosureBuildResult(payload=transformed, disclosures=disclosures, disclosable_paths=paths)


def _referenced_digests(value: Any) -> set[str]:
    digests: set[str] = set()
    if isinstance(value, dict):
        sd = value.get(SD_CLAIM_NAME)
        if sd is not None:
            if not isinstance(sd, list) or not all(isinstance(item, str) for item in sd):
                raise DisclosureError("_sd must contain disclosure digests")
            digests.update(sd)
        for key, nested in value.items():
            if key != SD_CLAIM_NAME:
                digests.update(_referenced_digests(nested))
    elif isinstance(value, list):
        for item in value:
            if isinstance(item, dict) and set(item.keys()) == {ARRAY_DIGEST_CLAIM_NAME}:
                digest = item[ARRAY_DIGEST_CLAIM_NAME]
                if not isinstance(digest, str):
                    raise DisclosureError("array disclosure digest must be a string")
                digests.add(digest)
            else:
                digests.update(_referenced_digests(item))
    return digests


def _claim_path(prefix: str, name: str) -> str:
    return f"{prefix}.{name}" if prefix else name


def reconstruct_disclosed_payload(sd_payload: dict[str, Any], disclosures: list[str]) -> DisclosureVerificationResult:
    referenced = _referenced_digests(sd_payload)
    seen_digests: set[str] = set()
    decoded_by_digest: dict[str, list[Any]] = {}
    for disclosure in disclosures:
        decoded = decode_disclosure(disclosure)
        digest = digest_for_disclosure(disclosure)
        if digest in seen_digests:
            raise DisclosureError("duplicate disclosure")
        if digest not in referenced:
            raise DisclosureError("disclosure digest is not referenced by issuer payload")
        seen_digests.add(digest)
        decoded_by_digest[digest] = decoded

    def walk(value: Any, prefix: str = "") -> Any:
        if isinstance(value, dict):
            out: dict[str, Any] = {}
            sd = value.get(SD_CLAIM_NAME)
            if sd is not None:
                if not isinstance(sd, list):
                    raise DisclosureError("_sd must be an array")
                names_seen: set[str] = set()
                for digest in sd:
                    decoded = decoded_by_digest.get(digest)
                    if decoded is None:
                        continue
                    if len(decoded) != 3:
                        raise DisclosureError("object property disclosure must have three elements")
                    _, name, claim_value = decoded
                    if name in names_seen or name in out:
                        raise DisclosureError("duplicate disclosed claim")
                    names_seen.add(name)
                    out[name] = walk(claim_value, _claim_path(prefix, name))
                result_paths.update(_claim_path(prefix, name) for name in names_seen)
            for key, nested in value.items():
                if key == SD_CLAIM_NAME:
                    continue
                out[key] = walk(nested, _claim_path(prefix, key))
            return out
        if isinstance(value, list):
            out_items: list[Any] = []
            for item in value:
                if isinstance(item, dict) and set(item.keys()) == {ARRAY_DIGEST_CLAIM_NAME}:
                    digest = item[ARRAY_DIGEST_CLAIM_NAME]
                    decoded = decoded_by_digest.get(digest)
                    if decoded is None:
                        continue
                    if len(decoded) != 2:
                        raise DisclosureError("array element disclosure must have two elements")
                    out_items.append(walk(decoded[1], f"{prefix}[]"))
                    result_paths.add(f"{prefix}[]")
                else:
                    out_items.append(walk(item, prefix))
            return out_items
        return value

    result_paths: set[str] = set()
    disclosed = walk(sd_payload)
    return DisclosureVerificationResult(disclosed_payload=disclosed, digests=seen_digests, paths=result_paths)


def strip_sd_metadata(value: Any) -> Any:
    if isinstance(value, dict):
        return {key: strip_sd_metadata(item) for key, item in value.items() if key != SD_CLAIM_NAME}
    if isinstance(value, list):
        return [
            strip_sd_metadata(item)
            for item in value
            if not (isinstance(item, dict) and set(item.keys()) == {ARRAY_DIGEST_CLAIM_NAME})
        ]
    return value
