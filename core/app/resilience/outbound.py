import logging
import random
import time
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any, Literal, TypeVar

import httpx

from app.core.config import Settings, get_settings

logger = logging.getLogger(__name__)

DependencyCategory = Literal["xrpl", "did_resolver", "ocr", "llm"]
T = TypeVar("T")


class OutboundDependencyError(RuntimeError):
    def __init__(self, category: DependencyCategory, operation: str, message: str):
        super().__init__(f"{category} {operation} failed: {message}")
        self.category = category
        self.operation = operation


class CircuitOpenError(OutboundDependencyError):
    pass


class TransientOutboundError(OutboundDependencyError):
    pass


@dataclass(frozen=True)
class OutboundPolicy:
    timeout_seconds: float
    max_attempts: int
    base_delay_seconds: float
    max_delay_seconds: float
    circuit_failure_threshold: int
    circuit_recovery_timeout_seconds: float


class CircuitBreaker:
    def __init__(self, *, failure_threshold: int, recovery_timeout_seconds: float) -> None:
        self.failure_threshold = max(1, failure_threshold)
        self.recovery_timeout_seconds = max(0.0, recovery_timeout_seconds)
        self.state = "closed"
        self.failure_count = 0
        self.opened_at: float | None = None

    def before_call(self, category: DependencyCategory) -> None:
        if self.state != "open":
            return
        assert self.opened_at is not None
        if time.monotonic() - self.opened_at >= self.recovery_timeout_seconds:
            self.state = "half_open"
            logger.warning("outbound circuit half-open category=%s", category)
            return
        raise CircuitOpenError(category, "circuit", "circuit is open")

    def record_success(self, category: DependencyCategory) -> None:
        if self.state != "closed" or self.failure_count:
            logger.info("outbound circuit closed category=%s", category)
        self.state = "closed"
        self.failure_count = 0
        self.opened_at = None

    def record_failure(self, category: DependencyCategory) -> None:
        self.failure_count += 1
        if self.state == "half_open" or self.failure_count >= self.failure_threshold:
            self.state = "open"
            self.opened_at = time.monotonic()
            logger.warning("outbound circuit opened category=%s failures=%s", category, self.failure_count)


_circuit_breakers: dict[DependencyCategory, CircuitBreaker] = {}


def reset_circuit_breakers() -> None:
    _circuit_breakers.clear()


def outbound_policy(settings: Settings, category: DependencyCategory) -> OutboundPolicy:
    return OutboundPolicy(
        timeout_seconds=_timeout_for(settings, category),
        max_attempts=max(1, settings.outbound_retry_max_attempts),
        base_delay_seconds=max(0.0, settings.outbound_retry_base_delay_seconds),
        max_delay_seconds=max(0.0, settings.outbound_retry_max_delay_seconds),
        circuit_failure_threshold=max(1, settings.outbound_circuit_failure_threshold),
        circuit_recovery_timeout_seconds=max(0.0, settings.outbound_circuit_recovery_timeout_seconds),
    )


def outbound_timeout(category: DependencyCategory, settings: Settings | None = None) -> float:
    return _timeout_for(settings or get_settings(), category)


def execute_outbound(
    category: DependencyCategory,
    operation: str,
    call: Callable[[], T],
    *,
    policy: OutboundPolicy | None = None,
    sleep: Callable[[float], None] = time.sleep,
) -> T:
    selected_policy = policy or outbound_policy(get_settings(), category)
    breaker = _breaker(category, selected_policy)
    breaker.before_call(category)

    last_exc: BaseException | None = None
    for attempt in range(1, selected_policy.max_attempts + 1):
        try:
            result = call()
        except Exception as exc:
            last_exc = exc
            retriable = is_retriable_failure(category, exc)
            logger.warning(
                "outbound call failed category=%s operation=%s attempt=%s/%s retriable=%s error=%s",
                category,
                operation,
                attempt,
                selected_policy.max_attempts,
                retriable,
                type(exc).__name__,
            )
            if not retriable:
                raise
            if attempt >= selected_policy.max_attempts:
                breaker.record_failure(category)
                raise OutboundDependencyError(category, operation, f"{type(exc).__name__}: {exc}") from exc
            sleep(_retry_delay_for_failure(selected_policy, attempt, exc))
            continue
        breaker.record_success(category)
        return result
    raise OutboundDependencyError(category, operation, str(last_exc or "unknown failure"))


def is_retriable_failure(category: DependencyCategory, exc: BaseException) -> bool:
    # Permanent policy/configuration and deterministic ledger failures must fail fast.
    if isinstance(exc, CircuitOpenError):
        return False
    if isinstance(exc, TransientOutboundError):
        return True
    if isinstance(exc, (ValueError, FileNotFoundError, PermissionError)):
        return False
    if isinstance(exc, RuntimeError):
        message = str(exc)
        if (
            "amendment disabled" in message
            or message.startswith("Refusing to connect to XRPL Mainnet.")
            or message.startswith("Mainnet URL configured.")
        ):
            return False
    if isinstance(exc, httpx.HTTPStatusError):
        status_code = exc.response.status_code
        if status_code == 429:
            return category in {"ocr", "llm"}
        if status_code in {408, 409, 425}:
            return True
        return 500 <= status_code < 600
    if isinstance(exc, (httpx.TimeoutException, httpx.ConnectError, httpx.NetworkError, TimeoutError, ConnectionError)):
        return True
    if isinstance(exc, httpx.RequestError):
        return True
    return False


def _breaker(category: DependencyCategory, policy: OutboundPolicy) -> CircuitBreaker:
    breaker = _circuit_breakers.get(category)
    if breaker is None:
        breaker = CircuitBreaker(
            failure_threshold=policy.circuit_failure_threshold,
            recovery_timeout_seconds=policy.circuit_recovery_timeout_seconds,
        )
        _circuit_breakers[category] = breaker
    return breaker


def _retry_delay(policy: OutboundPolicy, attempt: int) -> float:
    delay = min(policy.max_delay_seconds, policy.base_delay_seconds * (2 ** (attempt - 1)))
    if delay <= 0:
        return 0.0
    return random.uniform(0, delay)


def _retry_delay_for_failure(policy: OutboundPolicy, attempt: int, exc: BaseException) -> float:
    if isinstance(exc, httpx.HTTPStatusError):
        retry_after = _retry_after_seconds(exc.response.headers)
        if retry_after is not None:
            return retry_after
    return _retry_delay(policy, attempt)


def _retry_after_seconds(headers: httpx.Headers) -> float | None:
    value = headers.get("retry-after") or headers.get("Retry-After")
    if not value:
        return None
    stripped = value.strip()
    if not stripped:
        return None
    try:
        return max(0.0, float(stripped))
    except ValueError:
        pass
    try:
        retry_at = parsedate_to_datetime(stripped)
    except (TypeError, ValueError, IndexError, OverflowError):
        return None
    if retry_at.tzinfo is None:
        retry_at = retry_at.replace(tzinfo=timezone.utc)
    return max(0.0, (retry_at - datetime.now(timezone.utc)).total_seconds())


def _timeout_for(settings: Settings, category: DependencyCategory) -> float:
    if category == "xrpl":
        return settings.outbound_xrpl_timeout_seconds
    if category == "did_resolver":
        return settings.outbound_did_resolver_timeout_seconds
    if category == "ocr":
        return settings.outbound_ocr_timeout_seconds
    if category == "llm":
        return settings.outbound_llm_timeout_seconds
    return settings.outbound_default_timeout_seconds
