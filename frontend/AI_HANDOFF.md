# KYvC Frontend AI Handoff

Last updated: 2026-05-10

This document is for AI agents or engineers continuing work in the `frontend` project. It summarizes the current mobile wallet work, the integration points, and the checks that should be run after changes.

## Project Shape

- Framework: Next.js App Router, React 19, TypeScript.
- Main user-facing desktop/web app routes live under `app/corporate`, `app/finance`, `app/wallet`, `app/login`, and `app/signup`.
- Mobile wallet WebView routes live under `app/m`.
- Mobile shared UI and icons live under `components/m`.
- Mobile bridge/session helpers live under `lib/m`.
- HTTP API client and typed endpoint wrappers live in `lib/api.ts`.
- Mobile CSS is centralized in `app/m/mobile.css` and scoped by `body[data-mobile="true"] .m-shell`.

## Runtime Notes

- Next 16 requires Node 20+. The machine's default `node` may be older. Use a Node 20+ runtime for build/type checks.
- Both `package-lock.json` and `pnpm-lock.yaml` exist. Keep both in sync when adding dependencies.
- `qrcode.react` is used for real SVG QR rendering.
- Local browser preview does not have `window.Android`; bridge calls should show controlled errors or use browser fallbacks only where safe.

Useful frontend checks:

```bash
cd frontend
node ./node_modules/typescript/bin/tsc --noEmit
node ./node_modules/next/dist/bin/next build
```

If local `node` is too old, run those commands with a Node 20+ binary.

## Mobile Layout Contract

`app/m/layout.tsx` imports mobile CSS and injects a small script that sets `body[data-mobile="true"]` before hydration. `app/m/mobile-body-marker.tsx` keeps the flag in sync and installs the Android bridge callbacks via `setupBridge()`.

`app/layout.tsx` has `suppressHydrationWarning` on both `html` and `body` because the mobile layout mutates the body attribute before React hydrates. Do not remove this unless the mobile CSS scoping strategy changes.

## API Layer

All browser API calls go through `lib/api.ts`.

Important mobile-related exports:

- `credentials.list()`
- `credentials.detail(credentialId)`
- `credentials.offerForKyc(kycId)`
- `mobileVp.resolveQr(qrPayload)`
- `mobileVp.request(requestId)`
- `mobileVp.eligibleCredentials(requestId)`
- `mobileVp.submitPresentation(body)`

The API client uses `BASE_URL = "https://dev-api-kyvc.khuoo.synology.me"` and sends cookies with `withCredentials: true`. A 401 on mobile pages often means the browser preview is unauthenticated, not that the page code is broken.

## Android Bridge Contract

Bridge code lives in `lib/m/android-bridge.ts`.

Use:

- `isBridgeAvailable()` before optional native calls.
- `bridge.getWalletInfo()` for active XRPL account/DID.
- `bridge.getWalletDepositInfo()` for receive address.
- `bridge.getWalletAssets()` for XRP balance.
- `bridge.scanQRCode(purpose)` for native QR scanning.
- `bridge.submitXrpPayment(...)` for XRP transfer.
- `bridge.signMessage(...)` for VP signing.
- `bridge.submitPresentationToVerifier(...)` only for direct verifier fallback.
- `bridge.copyTextToClipboard(text)` when native clipboard support is available.

`callBridge()` intentionally rejects when `window.Android` is missing. Do not silently mock sensitive wallet, signing, payment, or credential flows.

## Session Storage Contract

Short-lived mobile flow state lives in `lib/m/session.ts`.

Current keys and uses:

- `scan`: QR scan result passed from `/m/vp/scan` to `/m/vp/submit` and `/m/vp/submitting`.
- `vpRequest`: resolved VP request metadata.
- `selectedVcId`: credential selected for VP submission.
- `xrpTransfer`: XRP transfer draft from `/m/xrp/send`.
- `xrpTransferResult`: XRP transfer result shown on `/m/xrp/complete`.

Never store seed, mnemonic, private key, full VC raw text, or long-lived secrets in this helper.

## VC QR Flow

Route: `app/m/vc/qr/page.tsx`

URL shape:

```text
/m/vc/qr/?id=urn%3Acred%3A2
```

Flow:

1. Parse `urn:cred:{number}` from the query string.
2. Fetch `credentials.detail(credentialId)`.
3. Try bridge `getWalletInfo()` for the active XRPL account.
4. If `detail.kycId` exists, try `credentials.offerForKyc(kycId)`.
5. Build a QR payload from the backend offer payload plus wallet metadata.
6. Render a real QR with `QRCodeSVG`.

Payload fields are compacted and may include:

- `type`
- `version`
- `source`
- `offerId`
- `qrToken`
- `credentialId`
- `credentialExternalId`
- `credentialTypeCode`
- `credentialStatusCode`
- `issuerDid`
- `holderDid`
- `holderXrplAddress`
- `credentialExpiresAt`
- `qrExpiresAt`
- `expiresAt`

Expiration behavior:

