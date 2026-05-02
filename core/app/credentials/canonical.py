import hashlib
import json
from typing import Any


def canonical_json(obj: Any) -> bytes:
    """PoC deterministic JSON bytes. This is not JSON-LD RDF normalization."""
    return json.dumps(
        obj,
        sort_keys=True,
        separators=(",", ":"),
        ensure_ascii=False,
    ).encode("utf-8")


def sha256_bytes(data: bytes) -> bytes:
    return hashlib.sha256(data).digest()


def multihash_sha2_256(data: bytes) -> bytes:
    return bytes.fromhex("1220") + sha256_bytes(data)

