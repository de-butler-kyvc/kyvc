import httpx
import pytest

from app.ai_assessment.enums import DocumentType
from app.ai_assessment.providers.openai import OpenAiDocumentExtractionProvider
from app.core.config import Settings
from app.resilience import outbound
from app.resilience.outbound import CircuitOpenError, OutboundPolicy, execute_outbound, reset_circuit_breakers


@pytest.fixture(autouse=True)
def reset_breakers():
    reset_circuit_breakers()
    yield
    reset_circuit_breakers()


def _policy(*, attempts=3, threshold=5, recovery=30.0) -> OutboundPolicy:
    return OutboundPolicy(
        timeout_seconds=1.0,
        max_attempts=attempts,
        base_delay_seconds=0.0,
        max_delay_seconds=0.0,
        circuit_failure_threshold=threshold,
        circuit_recovery_timeout_seconds=recovery,
    )


def _http_error(status_code: int) -> httpx.HTTPStatusError:
    request = httpx.Request("GET", "https://dependency.example")
    response = httpx.Response(status_code, request=request)
    return httpx.HTTPStatusError("failed", request=request, response=response)


def test_retriable_failure_succeeds_after_retry():
    calls = 0

    def flaky():
        nonlocal calls
        calls += 1
        if calls == 1:
            raise httpx.TimeoutException("timeout")
        return "ok"

    result = execute_outbound("llm", "test", flaky, policy=_policy(), sleep=lambda delay: None)

    assert result == "ok"
    assert calls == 2


def test_non_retriable_failure_does_not_retry():
    calls = 0

    def bad_request():
        nonlocal calls
        calls += 1
        raise _http_error(400)

    with pytest.raises(httpx.HTTPStatusError):
        execute_outbound("llm", "test", bad_request, policy=_policy(), sleep=lambda delay: None)

    assert calls == 1


def test_circuit_opens_after_threshold_failures():
    policy = _policy(attempts=1, threshold=2, recovery=60.0)

    def timeout():
        raise httpx.TimeoutException("timeout")

    with pytest.raises(Exception):
        execute_outbound("ocr", "test", timeout, policy=policy, sleep=lambda delay: None)
    with pytest.raises(Exception):
        execute_outbound("ocr", "test", timeout, policy=policy, sleep=lambda delay: None)
    with pytest.raises(CircuitOpenError):
        execute_outbound("ocr", "test", lambda: "should-not-run", policy=policy, sleep=lambda delay: None)


def test_circuit_half_open_recovery_path_closes_on_success():
    policy = _policy(attempts=1, threshold=1, recovery=0.0)

    with pytest.raises(Exception):
        execute_outbound(
            "did_resolver",
            "test",
            lambda: (_ for _ in ()).throw(httpx.ConnectError("connect")),
            policy=policy,
            sleep=lambda delay: None,
        )

    result = execute_outbound("did_resolver", "test", lambda: "recovered", policy=policy, sleep=lambda delay: None)

    assert result == "recovered"


def test_xrpl_amendment_disabled_is_not_retried():
    calls = 0

    def amendment_disabled():
        nonlocal calls
        calls += 1
        raise RuntimeError("DIDSet failed because the selected XRPL network has the amendment disabled")

    with pytest.raises(RuntimeError):
        execute_outbound("xrpl", "submit", amendment_disabled, policy=_policy(), sleep=lambda delay: None)

    assert calls == 1


def test_openai_provider_uses_shared_policy(monkeypatch):
    responses = [
        httpx.Response(500, request=httpx.Request("POST", "https://api.example/responses")),
        httpx.Response(
            200,
            json={"output": [{"content": [{"type": "output_text", "text": '{"documentType":"BUSINESS_REGISTRATION"}'}]}]},
            request=httpx.Request("POST", "https://api.example/responses"),
        ),
    ]

    monkeypatch.setattr(
        outbound,
        "get_settings",
        lambda: Settings(outbound_retry_base_delay_seconds=0.0, outbound_retry_max_delay_seconds=0.0),
    )

    def fake_post(*args, **kwargs):
        response = responses.pop(0)
        return response

    monkeypatch.setattr("app.ai_assessment.providers.openai.httpx.post", fake_post)
    provider = OpenAiDocumentExtractionProvider(api_key="test", model="test-model", base_url="https://api.example")

    payload = provider._extract_json(DocumentType.BUSINESS_REGISTRATION, "company text")

    assert payload["documentType"] == "BUSINESS_REGISTRATION"
    assert responses == []
