from urllib.parse import quote


def did_from_account(account: str) -> str:
    return f"did:xrpl:1:{account}"


def account_from_did(did: str) -> str:
    parts = did.split(":")
    if len(parts) != 4 or parts[0] != "did" or parts[1] != "xrpl" or parts[2] != "1":
        raise ValueError(f"unsupported DID: {did}")
    return parts[3]


def issuer_diddoc(did: str, key_id: str, public_jwk: dict) -> dict:
    vm_id = f"{did}#{key_id}"
    return {
        "@context": [
            "https://www.w3.org/ns/did/v1",
            "https://w3id.org/security/jwk/v1",
        ],
        "id": did,
        "verificationMethod": [
            {
                "id": vm_id,
                "type": "JsonWebKey",
                "controller": did,
                "publicKeyJwk": public_jwk,
            }
        ],
        "assertionMethod": [vm_id],
    }


def holder_diddoc(did: str, key_id: str, public_jwk: dict) -> dict:
    vm_id = f"{did}#{key_id}"
    return {
        "@context": [
            "https://www.w3.org/ns/did/v1",
            "https://w3id.org/security/jwk/v1",
        ],
        "id": did,
        "verificationMethod": [
            {
                "id": vm_id,
                "type": "JsonWebKey",
                "controller": did,
                "publicKeyJwk": public_jwk,
            }
        ],
        "authentication": [vm_id],
    }


def issuer_diddoc_multi(did: str, keys: list[tuple[str, dict]], assertion_key_ids: list[str]) -> dict:
    methods = []
    for key_id, jwk in keys:
        methods.append(
            {
                "id": f"{did}#{key_id}",
                "type": "JsonWebKey",
                "controller": did,
                "publicKeyJwk": jwk,
            }
        )
    return {
        "@context": [
            "https://www.w3.org/ns/did/v1",
            "https://w3id.org/security/jwk/v1",
        ],
        "id": did,
        "verificationMethod": methods,
        "assertionMethod": [f"{did}#{key_id}" for key_id in assertion_key_ids],
    }


def issuer_diddoc_url(base_url: str) -> str:
    return f"{base_url.rstrip('/')}/issuer/diddoc.json"


def holder_diddoc_url(base_url: str, holder_did: str) -> str:
    return f"{base_url.rstrip('/')}/holder/{quote(holder_did, safe='')}/diddoc.json"

