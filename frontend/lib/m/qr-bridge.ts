import { mobileVp } from "@/lib/api";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";
import { mSession } from "@/lib/m/session";

type RouterLike = {
  push: (href: string) => void;
};

export async function scanPresentationQrToRoute() {
  if (!isBridgeAvailable()) {
    throw new Error("앱에서만 사용할 수 있는 기능입니다");
  }

  const r = await bridge.scanPresentationQrCode();
  if (!r.ok) {
    throw new Error(r.error ?? "증명서 제출 QR 스캔에 실패했습니다.");
  }

  const resolved = r.qrData
    ? await mobileVp.resolveQr(r.qrData).catch(() => null)
    : null;

  mSession.writeScanResult({
    qrData: r.qrData,
    actionType: resolved?.type ?? r.actionType,
    coreBaseUrl: r.coreBaseUrl,
    requestId: resolved?.requestId ?? resolved?.targetId,
    challenge: r.challenge,
    domain: r.domain,
    endpoint: r.endpoint,
    receivedAt: Date.now(),
  });

  const nextAction = resolved?.nextAction;
  const type = resolved?.type ?? r.actionType ?? "VP_REQUEST";
  if (type === "VC_ISSUE" || nextAction === "OPEN_CREDENTIAL_OFFER") {
    return "/m/vc/issue";
  }
  if (type === "LOGIN_REQUEST") {
    return "/m/home";
  }
  return "/m/vp/submit";
}

export async function scanPresentationQrAndNavigate(
  router: RouterLike,
) {
  const route = await scanPresentationQrToRoute();
  router.push(route);
}
