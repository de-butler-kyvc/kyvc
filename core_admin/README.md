# KYvC Core Admin API

Core Admin exposes operator-facing API adapters for Core internal operations.
It treats Core as the source of truth and calls Core internal endpoints instead
of writing Core runtime configuration directly.

## Configuration

```env
APP_ENV=local
APP_PORT=8091
CORE_BASE_URL=http://127.0.0.1:8090
CORE_REQUEST_TIMEOUT_SECONDS=10
DEFAULT_OPERATOR_ID=core-admin
```

In container environments, set `CORE_BASE_URL` to the Core service URL on the
internal network, such as `http://kyvc-core-dev:8090`.

## API

- `GET /health`
- `GET /admin/core/status`
- `GET /admin/provider-selections/options`
- `GET /admin/provider-selections`
- `PUT /admin/provider-selections/{ocr|llm}`
- `GET /admin/provider-selections/history`

Provider selection updates accept only provider/profile identifiers and an
optional `changed_by`. Secrets, endpoint URLs, API keys, and model override
strings must stay in Core environment or secret storage.
