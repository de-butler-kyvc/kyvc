from typing import Any, Literal

from pydantic import BaseModel, Field
from xrpl.models.requests import ServerInfo

from app.core.config import Settings
from app.xrpl.client import enforce_mainnet_policy, make_client

StatusValue = Literal["UP", "DEGRADED", "DOWN"]


class ComponentStatus(BaseModel):
    status: StatusValue
    configured: bool = True
    detail: str


class ComponentsStatus(BaseModel):
    database: ComponentStatus
    xrpl: ComponentStatus
    issuer: ComponentStatus


class InternalStatusResponse(BaseModel):
    status: StatusValue
    service: str
    environment: str
    components: ComponentsStatus
    diagnostics: dict[str, Any] = Field(default_factory=dict)


def build_internal_status(settings: Settings, repository: Any) -> InternalStatusResponse:
    database = _database_status(repository)
    xrpl = _xrpl_status(settings)
    issuer = _issuer_status(settings, xrpl)
    overall = _overall_status(database, xrpl, issuer)
    diagnostics = {
        "xrplNetwork": settings.xrpl_network_name,
    }
    return InternalStatusResponse(
        status=overall,
        service=settings.app_name,
        environment=settings.app_env,
        components=ComponentsStatus(
            database=database,
            xrpl=xrpl,
            issuer=issuer,
        ),
        diagnostics=diagnostics,
    )


def _database_status(repository: Any) -> ComponentStatus:
    readiness_probe = getattr(repository, "readiness_probe", None)
    if not callable(readiness_probe):
        return ComponentStatus(
            status="DEGRADED",
            configured=True,
            detail="repository does not expose a readiness probe",
        )
    try:
        readiness_probe()
    except Exception as exc:
        return ComponentStatus(
            status="DOWN",
            configured=True,
            detail=f"database readiness probe failed: {type(exc).__name__}",
        )
    return ComponentStatus(status="UP", configured=True, detail="database readiness probe succeeded")


def _xrpl_status(settings: Settings) -> ComponentStatus:
    rpc_url = settings.xrpl_json_rpc_url.strip()
    if not rpc_url:
        return ComponentStatus(status="DEGRADED", configured=False, detail="XRPL JSON-RPC URL is not configured")
    try:
        enforce_mainnet_policy(rpc_url, settings.allow_mainnet, False)
    except RuntimeError as exc:
        return ComponentStatus(status="DOWN", configured=True, detail=str(exc))
    try:
        response = make_client(rpc_url).request(ServerInfo())
        result = _response_result(response)
    except Exception as exc:
        return ComponentStatus(
            status="DOWN",
            configured=True,
            detail=f"XRPL server_info request failed: {type(exc).__name__}",
        )
    if _response_is_successful(result):
        return ComponentStatus(status="UP", configured=True, detail="XRPL server_info request succeeded")
    return ComponentStatus(status="DOWN", configured=True, detail="XRPL server_info response was not successful")


def _issuer_status(settings: Settings, xrpl: ComponentStatus) -> ComponentStatus:
    if not settings.xrpl_issuer_seed:
        return ComponentStatus(status="DEGRADED", configured=False, detail="XRPL issuer seed is not configured")
    if xrpl.status != "UP":
        return ComponentStatus(
            status="DEGRADED",
            configured=True,
            detail="issuer seed is configured but XRPL is not ready",
        )
    return ComponentStatus(status="UP", configured=True, detail="issuer seed is configured")


def _overall_status(database: ComponentStatus, xrpl: ComponentStatus, issuer: ComponentStatus) -> StatusValue:
    if database.status == "DOWN":
        return "DOWN"
    if xrpl.status != "UP" or issuer.status != "UP" or database.status != "UP":
        return "DEGRADED"
    return "UP"


def _response_result(response: Any) -> dict[str, Any]:
    if isinstance(response, dict):
        return response.get("result", response)
    result = getattr(response, "result", None)
    if isinstance(result, dict):
        return result
    to_dict = getattr(response, "to_dict", None)
    if callable(to_dict):
        data = to_dict()
        if isinstance(data, dict):
            return data.get("result", data)
    return {}


def _response_is_successful(result: dict[str, Any]) -> bool:
    if result.get("error") or result.get("error_message") or result.get("errorMessage"):
        return False
    status = result.get("status")
    if status and status != "success":
        return False
    return bool(result.get("info") or result.get("state") or result)
