import type { CertItem } from "@/components/m/parts";
import type { NativeCredentialSummary } from "@/lib/m/android-bridge";

const PALETTES = [
  "linear-gradient(135deg,#111827 0%,#183b8f 48%,#7c3aed 100%)",
  "linear-gradient(135deg,#052e2b 0%,#0f766e 48%,#2563eb 100%)",
  "linear-gradient(135deg,#231942 0%,#5e3bce 50%,#00a3ff 100%)",
];

const NATIVE_STATUS_LABEL: Record<string, string> = {
  active: "활성",
  issued: "발급됨",
  inactive: "비활성",
  expired: "만료됨",
  notYetValid: "유효 전",
};

export function credentialPalette(index: number) {
  return PALETTES[index % PALETTES.length]!;
}

export function formatCredentialDate(value?: string) {
  return value ? value.slice(0, 10).replaceAll("-", ".") : "-";
}

export function nativeCredentialTitle(_summary: NativeCredentialSummary) {
  return "법인 KYC 증명서";
}

export function nativeCredentialIssuer(summary: NativeCredentialSummary) {
  return (
    summary.issuerDid ??
    summary.issuerAccount ??
    "Issuer"
  );
}

export function nativeCredentialStatus(summary: NativeCredentialSummary) {
  return (
    summary.statusLabel ??
    NATIVE_STATUS_LABEL[summary.status ?? ""] ??
    "발급됨"
  );
}

export function nativeSummaryToCert(
  summary: NativeCredentialSummary,
  index: number,
): CertItem {
  return {
    issuer: nativeCredentialIssuer(summary),
    title: nativeCredentialTitle(summary),
    status: nativeCredentialStatus(summary),
    id: summary.credentialId || `native-credential-${index}`,
    holderDid: summary.holderDid,
    date: formatCredentialDate(summary.issuedAt),
    expiresAt: summary.expiresAt
      ? formatCredentialDate(summary.expiresAt)
      : undefined,
    gradient: credentialPalette(index),
  };
}
