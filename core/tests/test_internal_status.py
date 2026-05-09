from fastapi.testclient import TestClient

from app.core.config import Settings
from app.main import create_app


class ReadyRepository:
    def readiness_probe(self):
        return None


class FakeXrplClient:
    def request(self, request):
        return {"status": "success", "info": {"complete_ledgers": "1-2"}}


class ErrorXrplClient:
    def request(self, request):
        return {"status": "error", "error": "notReady"}


def _client(settings: Settings | None = None) -> TestClient:
    app = create_app(
        settings=settings
        or Settings(
            xrpl_issuer_seed="issuer-seed",
        ),
        repository=ReadyRepository(),
    )
    return TestClient(app)


def test_internal_status_returns_structured_status(monkeypatch):
    monkeypatch.setattr("app.internal_status.service.make_client", lambda rpc_url: FakeXrplClient())
    client = _client()

    response = client.get("/internal/status")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert body["service"] == "kyvc-core"
    assert body["environment"] == "dev"
    assert body["components"]["database"]["status"] == "UP"
    assert body["components"]["xrpl"]["status"] == "UP"
    assert body["components"]["xrpl"]["configured"] is True
    assert body["components"]["issuer"]["status"] == "UP"
    assert "xrplNetwork" in body["diagnostics"]


def test_internal_status_reports_xrpl_probe_failure(monkeypatch):
    def fail_client(rpc_url):
        raise TimeoutError("timeout")

    monkeypatch.setattr("app.internal_status.service.make_client", fail_client)
    client = _client()

    response = client.get("/internal/status")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "DEGRADED"
    assert body["components"]["xrpl"]["status"] == "DOWN"
    assert body["components"]["xrpl"]["configured"] is True
    assert "TimeoutError" in body["components"]["xrpl"]["detail"]
    assert body["components"]["issuer"]["status"] == "DEGRADED"


def test_internal_status_reports_xrpl_error_response(monkeypatch):
    monkeypatch.setattr("app.internal_status.service.make_client", lambda rpc_url: ErrorXrplClient())
    client = _client()

    response = client.get("/internal/status")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "DEGRADED"
    assert body["components"]["xrpl"]["status"] == "DOWN"
    assert body["components"]["xrpl"]["detail"] == "XRPL server_info response was not successful"


def test_internal_status_reports_unconfigured_xrpl():
    client = _client(Settings(xrpl_json_rpc_url="", xrpl_issuer_seed="issuer-seed"))

    response = client.get("/internal/status")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "DEGRADED"
    assert body["components"]["xrpl"]["configured"] is False
    assert body["components"]["xrpl"]["status"] == "DEGRADED"
