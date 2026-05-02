import re

_HEX_RE = re.compile(r"^[0-9a-fA-F]*$")


def is_hex(value: str) -> bool:
    return len(value) % 2 == 0 and bool(_HEX_RE.fullmatch(value))


def ensure_upper_hex(value: str) -> str:
    if not is_hex(value):
        raise ValueError(f"not hex: {value!r}")
    return value.upper()


def bytes_to_hex(value: bytes) -> str:
    return value.hex().upper()


def hex_to_bytes(value: str) -> bytes:
    return bytes.fromhex(ensure_upper_hex(value))


def utf8_to_hex(value: str) -> str:
    return bytes_to_hex(value.encode("utf-8"))


def hex_to_utf8(value: str) -> str:
    return hex_to_bytes(value).decode("utf-8")

