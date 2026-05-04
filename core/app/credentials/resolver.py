from collections.abc import Mapping
from typing import Any, Protocol

import httpx

from app.credentials.canonical import canonical_json, multihash_sha2_256
from app.credentials.did import account_from_did
from app.credentials.hexutil import bytes_to_hex, hex_to_utf8


class DidResolver(Protocol):
    def resolve(self, did: str) -> dict[str, Any]:
        """Resolve a DID into a DID resolution result."""


class DidResolutionTerminalError(ValueError):
    """A resolver found the DID but determined it must not fall back."""


class DidDocumentCache(Protocol):
    def save_did_document(self, did: str, did_document: dict[str, Any]) -> None:
        """Persist a DID Document cache entry."""


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


class VerifiedStaticDidResolver:
    def __init__(
        self,
        did_documents: Mapping[str, dict[str, Any]],
        client: Any,
        *,
        cache: DidDocumentCache | None = None,
    ):
        self.did_documents = did_documents
        self.client = client
        self.cache = cache

    def resolve(self, did: str) -> dict[str, Any]:
        did_document = self.did_documents.get(did)
        if not did_document:
            raise ValueError(f"DID Document not found in request for {did}")

        try:
            account, uri, data_hash = xrpl_did_entry_metadata(self.client, did)
            verify_did_document_hash(did, did_document, data_hash)
            if self.cache is not None:
                self.cache.save_did_document(did, did_document)
        except Exception as exc:
            raise DidResolutionTerminalError(str(exc)) from exc

        result = did_resolution_result(
            did,
            did_document,
            {
                "resolver": "request-xrpl-verified",
                "ledger": "xrpl",
                "account": account,
            },
        )
        result["didDocumentMetadata"] = {
            "uri": uri,
            "dataHash": data_hash,
            "cached": self.cache is not None,
        }
        return result


class CompositeDidResolver:
    def __init__(self, *resolvers: DidResolver):
        self.resolvers = resolvers

    def resolve(self, did: str) -> dict[str, Any]:
        errors: list[str] = []
        for resolver in self.resolvers:
            try:
                return resolver.resolve(did)
            except DidResolutionTerminalError as exc:
                raise ValueError(str(exc)) from exc
            except Exception as exc:
                errors.append(str(exc))
        raise ValueError(f"DID resolution failed for {did}: {'; '.join(errors)}")


class CachingDidResolver:
    def __init__(self, resolver: DidResolver, cache: DidDocumentCache):
        self.resolver = resolver
        self.cache = cache

    def resolve(self, did: str) -> dict[str, Any]:
        result = self.resolver.resolve(did)
        self.cache.save_did_document(did, result["didDocument"])
        metadata = dict(result.get("didDocumentMetadata") or {})
        metadata["cached"] = True
        result["didDocumentMetadata"] = metadata
        return result


class VerifiedCachedDidResolver:
    def __init__(self, resolver: DidResolver, client: Any):
        self.resolver = resolver
        self.client = client

    def resolve(self, did: str) -> dict[str, Any]:
        result = self.resolver.resolve(did)
        did_document = result["didDocument"]
        account, uri, data_hash = xrpl_did_entry_metadata(self.client, did)
        verify_did_document_hash(did, did_document, data_hash)

        metadata = dict(result.get("didResolutionMetadata") or {})
        original_resolver = metadata.get("resolver")
        metadata.update(
            {
                "resolver": "cache-xrpl-verified",
                "cacheResolver": original_resolver,
                "ledger": "xrpl",
                "account": account,
            }
        )
        result["didResolutionMetadata"] = metadata
        document_metadata = dict(result.get("didDocumentMetadata") or {})
        document_metadata.update(
            {
                "uri": uri,
                "dataHash": data_hash,
                "cacheVerified": True,
            }
        )
        result["didDocumentMetadata"] = document_metadata
        return result


def did_document_hash(did_document: dict[str, Any]) -> str:
    return bytes_to_hex(multihash_sha2_256(canonical_json(did_document)))


def verify_did_document_hash(did: str, did_document: dict[str, Any], expected_hash: str) -> None:
    computed_hash = did_document_hash(did_document)
    if computed_hash != expected_hash:
        raise ValueError(f"DID Document hash mismatch for {did}: ledger={expected_hash} fetched={computed_hash}")


def xrpl_did_entry_metadata(client: Any, did: str) -> tuple[str, str, str]:
    from app.xrpl.ledger import get_did_entry

    account = account_from_did(did)
    entry = get_did_entry(client, account)
    if not entry:
        raise ValueError(f"DID ledger entry not found for {did}")

    uri_hex = str(entry.get("URI") or entry.get("uri") or "")
    data_hex = str(entry.get("Data") or entry.get("data") or "").upper()
    if not uri_hex or not data_hex:
        raise ValueError(f"DID ledger entry missing URI/Data for {did}")
    return account, hex_to_utf8(uri_hex), data_hex


class XrplDidResolver:
    def __init__(self, client: Any):
        self.client = client

    def resolve(self, did: str) -> dict[str, Any]:
        account, uri, data_hash = xrpl_did_entry_metadata(self.client, did)
        try:
            response = httpx.get(uri, timeout=30)
            response.raise_for_status()
        except httpx.HTTPError as exc:
            raise ValueError(f"DID Document fetch failed for {did}: uri={uri}: {exc}") from exc
        did_document = response.json()

        verify_did_document_hash(did, did_document, data_hash)

        result = did_resolution_result(did, did_document, {"resolver": "xrpl", "ledger": "xrpl", "account": account})
        result["didDocumentMetadata"] = {
            "uri": uri,
            "dataHash": data_hash,
        }
        return result
