"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import {
  bridge,
  isBridgeAvailable,
  type WalletAssetsResult,
  type WalletInfo,
} from "@/lib/m/android-bridge";
import {
  ensureMobileSessionOwner,
  formatXrpDrops,
  formatXrp,
  loadWalletAssets,
  parseXrpToDrops,
  readXrpBalance,
  xrpBalanceDropsFromAssets,
} from "@/lib/m/wallet-bridge";

const DID_TX_FEE_DROPS = BigInt(12);
const XRPL_BASE_RESERVE_DROPS = BigInt(1_000_000);
const XRPL_OWNER_RESERVE_DROPS = BigInt(200_000);

function positiveBigInt(value?: string | number | bigint | null) {
  if (value == null || value === "") return null;
  if (typeof value === "bigint") return value >= BigInt(0) ? value : null;
  const raw = String(value);
  return /^\d+$/.test(raw) ? BigInt(raw) : null;
}

function readAvailableDrops(assets?: WalletAssetsResult | null) {
  if (!assets?.ok || assets.depositRequired) return null;
  return (
    positiveBigInt(assets.availableXrpDrops) ??
    positiveBigInt(assets.availableDrops) ??
    positiveBigInt(assets.spendableDrops) ??
    parseXrpToDrops(assets.availableXrp) ??
    parseXrpToDrops(assets.spendableXrp)
  );
}

function readFeeDropsFromResult(result?: {
  feeDrops?: string | number;
  feeXrp?: string | number;
  networkFeeDrops?: string | number;
  networkFeeXrp?: string | number;
} | null) {
  if (!result) return null;
  return (
    positiveBigInt(result.feeDrops) ??
    positiveBigInt(result.networkFeeDrops) ??
    parseXrpToDrops(result.feeXrp) ??
    parseXrpToDrops(result.networkFeeXrp)
  );
}

function formatSignedXrpDrops(value?: bigint | null) {
  if (value == null) return "-";
  if (value < BigInt(0)) return `-${formatXrpDrops(-value)}`;
  return formatXrpDrops(value);
}

export default function MobileDidRegisterPage() {
  const router = useRouter();
  const [walletInfo, setWalletInfo] = useState<WalletInfo | null>(null);
  const [walletAssets, setWalletAssets] = useState<WalletAssetsResult | null>(null);
  const [feeDrops, setFeeDrops] = useState<bigint | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isBridgeAvailable()) return;
    ensureMobileSessionOwner()
      .then(async (state) => {
        setWalletInfo(state.wallet);
        setWalletAssets(await loadWalletAssets());
        const fee = await bridge.estimateHolderDidSetFee().catch(() => null);
        setFeeDrops(readFeeDropsFromResult(fee) ?? DID_TX_FEE_DROPS);
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : "지갑 상태 확인에 실패했습니다.");
        setFeeDrops(DID_TX_FEE_DROPS);
      });
  }, []);

  const onRegister = async () => {
    setError(null);
    setBusy(true);
    try {
      if (isBridgeAvailable()) {
        const r = await bridge.submitHolderDidSet();
        if (!r.ok) {
          setError(r.error ?? "DID 등록 요청에 실패했습니다.");
          return;
        }
      }
      router.replace("/m/home");
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "DID 등록 중 오류가 발생했습니다.",
      );
    } finally {
      setBusy(false);
    }
  };

  const balanceDrops = xrpBalanceDropsFromAssets(walletAssets);
  const availableDrops = readAvailableDrops(walletAssets);
  const ownerCount = walletAssets?.ownerCount ?? 0;
  const networkFeeDrops = feeDrops ?? DID_TX_FEE_DROPS;
  const baseReserveDrops =
    positiveBigInt(walletAssets?.baseReserveDrops) ?? XRPL_BASE_RESERVE_DROPS;
  const ownerReserveDrops =
    positiveBigInt(walletAssets?.ownerReserveDrops) ?? XRPL_OWNER_RESERVE_DROPS;
  const currentReserve =
    baseReserveDrops + ownerReserveDrops * BigInt(ownerCount);
  const currentSpendable =
    availableDrops ??
    (balanceDrops == null ? null : balanceDrops - currentReserve);
  const spendableAfterDid =
    currentSpendable == null
      ? null
      : currentSpendable - ownerReserveDrops - networkFeeDrops;
  const currentBalance =
    balanceDrops == null ? formatXrp(readXrpBalance(walletAssets)) : formatXrpDrops(balanceDrops);
  const depositRequired = Boolean(walletAssets?.depositRequired);
  const insufficientBalance = spendableAfterDid != null && spendableAfterDid < BigInt(0);

  return (
    <section className="view did-register-view">
      <MTopBar title="DID 등록" back="/m/home" />
      <div className="content scroll did-register-content">
        <div className="did-hero-icon" aria-hidden="true">
          <MIcon.wallet />
        </div>
        <h1>DID를 등록합니다</h1>
        <p>
          분산 식별자(DID)를 XRP Ledger에 등록합니다.
          <br />
          등록 후 증명서 발급 및 제출 기능을 이용할 수 있습니다.
        </p>

        <dl className="did-cost-card">
          <div>
            <dt>현재 잔액</dt>
            <dd>{currentBalance}</dd>
          </div>
          <div>
            <dt>네트워크 수수료</dt>
            <dd>{feeDrops == null ? "계산 중" : formatXrpDrops(networkFeeDrops)}</dd>
          </div>
          <div>
            <dt>계정 준비금 증가 (잠금, 소각 아님)</dt>
            <dd>{formatXrpDrops(ownerReserveDrops)}</dd>
          </div>
          <div>
            <dt>등록 후 사용 가능 잔액</dt>
            <dd className="accent">{formatSignedXrpDrops(spendableAfterDid)}</dd>
          </div>
        </dl>

        <div className="did-notice">
          <MIcon.zap />
          <div>
            <strong>온체인 비용이 발생합니다</strong>
            <p>
              네트워크 수수료는 소액 차감되며, 준비금은 DID 삭제 시 반환됩니다.
              등록은 블록체인에 영구 기록됩니다.
            </p>
          </div>
        </div>
        {insufficientBalance ? (
          <p className="m-error">
            DID 등록 후 필요한 준비금을 충족할 수 없습니다. XRP를 추가 입금해 주세요.
          </p>
        ) : null}
        {error ? <p className="m-error">{error}</p> : null}
      </div>
      <div className="bottom-action did-register-actions">
        <button
          type="button"
          className="primary"
          onClick={onRegister}
          disabled={
            busy ||
            !walletInfo ||
            feeDrops == null ||
            depositRequired ||
            insufficientBalance
          }
        >
          {busy ? "등록 중..." : "DID 등록하기"}
        </button>
        <button
          type="button"
          className="secondary"
          onClick={() => router.back()}
        >
          취소
        </button>
      </div>
    </section>
  );
}
