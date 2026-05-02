from typing import Any

VC_CONTEXTS = [
    "https://www.w3.org/ns/credentials/v2",
    "https://example.com/contexts/kyc-v1",
]
VP_CONTEXTS = ["https://www.w3.org/ns/credentials/v2"]

VERIFIABLE_CREDENTIAL_TYPE = "VerifiableCredential"
VERIFIABLE_PRESENTATION_TYPE = "VerifiablePresentation"
KYC_CREDENTIAL_TYPE = "KycCredential"
XRPL_CREDENTIAL_STATUS_TYPE = "XRPLCredentialStatus"
REVOCATION_STATUS_PURPOSE = "revocation"


def kyc_credential_subject(holder_did: str, claims: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": holder_did,
        **claims,
    }
