from typing import Any


def sync_status_mirror(
    repository: Any,
    *,
    issuer_account: str,
    holder_account: str,
    credential_type: str,
    entry: dict[str, Any] | None,
    fallback_flags: int = 0,
    fallback_expiration: int | None = None,
    fallback_uri: str | None = None,
) -> None:
    save = getattr(repository, "save_credential_status", None)
    if not callable(save):
        return

    flags = fallback_flags
    expiration = fallback_expiration
    uri = fallback_uri
    if entry is not None:
        flags = int(entry.get("Flags", entry.get("flags", flags)))
        entry_expiration = entry.get("Expiration", entry.get("expiration"))
        if entry_expiration is not None:
            expiration = int(entry_expiration)
        entry_uri = entry.get("URI", entry.get("uri"))
        if entry_uri is not None:
            uri = str(entry_uri)

    save(
        issuer_account=issuer_account,
        holder_account=holder_account,
        credential_type=credential_type,
        flags=flags,
        expiration=expiration,
        uri=uri,
    )


def remove_status_mirror(
    repository: Any,
    *,
    issuer_account: str,
    holder_account: str,
    credential_type: str,
) -> None:
    revoke = getattr(repository, "revoke_credential_status", None)
    if callable(revoke):
        revoke(
            issuer_account=issuer_account,
            holder_account=holder_account,
            credential_type=credential_type,
        )