- Prefer backend `offer.expiresAt`.
- If no backend offer is available, use a 3-minute client fallback for the address QR.

Browser preview caveat:

- If unauthenticated, the page should show `VC 조회 실패: 인증이 필요합니다.`
- If authenticated but no backend offer and no bridge wallet address, it should show a controlled QR generation error.

Related backend contract:

- `backend/domain/credential/application/CredentialService.java` now enriches `qrPayload` with `credentialExternalId`, `credentialTypeCode`, `holderDid`, and `holderXrplAddress`.

## XRP Flow

Receive:

- Route: `app/m/xrp/receive/page.tsx`
- Uses `bridge.getWalletDepositInfo()` when available.
- Displays a real QR for the receive address.
- Copies via native `bridge.copyWalletAddress()` first, browser clipboard fallback second.

Send:

- Route: `app/m/xrp/send/page.tsx`
- Uses `bridge.getWalletAssets()` for available balance.
- QR scan button calls `bridge.scanQRCode("XRP_ADDRESS")`.
- Accepts plain addresses, JSON payloads with address fields, and simple `xrpl:` URIs.
- Writes `xrpTransfer` to session storage.

Confirm:

- Route: `app/m/xrp/confirm/page.tsx`
- Reads `xrpTransfer`.
- Calls `bridge.submitXrpPayment({ destinationAddress, destinationTag?, amountXrp })`.
- Writes `xrpTransferResult`.

Complete:

- Route: `app/m/xrp/complete/page.tsx`
- Reads `xrpTransferResult`.
- Shows real address, amount, fee, time, and tx hash if returned.
- Clears transfer session state when returning home.

## VP QR / Submit Flow

Scan:

- Route: `app/m/vp/scan/page.tsx`
- Calls `bridge.scanQRCode("VP_REQUEST")`.
- If `qrData` exists, calls `mobileVp.resolveQr(qrData)`.
- Stores scan metadata in `mSession.writeScanResult`.
- Routes credential offers to `/m/vc/issue`, otherwise VP requests to `/m/vp/submit`.

Submit:

- Route: `app/m/vp/submit/page.tsx`
- If a `requestId` exists, loads request details with `mobileVp.request(requestId)`.
- Loads eligible credentials with `mobileVp.eligibleCredentials(requestId)`.
- If no requestId exists, falls back to `credentials.list()` for preview-friendly behavior.
- Writes selected credential ID to session storage.

Submitting:

- Route: `app/m/vp/submitting/page.tsx`
- Requires bridge availability for signing.
- Requests or uses nonce/challenge.
- Calls `bridge.signMessage(...)`.
- If `requestId` and selected credential ID are available, submits via `mobileVp.submitPresentation(...)`.
- Otherwise uses direct bridge verifier submission fallback.

## Design Notes

The current mobile updates were based on `/Users/kimjimin/Downloads/App (1).pdf`.

Keep mobile screens aligned with that reference:

- White or near-white mobile surface.
- Compact, centered mobile hierarchy.
- Dark navy primary buttons.
- Clean card surfaces with 12-28px radii depending on surface scale.
- Real QR assets, not placeholder patterned boxes.
- Avoid explanatory feature text inside the app unless it is part of the actual workflow.
- Test at a mobile viewport around `390x844`.

Primary CSS areas touched:

- QR card styles around `.qr-panel`, `.qr-box.real`, `.qr-address`.
- XRP receive QR styles around `.xrp-address-card`, `.xrp-address-qr`.
- Existing XRP flow styles around `.xrp-flow-view`, `.xrp-bottom-action`, `.send-confirm-card`.

## Verification History

Completed after the latest mobile integration work:

```bash
cd frontend
node ./node_modules/typescript/bin/tsc --noEmit
node ./node_modules/next/dist/bin/next build
```

Also visually checked in browser at mobile viewport:

- `/m/xrp/receive`
- `/m/xrp/send`
- `/m/xrp/confirm`
- `/m/vc/qr/?id=urn%3Acred%3A2`

Backend related smoke tests that passed:

```bash
cd backend
LOG_PATH=./build/test-logs sh ./gradlew test \
  --tests 'com.kyvc.backend.domain.credential.application.MobileWalletServiceTest' \
  --tests 'com.kyvc.backend.domain.vp.application.VpVerificationServiceTest'
```

Known backend full-test caveat:

- `sh ./gradlew test` needs a writable `LOG_PATH`.
- After setting `LOG_PATH=./build/test-logs`, the full suite still needs a local PostgreSQL/Flyway-ready database for `BackendApplicationTests.contextLoads`.

## Common Pitfalls

- Do not assume bridge calls work in a normal desktop browser.
- Do not add mock wallet/signing/payment behavior for sensitive flows.
- Do not put persistent secrets in `sessionStorage`.
- Do not remove `withCredentials: true` from the API client.
- Do not remove `body[data-mobile="true"]` scoping without rewriting `mobile.css`.
- Do not leave fake QR placeholders on production mobile screens.
- When adding a dependency, update both lockfiles.
