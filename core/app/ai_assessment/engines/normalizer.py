import re
from datetime import datetime
from typing import Any


class Normalizer:
    COMPANY_TOKENS = ("주식회사", "(주)", "㈜", "주)", "CO.,LTD.", "CO.LTD.", "LTD.", "INC.")

    def digits(self, value: Any) -> str | None:
        if value is None:
            return None
        digits = re.sub(r"\D", "", str(value))
        return digits or None

    def integer(self, value: Any) -> int | None:
        digits = self.digits(value)
        return int(digits) if digits else None

    def percent(self, value: Any) -> float | None:
        if value is None:
            return None
        if isinstance(value, int | float):
            return float(value)
        match = re.search(r"[-+]?\d+(?:\.\d+)?", str(value).replace(",", ""))
        return float(match.group(0)) if match else None

    def date(self, value: Any) -> str | None:
        if value is None:
            return None
        cleaned = str(value).strip()
        candidates = [
            cleaned,
            cleaned.replace(".", "-").replace("/", "-").replace("년", "-").replace("월", "-").replace("일", ""),
        ]
        for candidate in candidates:
            candidate = re.sub(r"\s+", "", candidate).strip("-")
            for fmt in ("%Y-%m-%d", "%Y%m%d", "%y%m%d"):
                try:
                    return datetime.strptime(candidate, fmt).date().isoformat()
                except ValueError:
                    pass
        return None

    def person_name(self, value: Any) -> str | None:
        return self._compact(value)

    def company_name(self, value: Any) -> str | None:
        text = self._compact(value)
        if text is None:
            return None
        for token in self.COMPANY_TOKENS:
            text = text.replace(token.upper(), "")
        text = text.replace("(", "").replace(")", "")
        return text or None

    def canonical_company_name(self, value: Any) -> str | None:
        return self.company_name(value)

    def _compact(self, value: Any) -> str | None:
        if value is None:
            return None
        text = "".join(str(value).upper().split())
        return text or None
