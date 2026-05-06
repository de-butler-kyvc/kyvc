import json
from collections.abc import Mapping
from datetime import UTC, datetime
from typing import Any

import pymysql
from pymysql.connections import Connection
from pymysql.cursors import DictCursor

from app.credentials.resolver import did_resolution_result
from app.storage.interfaces import VerificationChallengeEntry


class MySQLRepository:
    def __init__(
        self,
        *,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        charset: str = "utf8mb4",
        connect_timeout: int = 10,
    ):
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        self.charset = charset
        self.connect_timeout = connect_timeout
        self._schema_initialized = False

    def _connect(self, *, init_schema: bool = True) -> Connection:
        if init_schema and not self._schema_initialized:
            self._init_schema()
        return pymysql.connect(
            host=self.host,
            port=self.port,
            database=self.database,
            user=self.user,
            password=self.password,
            charset=self.charset,
            cursorclass=DictCursor,
            autocommit=True,
            connect_timeout=self.connect_timeout,
        )

    def _init_schema(self) -> None:
        statements = [
            """
            CREATE TABLE IF NOT EXISTS did_documents (
                did VARCHAR(255) PRIMARY KEY,
                document_json LONGTEXT NOT NULL,
                updated_at VARCHAR(32) NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS issued_credentials (
                credential_id VARCHAR(255) PRIMARY KEY,
                issuer_did VARCHAR(255) NOT NULL,
                issuer_account VARCHAR(128) NOT NULL,
                holder_did VARCHAR(255) NOT NULL,
                holder_account VARCHAR(128) NOT NULL,
                credential_type VARCHAR(160) NOT NULL,
                vc_core_hash VARCHAR(128) NOT NULL,
                vc_json LONGTEXT NOT NULL,
                created_at VARCHAR(32) NOT NULL,
                revoked_at VARCHAR(32) NULL,
                INDEX idx_issued_credentials_status (
                    issuer_account, holder_account, credential_type
                )
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS credential_status (
                issuer_account VARCHAR(128) NOT NULL,
                holder_account VARCHAR(128) NOT NULL,
                credential_type VARCHAR(160) NOT NULL,
                flags INT NOT NULL,
                expiration BIGINT NULL,
                uri TEXT NULL,
                updated_at VARCHAR(32) NOT NULL,
                PRIMARY KEY (issuer_account, holder_account, credential_type)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS verification_logs (
                id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                subject_id VARCHAR(255) NULL,
                ok TINYINT(1) NOT NULL,
                errors_json LONGTEXT NOT NULL,
                details_json LONGTEXT NOT NULL,
                verified_at VARCHAR(32) NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS verification_challenges (
                challenge VARCHAR(255) PRIMARY KEY,
                domain VARCHAR(255) NOT NULL,
                expires_at VARCHAR(32) NOT NULL,
                used_at VARCHAR(32) NULL,
                created_at VARCHAR(32) NOT NULL,
                presentation_definition_json LONGTEXT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
        ]
        with self._connect(init_schema=False) as connection:
            with connection.cursor() as cursor:
                for statement in statements:
                    cursor.execute(statement)
                try:
                    cursor.execute(
                        """
                        ALTER TABLE verification_challenges
                        ADD COLUMN presentation_definition_json LONGTEXT NULL
                        """
                    )
                except pymysql.err.OperationalError as exc:
                    if exc.args[0] != 1060:
                        raise
        self._schema_initialized = True

    @staticmethod
    def _json(data: Mapping[str, Any] | list[Any]) -> str:
        return json.dumps(data, ensure_ascii=False, sort_keys=True, separators=(",", ":"))

    @staticmethod
    def _now() -> str:
        return datetime.now(tz=UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    @staticmethod
    def _datetime(value: datetime) -> str:
        return value.astimezone(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z")

    @staticmethod
    def _parse_datetime(value: str) -> datetime:
        return datetime.fromisoformat(value.replace("Z", "+00:00")).astimezone(UTC)

    def save_did_document(self, did: str, did_document: dict[str, Any]) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO did_documents (did, document_json, updated_at)
                    VALUES (%s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        document_json = VALUES(document_json),
                        updated_at = VALUES(updated_at)
                    """,
                    (did, self._json(did_document), self._now()),
                )

    def get_did_document(self, did: str) -> dict[str, Any] | None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute("SELECT document_json FROM did_documents WHERE did = %s", (did,))
                row = cursor.fetchone()
        if row is None:
            return None
        data = json.loads(str(row["document_json"]))
        return data if isinstance(data, dict) else None

    def resolve(self, did: str) -> dict[str, Any]:
        did_document = self.get_did_document(did)
        if did_document is None:
            raise ValueError(f"DID Document not found in mysql for {did}")
        return did_resolution_result(did, did_document, {"resolver": "mysql"})

    def save_issued_credential(
        self,
        *,
        vc: dict[str, Any],
        issuer_did: str,
        issuer_account: str,
        holder_did: str,
        holder_account: str,
        credential_type: str,
        vc_core_hash: str,
    ) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO issued_credentials (
                        credential_id, issuer_did, issuer_account, holder_did, holder_account,
                        credential_type, vc_core_hash, vc_json, created_at
                    )
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        vc_json = VALUES(vc_json),
                        credential_type = VALUES(credential_type),
                        vc_core_hash = VALUES(vc_core_hash)
                    """,
                    (
                        str(vc["id"]),
                        issuer_did,
                        issuer_account,
                        holder_did,
                        holder_account,
                        credential_type,
                        vc_core_hash,
                        self._json(vc),
                        self._now(),
                    ),
                )

    def save_credential_status(
        self,
        *,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
        flags: int,
        expiration: int | None = None,
        uri: str | None = None,
    ) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO credential_status (
                        issuer_account, holder_account, credential_type,
                        flags, expiration, uri, updated_at
                    )
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                    ON DUPLICATE KEY UPDATE
                        flags = VALUES(flags),
                        expiration = VALUES(expiration),
                        uri = VALUES(uri),
                        updated_at = VALUES(updated_at)
                    """,
                    (
                        issuer_account,
                        holder_account,
                        credential_type,
                        flags,
                        expiration,
                        uri,
                        self._now(),
                    ),
                )

    def get_credential_entry(
        self,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
    ) -> dict[str, Any] | None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT issuer_account, holder_account, credential_type, flags, expiration, uri
                    FROM credential_status
                    WHERE issuer_account = %s AND holder_account = %s AND credential_type = %s
                    """,
                    (issuer_account, holder_account, credential_type),
                )
                row = cursor.fetchone()
        if row is None:
            return None
        entry: dict[str, Any] = {
            "Issuer": row["issuer_account"],
            "Subject": row["holder_account"],
            "CredentialType": row["credential_type"],
            "Flags": int(row["flags"]),
        }
        if row["expiration"] is not None:
            entry["Expiration"] = int(row["expiration"])
        if row["uri"] is not None:
            entry["URI"] = row["uri"]
        return entry

    def revoke_credential_status(
        self,
        *,
        issuer_account: str,
        holder_account: str,
        credential_type: str,
    ) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    DELETE FROM credential_status
                    WHERE issuer_account = %s AND holder_account = %s AND credential_type = %s
                    """,
                    (issuer_account, holder_account, credential_type),
                )
                cursor.execute(
                    """
                    UPDATE issued_credentials
                    SET revoked_at = %s
                    WHERE issuer_account = %s AND holder_account = %s AND credential_type = %s
                    """,
                    (self._now(), issuer_account, holder_account, credential_type),
                )

    def save_verification_result(
        self,
        *,
        subject_id: str | None,
        ok: bool,
        errors: list[str],
        details: dict[str, Any],
        verified_at: datetime,
    ) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO verification_logs (subject_id, ok, errors_json, details_json, verified_at)
                    VALUES (%s, %s, %s, %s, %s)
                    """,
                    (
                        subject_id,
                        1 if ok else 0,
                        self._json(errors),
                        self._json(details),
                        self._datetime(verified_at),
                    ),
                )

    def save_verification_challenge(
        self,
        *,
        challenge: str,
        domain: str,
        expires_at: datetime,
        created_at: datetime,
        presentation_definition: dict[str, Any] | None = None,
    ) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO verification_challenges (
                        challenge, domain, expires_at, used_at, created_at, presentation_definition_json
                    )
                    VALUES (%s, %s, %s, NULL, %s, %s)
                    """,
                    (
                        challenge,
                        domain,
                        self._datetime(expires_at),
                        self._datetime(created_at),
                        self._json(presentation_definition) if presentation_definition is not None else None,
                    ),
                )

    def get_verification_challenge(self, challenge: str) -> VerificationChallengeEntry | None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    SELECT challenge, domain, expires_at, used_at, created_at, presentation_definition_json
                    FROM verification_challenges
                    WHERE challenge = %s
                    """,
                    (challenge,),
                )
                row = cursor.fetchone()
        if row is None:
            return None
        used_at = row["used_at"]
        created_at = row["created_at"]
        presentation_definition = row.get("presentation_definition_json")
        return VerificationChallengeEntry(
            challenge=str(row["challenge"]),
            domain=str(row["domain"]),
            expires_at=self._parse_datetime(str(row["expires_at"])),
            used_at=self._parse_datetime(str(used_at)) if used_at is not None else None,
            created_at=self._parse_datetime(str(created_at)) if created_at is not None else None,
            presentation_definition=(
                json.loads(str(presentation_definition)) if presentation_definition is not None else None
            ),
        )

    def mark_verification_challenge_used(self, challenge: str, used_at: datetime) -> bool:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE verification_challenges
                    SET used_at = %s
                    WHERE challenge = %s AND used_at IS NULL
                    """,
                    (self._datetime(used_at), challenge),
                )
                return cursor.rowcount == 1
