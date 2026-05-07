import re
from typing import Any


class Normalizer:
    def digits(self, value: Any) -> str | None:
        if value is None:
            return None
        digits = re.sub(r"\D", "", str(value))
        return digits or None

    def person_name(self, value: Any) -> str | None:
        return self._compact(value)

    def company_name(self, value: Any) -> str | None:
        text = self._compact(value)
        if text is None:
            return None
        for token in ("주식회사", "(주)", "㈜", "CO.,LTD.", "CO.LTD.", "LTD.", "INC."):
            text = text.replace(token.upper(), "")
        return text or None

    def _compact(self, value: Any) -> str | None:
        if value is None:
            return None
        text = "".join(str(value).upper().split())
        return text or None
