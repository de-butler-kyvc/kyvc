from collections.abc import Mapping
from typing import Any, Protocol

import httpx

from app.credentials.canonical import canonical_json, multihash_sha2_256
from app.credentials.did import account_from_did
from app.credentials.hexutil import bytes_to_hex, hex_to_utf8


class DidResolver(Protocol):
    def resolve(self, did: str) -> dict[str, Any]:
        """Resolve a DID into a DID resolution result."""


def find_verification_method(did_document: dict[str, Any], verification_method_id: str) -> dict[str, Any] | None:
    for method in did_document.get("verificationMethod", []):
        if method.get("id") == verification_method_id:
            return method
    return None


def did_resolution_result(did: str, did_document: dict[str, Any], metadata: dict[str, Any] | None = None) -> dict[str, Any]:
    if did_document.get("id") != did:
        raise ValueError(f"DID Document id mismatch for {did}")
    for method in did_document.get("verificationMethod", []):
        if method.get("controller") != did:
            raise ValueError(f"verificationMethod controller mismatch for {method.get('id')}")
    return {
        "didResolutionMetadata": {
            "contentType": "application/did+json",
            **(metadata or {}),
        },
        "didDocument": did_document,
        "didDocumentMetadata": {},
    }


class StaticDidResolver:
    def __init__(self, did_documents: Mapping[str, dict[str, Any]]):
        self.did_documents = did_documents

    def resolve(self, did: str) -> dict[str, Any]:
        did_document = self.did_documents.get(did)
        if not did_document:
            raise ValueError(f"DID Document not found for {did}")
        return did_resolution_result(did, did_document, {"resolver": "static"})


class CompositeDidResolver:
    def __init__(self, *resolvers: DidResolver):
        self.resolvers = resolvers

    def resolve(self, did: str) -> dict[str, Any]:
        errors: list[str] = []
        for resolver in self.resolvers:
            try:
                return resolver.resolve(did)
            except Exception as exc:
                errors.append(str(exc))
        raise ValueError(f"DID resolution failed for {did}: {'; '.join(errors)}")


class XrplDidResolver:
    def __init__(self, client: Any):
        self.client = client

    def resolve(self, did: str) -> dict[str, Any]:
        from app.xrpl.ledger import get_did_entry

        account = account_from_did(did)
        entry = get_did_entry(self.client, account)
        if not entry:
            raise ValueError(f"DID ledger entry not found for {did}")

        uri_hex = str(entry.get("URI") or entry.get("uri") or "")
        data_hex = str(entry.get("Data") or entry.get("data") or "").upper()
        if not uri_hex or not data_hex:
            raise ValueError(f"DID ledger entry missing URI/Data for {did}")

        uri = hex_to_utf8(uri_hex)
        response = httpx.get(uri, timeout=30)
        response.raise_for_status()
        did_document = response.json()

        computed_hash = bytes_to_hex(multihash_sha2_256(canonical_json(did_document)))
        if computed_hash != data_hex:
            raise ValueError(f"DID Document hash mismatch for {did}: ledger={data_hex} fetched={computed_hash}")

        result = did_resolution_result(did, did_document, {"ledger": "xrpl", "account": account})
        result["didDocumentMetadata"] = {
            "uri": uri,
            "dataHash": data_hex,
        }
        return result

