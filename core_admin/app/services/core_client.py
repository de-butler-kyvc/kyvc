from __future__ import annotations

from typing import Any

import httpx

from app.core.config import Settings


class CoreClientError(RuntimeError):
    def __init__(self, status_code: int, detail: Any):
        super().__init__(str(detail))
        self.status_code = status_code
        self.detail = detail


class CoreClient:
    def __init__(self, settings: Settings):
        self.base_url = settings.core_base_url.rstrip("/")
        self.timeout = settings.core_request_timeout_seconds

    def get_internal_status(self) -> dict[str, Any]:
        return self._request("GET", "/internal/status")

    def get_provider_options(self) -> dict[str, Any]:
        return self._request("GET", "/internal/provider-selections/options")

    def get_provider_selections(self) -> dict[str, Any]:
        return self._request("GET", "/internal/provider-selections")

    def update_provider_selection(
        self,
        category: str,
        *,
        provider: str,
        profile: str,
        changed_by: str | None,
    ) -> dict[str, Any]:
        payload = {
            "provider": provider,
            "profile": profile,
            "changed_by": changed_by,
        }
        return self._request("PUT", f"/internal/provider-selections/{category}", json=payload)

    def get_provider_history(self, *, limit: int) -> dict[str, Any]:
        return self._request("GET", "/internal/provider-selections/history", params={"limit": limit})

    def _request(self, method: str, path: str, **kwargs: Any) -> dict[str, Any]:
        url = f"{self.base_url}{path}"
        try:
            response = httpx.request(method, url, timeout=self.timeout, **kwargs)
        except httpx.TimeoutException as exc:
            raise CoreClientError(504, "Core internal API request timed out") from exc
        except httpx.RequestError as exc:
            raise CoreClientError(502, f"Core internal API request failed: {exc.__class__.__name__}") from exc

        if response.status_code >= 400:
            raise CoreClientError(response.status_code, _response_detail(response))

        try:
            data = response.json()
        except ValueError as exc:
            raise CoreClientError(502, "Core internal API returned a non-JSON response") from exc
        if not isinstance(data, dict):
            raise CoreClientError(502, "Core internal API returned an unexpected response shape")
        return data


def _response_detail(response: httpx.Response) -> Any:
    try:
        data = response.json()
    except ValueError:
        return response.text
    if isinstance(data, dict) and "detail" in data:
        return data["detail"]
    return data
